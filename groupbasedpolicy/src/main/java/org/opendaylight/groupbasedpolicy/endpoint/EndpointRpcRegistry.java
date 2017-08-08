/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.endpoint;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.SetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnsetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.ConditionMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.ConditionMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.has.endpoint.group.conditions.EndpointGroupCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.has.endpoint.group.conditions.EndpointGroupConditionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Prefix;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Endpoint registry provides a scalable store for accessing and updating
 * information about endpoints.
 */
public class EndpointRpcRegistry implements EndpointService, EpRendererAugmentationRegistry, AutoCloseable {

    final static ConcurrentMap<String, EpRendererAugmentation> registeredRenderers =
            new ConcurrentHashMap<String, EpRendererAugmentation>();
    private static final Logger LOG = LoggerFactory.getLogger(EndpointRpcRegistry.class);
    private final DataBroker dataProvider;
    private Function<Void, RpcResult<Void>> futureTrans = input -> RpcResultBuilder.<Void>success().build();

    /**
     * This method registers a renderer for endpoint RPC API. This method
     * ensures single RPC registration for all renderers since a single RPC
     * registration is only allowed.
     *
     * @param epRendererAugmentation
     *        - specific implementation RPC augmentation, if any. Otherwise
     *        NULL
     */
    @Override
    public void register(EpRendererAugmentation epRendererAugmentation) {
        if (epRendererAugmentation != null) {
            registeredRenderers.putIfAbsent(epRendererAugmentation.getClass().getName(), epRendererAugmentation);
            LOG.info("Registered {}", epRendererAugmentation.getClass().getName());
        }
    }

    /**
     * @param regImp the endpoint augmentation
     */
    @Override
    public void unregister(EpRendererAugmentation regImp) {
        if (regImp == null || !registeredRenderers.containsKey(regImp.getClass().getName())) {
            return;
        }
        registeredRenderers.remove(regImp.getClass().getName());
        LOG.info("Unregistered {}", regImp.getClass().getName());
    }

    /**
     * Constructor
     *
     * @param dataProvider the {@link DataBroker}
     */
    public EndpointRpcRegistry(DataBroker dataProvider) {
        this.dataProvider = dataProvider;

        if (dataProvider != null) {
            InstanceIdentifier<Endpoints> iid = InstanceIdentifier.builder(Endpoints.class).build();
            WriteTransaction t = this.dataProvider.newWriteOnlyTransaction();
            t.put(LogicalDatastoreType.OPERATIONAL, iid, new EndpointsBuilder().build());
            CheckedFuture<Void, TransactionCommitFailedException> f = t.submit();
            Futures.addCallback(f, new FutureCallback<Void>() {

                @Override
                public void onFailure(Throwable t) {
                    LOG.error("Could not write endpoint base container", t);
                }

                @Override
                public void onSuccess(Void result) {

                }
            }, MoreExecutors.directExecutor());
        }

        // TODO Be alagalah - age out endpoint data and remove
        // endpoint group/condition mappings with no conditions
    }

    @Override
    public void close() {
    }

    /**
     * Construct an endpoint with the appropriate augmentations from the
     * endpoint input. Each concrete implementation can provides its specifics
     * earlier.
     *
     * @param input
     *        the input object
     */
    private EndpointBuilder buildEndpoint(RegisterEndpointInput input) {
        EndpointBuilder eb = new EndpointBuilder(input);
        for (Entry<String, EpRendererAugmentation> entry : registeredRenderers.entrySet()) {
            try {
                Map.Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> augmentationEntry =
                        entry.getValue().buildEndpointAugmentation(input);
                if (augmentationEntry != null) {
                    eb.addAugmentation(augmentationEntry.getKey(), augmentationEntry.getValue());
                }
            } catch (Exception e) {
                LOG.warn("Endpoint Augmentation error while processing " + entry.getKey() + ". Reason: ", e);
            }
        }
        return eb;
    }

    /**
     * Construct an L3 endpoint with the appropriate augmentations from the
     * endpoint input. Each concrete implementation can provides its specifics
     * earlier.
     *
     * @param input
     *        the input object
     */
    private EndpointL3Builder buildEndpointL3(RegisterEndpointInput input) {
        EndpointL3Builder eb = new EndpointL3Builder(input);
        for (Entry<String, EpRendererAugmentation> entry : registeredRenderers.entrySet()) {
            try {
                Map.Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> augmentationEntry =
                        entry.getValue().buildEndpointL3Augmentation(input);
                if (augmentationEntry != null) {
                    eb.addAugmentation(augmentationEntry.getKey(), augmentationEntry.getValue());
                }
            } catch (Exception e) {
                LOG.warn("L3 endpoint Augmentation error while processing " + entry.getKey() + ". Reason: ", e);
            }
        }
        return eb;
    }

    /**
     * Construct an L3 endpoint with the appropriate augmentations from the
     * endpoint input. Each concrete implementation can provides its specifics
     * earlier.
     *
     * @param input
     *        the input object
     */
    private EndpointL3PrefixBuilder buildL3PrefixEndpoint(RegisterL3PrefixEndpointInput input) {
        EndpointL3PrefixBuilder eb = new EndpointL3PrefixBuilder(input);
        for (Entry<String, EpRendererAugmentation> entry : registeredRenderers.entrySet()) {
            try {
                Map.Entry<Class<? extends Augmentation<EndpointL3Prefix>>, Augmentation<EndpointL3Prefix>> augmentationEntry =
                        entry.getValue().buildL3PrefixEndpointAugmentation(input);
                if (augmentationEntry != null) {
                    eb.addAugmentation(augmentationEntry.getKey(), augmentationEntry.getValue());
                }
            } catch (Exception e) {
                LOG.warn("L3 endpoint Augmentation error while processing " + entry.getKey() + ". Reason: ", e);
            }
        }
        return eb;
    }

