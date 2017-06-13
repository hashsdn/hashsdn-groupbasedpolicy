/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_integration.sxp_ise_adapter;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.spi.SxpEpProviderProvider;
import org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl.GbpIseAdapterProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpIseAdapterProviderInstance implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpIseAdapterProviderInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);

    private final DataBroker dataBroker;
    private final BindingAwareBroker bindingAwareBroker;
    private final ClusterSingletonServiceProvider clusterSingletonService;
    private final SxpEpProviderProvider sxpEpProvider;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private GbpIseAdapterProvider iseAdapterProvider;

    public SxpIseAdapterProviderInstance(final DataBroker dataBroker,
                                         final BindingAwareBroker bindingAwareBroker,
                                         final ClusterSingletonServiceProvider clusterSingletonService,
                                         final SxpEpProviderProvider sxpEpProvider) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.bindingAwareBroker = Preconditions.checkNotNull(bindingAwareBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
        this.sxpEpProvider = Preconditions.checkNotNull(sxpEpProvider);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        try {
            singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
        } catch (Exception e) {
            LOG.warn("Exception thrown while registering cluster singleton service in {}", this.getClass(), e.getMessage());
        }
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        iseAdapterProvider = new GbpIseAdapterProvider(dataBroker, bindingAwareBroker, sxpEpProvider);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        try {
            iseAdapterProvider.close();
        } catch (Exception e) {
            LOG.warn("iseAdapterProvider closing failed: {}", e.getMessage());
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly. Cause: {}", e.getMessage());
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
