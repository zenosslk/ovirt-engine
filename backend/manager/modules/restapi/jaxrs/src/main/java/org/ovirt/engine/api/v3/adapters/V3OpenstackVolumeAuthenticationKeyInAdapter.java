/*
 * Copyright oVirt Authors
 * SPDX-License-Identifier: Apache-2.0
*/

package org.ovirt.engine.api.v3.adapters;

import static org.ovirt.engine.api.v3.adapters.V3InAdapters.adaptIn;

import org.ovirt.engine.api.model.OpenstackVolumeAuthenticationKey;
import org.ovirt.engine.api.model.OpenstackVolumeAuthenticationKeyUsageType;
import org.ovirt.engine.api.v3.V3Adapter;
import org.ovirt.engine.api.v3.types.V3OpenstackVolumeAuthenticationKey;

public class V3OpenstackVolumeAuthenticationKeyInAdapter implements V3Adapter<V3OpenstackVolumeAuthenticationKey, OpenstackVolumeAuthenticationKey> {
    @Override
    public OpenstackVolumeAuthenticationKey adapt(V3OpenstackVolumeAuthenticationKey from) {
        OpenstackVolumeAuthenticationKey to = new OpenstackVolumeAuthenticationKey();
        if (from.isSetLinks()) {
            to.getLinks().addAll(adaptIn(from.getLinks()));
        }
        if (from.isSetActions()) {
            to.setActions(adaptIn(from.getActions()));
        }
        if (from.isSetComment()) {
            to.setComment(from.getComment());
        }
        if (from.isSetCreationDate()) {
            to.setCreationDate(from.getCreationDate());
        }
        if (from.isSetDescription()) {
            to.setDescription(from.getDescription());
        }
        if (from.isSetId()) {
            to.setId(from.getId());
        }
        if (from.isSetHref()) {
            to.setHref(from.getHref());
        }
        if (from.isSetName()) {
            to.setName(from.getName());
        }
        if (from.isSetOpenstackVolumeProvider()) {
            to.setOpenstackVolumeProvider(adaptIn(from.getOpenstackVolumeProvider()));
        }
        if (from.isSetUsageType()) {
            to.setUsageType(OpenstackVolumeAuthenticationKeyUsageType.fromValue(from.getUsageType()));
        }
        if (from.isSetUuid()) {
            to.setUuid(from.getUuid());
        }
        if (from.isSetValue()) {
            to.setValue(from.getValue());
        }
        return to;
    }
}
