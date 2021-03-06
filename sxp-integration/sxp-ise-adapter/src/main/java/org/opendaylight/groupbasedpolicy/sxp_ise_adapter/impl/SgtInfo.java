/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Purpose: simple holder for {@link Sgt} and name
 */
public class SgtInfo {

    private final Sgt sgt;
    private final String name;
    private final String uuid;

    /**
     * @param sgt  value to hold
     * @param name value to hold
     * @param uuid
     */
    public SgtInfo(@Nonnull final Sgt sgt, @Nullable final String name, final String uuid) {
        this.sgt = sgt;
        this.name = name;
        this.uuid = uuid;
    }

    /**
     * @return sgt
     */
    public Sgt getSgt() {
        return sgt;
    }

    /**
     * @return name associated to sgt
     */
    public String getName() {
        return name;
    }

    /**
     * @return uuid of sgt
     */
    public String getUuid() {
        return uuid;
    }
}
