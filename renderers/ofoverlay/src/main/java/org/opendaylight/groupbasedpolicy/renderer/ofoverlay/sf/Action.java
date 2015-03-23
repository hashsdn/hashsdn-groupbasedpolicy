/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;

/**
 * Represent an action definition, and provide tools for generating
 * flow instructions based on the action
 * @author tbachman
 */
public abstract class Action {
    /**
     * Get the action definition for this action
     * @return the {@link ActionDefinition} for this action
     */
    public abstract ActionDefinitionId getId();

    /**
     * Get the action definition for this action
     * @return the {@link ActionDefinition} for this action
     */
    public abstract ActionDefinition getActionDef();

    /**
     * Construct a set of actions that will apply to the traffic.  Augment
     * the existing list of actions or add new actions.  It's important
     * that the order of the returned list be consistent however
     * @param actions The existing actions
     * @param params the parameters for the action instance
     * @return the updated list of actions (may be a different length)
     */
    public abstract List<ActionBuilder> updateAction(List<ActionBuilder> actions,
                                                     Map<String, Object> params,
                                                     Integer i);
}