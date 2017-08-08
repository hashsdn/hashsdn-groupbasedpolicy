/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.GbpSxpIseAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseHarvestStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseHarvestStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.ise.source.config.ConnectionConfig;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listen for harvest configuration and trigger harvesting
 */
public class GbpIseConfigListenerImpl implements GbpIseConfigListener {

    private static final Logger LOG = LoggerFactory.getLogger(GbpIseConfigListenerImpl.class);

    private static final String DATE_AND_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private final DataBroker dataBroker;
    private final GbpIseSgtHarvester gbpIseSgtHarvester;
    @Nonnull private final EPPolicyTemplateProviderFacade templateProviderFacade;
    private final ThreadPoolExecutor pool;

    public GbpIseConfigListenerImpl(@Nonnull final DataBroker dataBroker, @Nonnull final GbpIseSgtHarvester gbpIseSgtHarvester,
                                    @Nonnull final EPPolicyTemplateProviderFacade templateProviderFacade) {
        this.dataBroker = dataBroker;
        this.gbpIseSgtHarvester = gbpIseSgtHarvester;
        this.templateProviderFacade = templateProviderFacade;
        pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10),
                new ThreadFactoryBuilder().setNameFormat("ise-sgt-harverster-%d").build()) {
            @Override
            protected void afterExecute(final Runnable r, final Throwable t) {
                super.afterExecute(r, t);
                if (t != null) {
                    LOG.warn("ise harvest task failed", t);
                }
            }
        };
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<IseSourceConfig>> collection) {
        for (DataTreeModification<IseSourceConfig> modification : collection) {
            final IseSourceConfig iseSourceConfig = modification.getRootNode().getDataAfter();
            final IseContext iseContext = new IseContext(iseSourceConfig);
            templateProviderFacade.assignIseContext(iseContext);
            if (iseSourceConfig != null) {
                final Optional<ConnectionConfig> connectionConfig = Optional.ofNullable(iseSourceConfig.getConnectionConfig());
                LOG.debug("Ise-source config assigned: {} -> {}", iseSourceConfig.getTenant(),
                        connectionConfig.map(ConnectionConfig::getIseRestUrl).orElse(new Uri("n/a")));
                pool.submit(() -> {
                    final ListenableFuture<Collection<SgtInfo>> harvestResult = gbpIseSgtHarvester.harvestAll(iseContext);
                    Futures.addCallback(harvestResult, new FutureCallback<Collection<SgtInfo>>() {
                        @Override
                        public void onSuccess(@Nullable final Collection<SgtInfo> result) {
                            final Integer counter = Optional.ofNullable(result).map(Collection::size).orElse(0);
                            LOG.debug("ise harvest finished, outcome: {}", counter);
                            storeOutcome(true, counter, null);
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            LOG.debug("ise harvest failed", t);
                            storeOutcome(false, 0, t.getMessage());
                        }
                    }, MoreExecutors.directExecutor());

                    try {
                        harvestResult.get(30, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        LOG.debug("failed to finish ise-sgt-harvest task properly on time", e);
                    }
                });
            } else {
                LOG.debug("Ise-source config removed");
            }
        }
    }

    private CheckedFuture<Void, TransactionCommitFailedException> storeOutcome(final boolean succeeded, final int counter, final String reason) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<IseHarvestStatus> harvestStatusPath = InstanceIdentifier.create(GbpSxpIseAdapter.class)
                .child(IseHarvestStatus.class);
        final IseHarvestStatus harvestStatus = new IseHarvestStatusBuilder()
                .setReason(reason)
                .setSuccess(succeeded)
                .setTemplatesWritten(counter)
                .setTimestamp(createDateTime(new Date()))
                .build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, harvestStatusPath, harvestStatus, true);
        return wTx.submit();
    }

    private static DateAndTime createDateTime(Date when) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_AND_TIME_FORMAT);
        return new DateAndTime(simpleDateFormat.format(when));
    }

    @Override
    public void close() throws Exception {
        if (! pool.isTerminated()) {
            pool.shutdown();
            final boolean terminated = pool.awaitTermination(10, TimeUnit.SECONDS);
            if (! terminated) {
                pool.shutdownNow();
            }
        }
    }
}
