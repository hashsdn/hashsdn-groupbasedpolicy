/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util;

import javax.annotation.Nonnull;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.IpPrefixEqualCommand;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;

/**
 * Purpose: provide simple equal using direct invocation of {@link Object#equals(Object)} method
 */
public class IpPrefixEqualCommandDirectImpl implements IpPrefixEqualCommand {
    private final IpPrefix myValue;

    public IpPrefixEqualCommandDirectImpl(@Nonnull final IpPrefix myValue) {
        this.myValue = myValue;
    }

    @Override
    public boolean isEqualTo(final IpPrefix value) {
        return myValue.equals(value);
    }
}
