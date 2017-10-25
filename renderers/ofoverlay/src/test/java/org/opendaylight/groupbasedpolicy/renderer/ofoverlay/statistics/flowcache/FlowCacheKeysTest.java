/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FlowCacheKeysTest {

    @Test
    public void testBuilder() {
        FlowCacheKeys.FlowCacheKeysBuilder builder = null;
        try {
            builder = FlowCacheKeys.builder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(builder);
    }

}
