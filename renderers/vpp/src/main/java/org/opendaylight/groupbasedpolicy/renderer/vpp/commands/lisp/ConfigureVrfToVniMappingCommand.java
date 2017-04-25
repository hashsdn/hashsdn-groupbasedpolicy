/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.VniTableDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by Shakib Ahmed on 3/16/17.
 */
public class ConfigureVrfToVniMappingCommand extends AbstractLispCommand<VniTable> {

    VniTableDom vniTableDom;

    public ConfigureVrfToVniMappingCommand(VniTableDom vniTableDom) {
        this.vniTableDom = vniTableDom;
    }

    @Override
    public InstanceIdentifier<VniTable> getIid() {
        return VppIidFactory.getVniTableIid(new VniTableKey(vniTableDom.getVirtualNetworkIdentifier()));
    }

    @Override
    public VniTable getData() {
        return vniTableDom.getSALObject();
    }
}