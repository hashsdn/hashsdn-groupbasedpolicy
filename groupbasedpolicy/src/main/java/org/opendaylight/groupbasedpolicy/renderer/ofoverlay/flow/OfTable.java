/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Manage the state of a openflow  table by reacting to any events and updating
 * the table state.  This is an abstract class that must be extended for
 * each specific table being managed.
 * @author readams
 */
public abstract class OfTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(OfTable.class);

    protected final OfTableCtx ctx;

    public OfTable(OfTableCtx ctx) {
        super();
        this.ctx = ctx;
    }

    /**
     * The context needed for flow tables
     */
    public static class OfTableCtx {
        protected final DataBroker dataBroker;
        protected final RpcProviderRegistry rpcRegistry;
    
        protected final PolicyManager policyManager;
        protected final SwitchManager switchManager;
        protected final EndpointManager epManager;
    
        protected final PolicyResolver policyResolver;
    
        protected final ScheduledExecutorService executor;
    
        public OfTableCtx(DataBroker dataBroker,
                          RpcProviderRegistry rpcRegistry,
                          PolicyManager policyManager,
                          PolicyResolver policyResolver,
                          SwitchManager switchManager,
                          EndpointManager endpointManager,
                          ScheduledExecutorService executor) {
            super();
            this.dataBroker = dataBroker;
            this.rpcRegistry = rpcRegistry;
            this.policyManager = policyManager;
            this.switchManager = switchManager;
            this.epManager = endpointManager;
            this.policyResolver = policyResolver;
            this.executor = executor;
        }
    
    }

    // *******
    // OfTable
    // *******

    /**
     * Update the relevant flow table for the node
     * @param nodeId the node to update
     * @param dirty the dirty set
     * @throws Exception
     */
    public abstract void update(NodeId nodeId, 
                                PolicyInfo policyInfo,
                                Dirty dirty) throws Exception;
    
    // ***************
    // Utility methods
    // ***************


    /**
     * Generic callback for handling result of flow manipulation
     * @author readams
     *
     * @param <T> the expected output type
     */
    protected static class OfCallback<T> implements FutureCallback<T> {
        @Override
        public void onSuccess(T result) {
        }

        @Override
        public void onFailure(Throwable t) {
            LOG.error("Failed to add flow entry", t);
        }
    }
    protected static final OfCallback<Void> updateCallback =
            new OfCallback<>();


}
