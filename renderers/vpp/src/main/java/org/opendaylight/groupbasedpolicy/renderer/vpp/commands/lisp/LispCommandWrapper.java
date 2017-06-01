/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.GbpGpeEntryDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.GpeEnableDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.InterfaceDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LispDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LocalMappingDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LocatorSetDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapRegisterDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapResolverDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapServerDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.VniTableDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.VrfSubtableDom;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.hmac.key.grouping.HmacKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServer;

import java.util.Arrays;

/**
 * Created by Shakib Ahmed on 3/21/17.
 */
public class LispCommandWrapper {
    public static AbstractLispCommand<Lisp> enableLisp() {
        LispDom lispDom = new LispDom();
        lispDom.setEnabled(true);
        return new ConfigureLispStatusCommand(lispDom);
    }

    public static AbstractLispCommand<LocatorSet> addLocatorSet(String locatorName,
                                                                String interfaceName,
                                                                short priority,
                                                                short weight) {
        InterfaceDom interfaceDom = new InterfaceDom();
        interfaceDom.setInterfaceName(interfaceName);
        interfaceDom.setPriority(priority);
        interfaceDom.setWeight(weight);

        LocatorSetDom locatorSetDom = new LocatorSetDom();
        locatorSetDom.setInterfaces(Arrays.asList(interfaceDom.getSALObject()));
        locatorSetDom.setLocatorName(locatorName);
        return new ConfigureLocatorSetCommand(locatorSetDom);
    }

    public static AbstractLispCommand<VniTable> mapVniToVrf(long vni, long vrfTableId) {
        VrfSubtableDom vrfSubtableDom = new VrfSubtableDom();
        vrfSubtableDom.setTableId(vrfTableId);

        VniTableDom vniTableDom = new VniTableDom();
        vniTableDom.setVirtualNetworkIdentifier(vni);
        vniTableDom.setVrfSubtable(vrfSubtableDom.getSALObject());

        return new ConfigureVrfToVniMappingCommand(vniTableDom);
    }

    public static AbstractLispCommand<MapRegister> enableMapRegister() {
        MapRegisterDom mapRegisterDom = new MapRegisterDom();
        mapRegisterDom.setEnabled(true);
        return new ConfigureMapRegisterStatusCommand(mapRegisterDom);
    }

    public static AbstractLispCommand<MapResolver> addMapResolver(IpAddress ipAddress) {
        MapResolverDom mapResolverDom = new MapResolverDom();
        mapResolverDom.setIpAddress(ipAddress);

        return new ConfigureMapResolverCommand(mapResolverDom);
    }

    public static AbstractLispCommand<MapServer> addMapServer(IpAddress ipAddress) {
        MapServerDom mapServerDom = new MapServerDom();
        mapServerDom.setIpAddress(ipAddress);

        return new ConfigureMapServerCommand(mapServerDom);
    }

    public static AbstractLispCommand<LocalMapping> addLocalMappingInEidTable(String mappingName,
                                                                              Eid eid,
                                                                              String locatorName,
                                                                              HmacKey hmacKey) {
        LocalMappingDom localMappingDom = new LocalMappingDom();
        localMappingDom.setMappingId(new MappingId(mappingName));
        localMappingDom.setEid(eid);
        localMappingDom.setLocatorName(locatorName);
        localMappingDom.setHmacKey(hmacKey);

        return new ConfigureLocalMappingInEidTableCommand(localMappingDom);
    }

    public static AbstractLispCommand<LocalMapping> deleteLocalMappingFromEidTable(String mappingName,
                                                                                   long vni) {
        LocalMappingDom localMappingDom = new LocalMappingDom();
        localMappingDom.setMappingId(new MappingId(mappingName));
        localMappingDom.setVni(vni);

        return new ConfigureLocalMappingInEidTableCommand(localMappingDom);
    }

    public static AbstractLispCommand<LispFeatureData> deleteLispFeatureData() {
        return new DeleteLispFeatureDataCommand();
    }

    public static AbstractLispCommand<GpeFeatureData> enableGpe() {
        GpeEnableDom gpeEnableDom = new GpeEnableDom();
        gpeEnableDom.setEnabled(true);

        return new ConfigureGpeCommand(gpeEnableDom);
    }

    public static AbstractLispCommand<GpeEntry> addGpeSendMapregisterAction(String entryName,
                                                                            RemoteEid rEid,
                                                                            long vni,
                                                                            long vrf) {
        GbpGpeEntryDom gpeEntryDom = new GbpGpeEntryDom();
        gpeEntryDom.setId(entryName);
        gpeEntryDom.setRemoteEid(rEid);
        gpeEntryDom.setVni(vni);
        gpeEntryDom.setVrf(vrf);
        gpeEntryDom.setAction(MapReplyAction.SendMapRequest);

        return new ConfigureGpeEntryCommand(gpeEntryDom);
    }

    public static AbstractLispCommand<GpeEntry> deleteGpeEntry(String entryName) {
        GbpGpeEntryDom gpeEntryDom = new GbpGpeEntryDom();
        gpeEntryDom.setId(entryName);

        return new ConfigureGpeEntryCommand(gpeEntryDom);
    }
}