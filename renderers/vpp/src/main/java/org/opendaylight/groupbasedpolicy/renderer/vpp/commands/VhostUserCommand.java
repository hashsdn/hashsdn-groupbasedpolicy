/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816.NatInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.BridgeBasedBuilder;

public class VhostUserCommand extends AbstractInterfaceCommand {

    private String socket;
    private VhostUserRole role;

    private VhostUserCommand(VhostUserCommandBuilder builder) {
        this.name = builder.getName();
        this.operation = builder.getOperation();
        this.socket = builder.getSocket();
        this.role = builder.getRole();
        this.enabled = builder.isEnabled();
        this.snatEnabled = builder.isSnatEnabled();
        this.description = builder.getDescription();
        this.bridgeDomain = builder.getBridgeDomain();
        this.enableProxyArp = builder.getEnableProxyArp();

    }

    public static VhostUserCommandBuilder builder() {
        return new VhostUserCommandBuilder();
    }

    public String getSocket() {
        return socket;
    }

    public VhostUserRole getRole() {
        return role;
    }

    @Override
    public InterfaceBuilder getInterfaceBuilder() {
        InterfaceBuilder interfaceBuilder =
            new InterfaceBuilder().setKey(new InterfaceKey(name))
                .setEnabled(enabled)
                .setDescription(description)
                .setType(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VhostUser.class)
                .setName(name)
                .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled);

        // Create the vhost augmentation
        VppInterfaceAugmentationBuilder vppAugmentationBuilder = new VppInterfaceAugmentationBuilder()
                .setVhostUser(new VhostUserBuilder().setRole(role).setSocket(socket).build());

        if (getVrfId() != null) {
            vppAugmentationBuilder.setRouting(new RoutingBuilder().setIpv4VrfId(getVrfId()).build());
        }

        if (!Strings.isNullOrEmpty(bridgeDomain)) {
            vppAugmentationBuilder.setL2(new L2Builder()
                    .setInterconnection(new BridgeBasedBuilder().setBridgeDomain(bridgeDomain).build()).build());
        }
        if (snatEnabled) {
            Nat nat = new NatBuilder().setInbound(new InboundBuilder()
                    .setNat44Support(true)
                    .setPostRouting(ConfigUtil.getInstance().isL3FlatEnabled()).build()).build();
            interfaceBuilder.addAugmentation(NatInterfaceAugmentation.class,
                    new NatInterfaceAugmentationBuilder().setNat(nat).build());
        }
        interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilder.build());
        addEnableProxyArpAugmentation(interfaceBuilder);
        return interfaceBuilder;
    }

    @Override
    public String toString() {
        return "VhostUserCommand [socket=" + socket + ", role=" + role + ", bridgeDomain=" + bridgeDomain
                + ", operation=" + operation + ", name=" + name + ", description=" + description + ", enabled="
                + enabled + ", enableProxyArp=" + enableProxyArp + ", vrfId=" + vrfId + ", snatEnabled=" + snatEnabled
            + "]";
    }

    public static class VhostUserCommandBuilder {

        private String name;
        private General.Operations operation;
        private String socket;
        private VhostUserRole role = VhostUserRole.Server;
        private boolean enabled = true;
        private String description;
        private String bridgeDomain;
        private Boolean enableProxyArp;
        private Long vrfId;
        private boolean snatEnabled;

        public String getName() {
            return name;
        }

        public VhostUserCommandBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public General.Operations getOperation() {
            return operation;
        }

        public VhostUserCommandBuilder setOperation(General.Operations operation) {
            this.operation = operation;
            return this;
        }

        public String getSocket() {
            return socket;
        }

        public VhostUserCommandBuilder setSocket(String socket) {
            this.socket = socket;
            return this;
        }

        VhostUserRole getRole() {
            return role;
        }

        public VhostUserCommandBuilder setRole(VhostUserRole role) {
            this.role = role;
            return this;
        }

        boolean isEnabled() {
            return enabled;
        }

        VhostUserCommandBuilder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        boolean isSnatEnabled() {
            return snatEnabled;
        }

        public VhostUserCommandBuilder setSnatEnabled(boolean snatEnabled) {
            this.snatEnabled = snatEnabled;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public VhostUserCommandBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        String getBridgeDomain() {
            return bridgeDomain;
        }

        VhostUserCommandBuilder setBridgeDomain(String bridgeDomain) {
            this.bridgeDomain = bridgeDomain;
            return this;
        }

        public Boolean getEnableProxyArp() {
            return enableProxyArp;
        }

        public void setEnableProxyArp(Boolean enableProxyArp) {
            this.enableProxyArp = enableProxyArp;
        }

        public Long getVrfId() {
            return vrfId;
        }

        public void setVrfId(Long vrfId) {
            this.vrfId = vrfId;
        }

        /**
         * VhostUserCommand build method.
         *
         * @return VhostUserCommand
         * @throws IllegalArgumentException if name, operation or socket is null.
         */
        public VhostUserCommand build() {
            Preconditions.checkArgument(this.name != null);
            Preconditions.checkArgument(this.operation != null);
            if (operation == Operations.PUT) {
                Preconditions.checkArgument(this.socket != null);
            }

            return new VhostUserCommand(this);
        }
    }
}
