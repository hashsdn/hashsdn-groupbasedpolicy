/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Manage the table that maps the destination address to the next hop
 * for the path as well as applies any relevant routing transformations.
 * @author readams
 */
public class DestinationMapper extends FlowTable {
    public static final short TABLE_ID = 2;
    /**
     * This is the MAC address of the magical router in the sky
     */
    public static final MacAddress ROUTER_MAC = 
            new MacAddress("88:f0:31:b5:12:b5");

    public DestinationMapper(FlowTableCtx ctx) {
        super(ctx);
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(ReadWriteTransaction t, 
                     InstanceIdentifier<Table> tiid,
                     Map<String, FlowCtx> flowMap, 
                     NodeId nodeId, Dirty dirty)
                             throws Exception {
        HashSet<EgKey> visitedEgs = new HashSet<>();
        for (Endpoint e : ctx.endpointManager.getEndpointsForNode(nodeId)) {
            if (e.getTenant() == null || e.getEndpointGroup() == null)
                continue;
            EgKey key = new EgKey(e.getTenant(), e.getEndpointGroup());
            syncEPG(t, tiid, flowMap, nodeId, key, visitedEgs);
            
            Set<EgKey> peers = ctx.policyResolver
                    .getProvidersForConsumer(e.getTenant(), 
                                             e.getEndpointGroup());
            syncEgKeys(t, tiid, flowMap, nodeId, peers, visitedEgs);
            peers = ctx.policyResolver
                    .getConsumersForProvider(e.getTenant(), 
                                             e.getEndpointGroup());
            syncEgKeys(t, tiid, flowMap, nodeId, peers, visitedEgs);
        }
    }
    
    private void syncEgKeys(ReadWriteTransaction t, 
                            InstanceIdentifier<Table> tiid,
                            Map<String, FlowCtx> flowMap, 
                            NodeId nodeId,
                            Set<EgKey> peers,
                            HashSet<EgKey> visitedEgs) throws Exception {
        for (EgKey key : peers) {
            syncEPG(t, tiid, flowMap, nodeId, key, visitedEgs);
        }
    }

    // set up next-hop destinations for all the endpoints in the endpoint
    // group on the node
    private void syncEPG(ReadWriteTransaction t, 
                         InstanceIdentifier<Table> tiid,
                         Map<String, FlowCtx> flowMap, 
                         NodeId nodeId,
                         EgKey key,
                         HashSet<EgKey> visitedEgs) throws Exception {
        if (visitedEgs.contains(key)) return;
        visitedEgs.add(key);
        
        Collection<Endpoint> egEps = ctx.endpointManager
                .getEndpointsForGroup(key.getTenantId(), key.getEgId());
        for (Endpoint e : egEps) {
            if (e.getTenant() == null || e.getEndpointGroup() == null)
                continue;
            OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
            if (ofc == null || ofc.getNodeId() == null) continue;
            
            syncEPL2(t, tiid, flowMap, nodeId, e, ofc, key);
        }
    }

    private void syncEPL2(ReadWriteTransaction t,
                          InstanceIdentifier<Table> tiid,
                          Map<String, FlowCtx> flowMap, 
                          NodeId nodeId, 
                          Endpoint e, OfOverlayContext ofc,
                          EgKey key) 
                                 throws Exception {

        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        int order = 0;

        String nextHop;
        if (LocationType.External.equals(ofc.getLocationType())) {
            // XXX - TODO - perform NAT and send to the external network
            nextHop = "external";
            LOG.warn("External endpoints not yet supported");
            return;
        } else {
            Action setDlSrc = FlowUtils.setDlSrc(ROUTER_MAC);
            Action setDlDst = FlowUtils.setDlDst(e.getMacAddress());
            Action decTtl = FlowUtils.decNwTtl();

            if (Objects.equals(ofc.getNodeId(), nodeId)) {
                // this is a local endpoint
                nextHop = ofc.getNodeConnectorId().getValue();

                instructions.add(new InstructionBuilder()
                    .setOrder(order++)
                    .setInstruction(FlowUtils.outputActionIns(ofc.getNodeConnectorId()))
                    .build());
                l3instructions.add(new InstructionBuilder()
                    .setOrder(order)
                    .setInstruction(FlowUtils.writeActionIns(setDlSrc,
                                                             setDlDst,
                                                             decTtl))
                    .build());
            } else {
                // this endpoint is on a different switch; send to the 
                // appropriate tunnel

                IpAddress tunDst = 
                        ctx.switchManager.getTunnelIP(ofc.getNodeId());
                NodeConnectorId tunPort =
                        ctx.switchManager.getTunnelPort(nodeId);
                if (tunDst == null) return;
                if (tunPort == null) return;

                if (tunDst.getIpv4Address() != null) {
                    nextHop = tunDst.getIpv4Address().getValue();
                    
                    // XXX - TODO Add action: set tunnel dst to tunDst ipv4 
                } else if (tunDst.getIpv6Address() != null) {
                    nextHop = tunDst.getIpv6Address().getValue();

                    // XXX - TODO Add action: set tunnel dst to tunDst ipv6 
                } else {
                    // this shouldn't happen
                    LOG.error("Tunnel IP for {} invalid", ofc.getNodeId());
                    return;
                }
                
                // XXX - TODO Add action: set tunnel_id from sEPG register
                instructions.add(new InstructionBuilder()
                    .setOrder(order++)
                    .setInstruction(FlowUtils.outputActionIns(tunPort))
                    .build());
                l3instructions.add(new InstructionBuilder()
                    .setOrder(order)
                    .setInstruction(FlowUtils.writeActionIns(setDlSrc, decTtl))
                    .build());

            }
        }
        Instruction gotoTable = new InstructionBuilder()
            .setOrder(order++)
            .setInstruction(FlowUtils.gotoTableIns((short)(getTableId()+1)))
            .build();
        instructions.add(gotoTable);
        l3instructions.add(gotoTable);

        FlowId flowid = new FlowId(new StringBuilder()
            .append(e.getL2Context())
            .append("|l2|")
            .append(e.getMacAddress())
            .append("|")
            .append(nextHop)
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            // XXX TODO add match against bridge domain register
            FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(50))
                .setMatch(new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(null, 
                                                          e.getMacAddress(), 
                                                          null))
                    .build())
                .setInstructions(new InstructionsBuilder()
                    .setInstruction(instructions)
                    .build());
            
            writeFlow(t, tiid, flowb.build());
        }
        if (e.getL3Address() == null) return;
        for (L3Address l3a : e.getL3Address()) {
            if (l3a.getIpAddress() == null || l3a.getL3Context() == null)
                continue;
            Layer3Match m = null;
            Long etherType = null;
            String ikey = null;
            if (l3a.getIpAddress().getIpv4Address() != null) {
                ikey = l3a.getIpAddress().getIpv4Address().getValue();
                etherType = FlowUtils.IPv4;
                m = new Ipv4MatchBuilder()
                    .setIpv4Destination(new Ipv4Prefix(ikey))
                    .build();
            } else if (l3a.getIpAddress().getIpv6Address() != null) {
                ikey = l3a.getIpAddress().getIpv6Address().getValue();
                etherType = FlowUtils.IPv6;
                m = new Ipv6MatchBuilder()
                    .setIpv6Destination(new Ipv6Prefix(ikey))
                    .build();
            } else
                continue;

            flowid = new FlowId(new StringBuilder()
                .append(l3a.getL3Context())
                .append("|l3|")
                .append(l3a.getIpAddress())
                .append("|")
                .append(nextHop)
                .toString());
            if (visit(flowMap, flowid.getValue())) {
                // XXX TODO add match against routing domain register

                FlowBuilder flowb = base()
                    .setId(flowid)
                    .setPriority(Integer.valueOf(132))
                    .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(null, 
                                                                  ROUTER_MAC, 
                                                                  etherType))
                        .setLayer3Match(m)
                        .build())
                    .setInstructions(new InstructionsBuilder()
                        .setInstruction(l3instructions)
                        .build());
                
                writeFlow(t, tiid, flowb.build());
            }
        }
    }
}