    @Override
    public Future<RpcResult<Void>> registerEndpoint(RegisterEndpointInput input) {
        long timestamp = System.currentTimeMillis();

        // TODO: Replicate RPC feedback implemented in L3Prefix register for
        // unmet requirements.
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        if (input.getL2Context() != null && input.getMacAddress() != null) {
            Endpoint ep = buildEndpoint(input).setTimestamp(timestamp).build();

            EndpointKey key = new EndpointKey(ep.getL2Context(), ep.getMacAddress());
            t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.endpointIid(key), ep, true);
        }
        if (input.getL3Address() != null) {
            for (L3Address l3addr : input.getL3Address()) {
                EndpointL3Key key3 = new EndpointL3Key(l3addr.getIpAddress(), l3addr.getL3Context());
                EndpointL3 ep3 = buildEndpointL3(input).setIpAddress(key3.getIpAddress())
                    .setL3Context(key3.getL3Context())
                    .setTimestamp(timestamp)
                    .build();
                t.put(LogicalDatastoreType.OPERATIONAL, IidFactory.l3EndpointIid(key3), ep3, true);
            }
        }
        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<Void>> registerL3PrefixEndpoint(RegisterL3PrefixEndpointInput input) {

        if (input.getL3Context() == null) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.RPC, "L3 Prefix Endpoint must have L3Context.")
                .build());
        }
        if (input.getIpPrefix() == null) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.RPC, "L3 Prefix Endpoint must have ip-prefix.")
                .build());
        }

        if (input.getTenant() == null) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.RPC, "L3 Prefix Endpoint must have tenant.")
                .build());
        }

        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        long timestamp = System.currentTimeMillis();

        // TODO: Convert IPPrefix into it's IPv4/IPv6 canonical form.
        // See org.apache.commons.net.util.SubnetUtils.SubnetInfo

        EndpointL3PrefixKey epL3PrefixKey = new EndpointL3PrefixKey(input.getIpPrefix(), input.getL3Context());

        EndpointL3Prefix epL3Prefix = buildL3PrefixEndpoint(input).setTimestamp(timestamp).build();
        InstanceIdentifier<EndpointL3Prefix> iid_l3prefix =
                InstanceIdentifier.builder(Endpoints.class).child(EndpointL3Prefix.class, epL3PrefixKey).build();
        t.put(LogicalDatastoreType.OPERATIONAL, iid_l3prefix, epL3Prefix);

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<Void>> unregisterEndpoint(UnregisterEndpointInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();
        if (input.getL2() != null) {
            for (L2 l2a : input.getL2()) {
                EndpointKey key = new EndpointKey(l2a.getL2Context(), l2a.getMacAddress());
                InstanceIdentifier<Endpoint> iid =
                        InstanceIdentifier.builder(Endpoints.class).child(Endpoint.class, key).build();
                t.delete(LogicalDatastoreType.OPERATIONAL, iid);
            }
        }
        if (input.getL3() != null) {
            for (L3 l3addr : input.getL3()) {
                EndpointL3Key key3 = new EndpointL3Key(l3addr.getIpAddress(), l3addr.getL3Context());
                InstanceIdentifier<EndpointL3> iid_l3 =
                        InstanceIdentifier.builder(Endpoints.class).child(EndpointL3.class, key3).build();
                t.delete(LogicalDatastoreType.OPERATIONAL, iid_l3);
            }
        }
        if (input.getL3Prefix() != null) {
            for (L3Prefix l3prefix : input.getL3Prefix()) {
                EndpointL3PrefixKey key = new EndpointL3PrefixKey(l3prefix.getIpPrefix(), l3prefix.getL3Context());
                InstanceIdentifier<EndpointL3Prefix> iidL3Prefix =
                        InstanceIdentifier.builder(Endpoints.class).child(EndpointL3Prefix.class, key).build();
                t.delete(LogicalDatastoreType.OPERATIONAL, iidL3Prefix);
            }
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<Void>> setEndpointGroupConditions(SetEndpointGroupConditionsInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        ConditionMappingKey key = new ConditionMappingKey(input.getEndpointGroup());

        for (EndpointGroupCondition condition : input.getEndpointGroupCondition()) {
            EndpointGroupConditionKey ckey = new EndpointGroupConditionKey(condition.getCondition());
            InstanceIdentifier<EndpointGroupCondition> iid = InstanceIdentifier.builder(Endpoints.class)
                .child(ConditionMapping.class, key)
                .child(EndpointGroupCondition.class, ckey)
                .build();
            t.put(LogicalDatastoreType.OPERATIONAL, iid, condition);
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, MoreExecutors.directExecutor());
    }

    @Override
    public Future<RpcResult<Void>> unsetEndpointGroupConditions(UnsetEndpointGroupConditionsInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        ConditionMappingKey key = new ConditionMappingKey(input.getEndpointGroup());

        for (EndpointGroupCondition condition : input.getEndpointGroupCondition()) {
            EndpointGroupConditionKey ckey = new EndpointGroupConditionKey(condition.getCondition());
            InstanceIdentifier<EndpointGroupCondition> iid = InstanceIdentifier.builder(Endpoints.class)
                .child(ConditionMapping.class, key)
                .child(EndpointGroupCondition.class, ckey)
                .build();

            t.delete(LogicalDatastoreType.OPERATIONAL, iid);
        }

        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, MoreExecutors.directExecutor());
    }
}
