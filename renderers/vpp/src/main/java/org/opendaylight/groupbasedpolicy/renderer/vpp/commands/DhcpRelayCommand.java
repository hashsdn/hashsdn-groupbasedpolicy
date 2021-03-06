/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.Server;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import java.util.List;

public class DhcpRelayCommand extends AbstractConfigCommand {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpRelayCommand.class);
    private Long rxVrfId;
    private IpAddress gatewayIpAddress;
    private List<Server> serverIpAddresses;
    private Class<? extends AddressFamily> addressType;
    private NodeId vppNodeId;

    private DhcpRelayCommand(DhcpRelayBuilder builder) {
        operation = builder.getOperation();
        rxVrfId = builder.getRxVrfId();
        gatewayIpAddress = builder.getGatewayIpAddress();
        serverIpAddresses = builder.getServerIpAddresses();
        addressType = builder.getAddressType();
        vppNodeId = builder.getVppNodeId();
    }

    public static DhcpRelayBuilder builder() {
        return new DhcpRelayBuilder();
    }

    Long getRxVrfId() {
        return rxVrfId;
    }

    public IpAddress getGatewayIpAddress() {
        return gatewayIpAddress;
    }

    public List<Server>  getServerIpAddresses() {
        return serverIpAddresses;
    }

    public Class<? extends AddressFamily> getAddressType() {
        return addressType;
    }

    @Override public InstanceIdentifier<Relay> getIid() {
        return VppIidFactory.getDhcpRelayIid(getDhcpBuilder().getKey());
    }

    public NodeId getVppNodeId() {
        return vppNodeId;
    }

    void put(ReadWriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, getIid(), getDhcpBuilder().build(), true);
    }

    void merge(ReadWriteTransaction rwTx) {
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, getIid(), getDhcpBuilder().build(), true);
    }

    void delete(ReadWriteTransaction rwTx) {
        try {
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, getIid());
        } catch (IllegalStateException ex) {
            LOG.debug("Routing protocol not deleted from DS {}", this, ex);
        }
    }

    @Override public String toString() {
        return "DhcpProxyCommand [ rxVrfId=" + rxVrfId + ", gatewayIpAddress=" + gatewayIpAddress
            + ", serverIpAddresses=" + serverIpAddresses +  ", addressType=" + addressType
            + ", operations=" + operation + "]";
    }

    /**
     * Compares two DhcpRelayCommands without checking operation status.
     * @param compareTo DhcpRelayCommand to compare with.
     * @return true if commands match, false otherwise.
     */
    @Override public boolean equals(Object compareTo) {
        if (compareTo == null || !compareTo.getClass().equals(this.getClass())) {
            return false;
        }

        DhcpRelayCommand command = (DhcpRelayCommand) compareTo;

        if (!this.getVppNodeId().equals(command.getVppNodeId())) {
            return false;
        } else if (!this.getAddressType().equals(command.getAddressType())) {
            return false;
        } else if (!this.getGatewayIpAddress().equals(command.getGatewayIpAddress())) {
            return false;
        } else if (!this.getIid().equals(command.getIid())) {
            return false;
        } else if (!this.getRxVrfId().equals(command.getRxVrfId())) {
            return false;
        } else if (this.getServerIpAddresses() != null && !this.getServerIpAddresses()
            .containsAll(command.getServerIpAddresses())) {
            return false;
        } else if (command.getServerIpAddresses() != null && !command.getServerIpAddresses()
            .containsAll(this.getServerIpAddresses())) {
            return false;
        }

        return true;
    }

    public RelayBuilder getDhcpBuilder() {
        return new RelayBuilder()
            .setAddressType(addressType)
            .setGatewayAddress(gatewayIpAddress)
            .setKey(new RelayKey(addressType, rxVrfId))
            .setRxVrfId(rxVrfId)
            .setServer(serverIpAddresses);
    }

    public static class DhcpRelayBuilder {

        private General.Operations operation;
        private Long rxVrfId;
        private IpAddress gatewayIpAddress;
        private List<Server>  serverIpAddress;
        private Class<? extends AddressFamily> addressType;
        private NodeId VppNodeId;

        public General.Operations getOperation() {
            return operation;
        }

        public DhcpRelayBuilder setOperation(General.Operations operation) {
            this.operation = operation;
            return this;
        }

        public Long getRxVrfId() {
            return rxVrfId;
        }

        public DhcpRelayBuilder setRxVrfId(Long rxVrfId) {
            this.rxVrfId = rxVrfId;
            return this;
        }

        public IpAddress getGatewayIpAddress() {
            return gatewayIpAddress;
        }

        public DhcpRelayBuilder setGatewayIpAddress(IpAddress gatewayIpAddress) {
            this.gatewayIpAddress = gatewayIpAddress;
            return this;
        }

        public List<Server>  getServerIpAddresses() {
            return serverIpAddress;
        }

        public DhcpRelayBuilder setServerIpAddresses(List<Server>  serverIpAddress) {
            this.serverIpAddress = serverIpAddress;
            return this;
        }

        public Class<? extends AddressFamily> getAddressType() {
            return addressType;
        }

        public DhcpRelayBuilder setAddressType(Class<? extends AddressFamily> addressType) {
            this.addressType = addressType;
            return this;
        }

        public NodeId getVppNodeId() {
            return VppNodeId;
        }

        public DhcpRelayBuilder setVppNodeId(NodeId vppNodeId) {
            VppNodeId = vppNodeId;
            return this;
        }

        /**
         * RoutingCommand build method.
         *
         * @return RoutingCommand
         * @throws IllegalArgumentException if routerProtocol, operation or rxVrfId is null.
         */
        public DhcpRelayCommand build() {
            Preconditions.checkArgument(this.operation != null);
            Preconditions.checkArgument(this.rxVrfId != null);
            Preconditions.checkArgument(this.addressType != null);

            return new DhcpRelayCommand(this);
        }
    }
}
