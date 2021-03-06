/*
 * Copyright oVirt Authors
 * SPDX-License-Identifier: Apache-2.0
*/

package org.ovirt.engine.api.v3.adapters;

import static org.ovirt.engine.api.v3.adapters.V3OutAdapters.adaptOut;

import org.ovirt.engine.api.model.Permission;
import org.ovirt.engine.api.v3.V3Adapter;
import org.ovirt.engine.api.v3.types.V3Permission;

public class V3PermissionOutAdapter implements V3Adapter<Permission, V3Permission> {
    @Override
    public V3Permission adapt(Permission from) {
        V3Permission to = new V3Permission();
        if (from.isSetLinks()) {
            to.getLinks().addAll(adaptOut(from.getLinks()));
        }
        if (from.isSetActions()) {
            to.setActions(adaptOut(from.getActions()));
        }
        if (from.isSetCluster()) {
            to.setCluster(adaptOut(from.getCluster()));
        }
        if (from.isSetComment()) {
            to.setComment(from.getComment());
        }
        if (from.isSetDataCenter()) {
            to.setDataCenter(adaptOut(from.getDataCenter()));
        }
        if (from.isSetDescription()) {
            to.setDescription(from.getDescription());
        }
        if (from.isSetDisk()) {
            to.setDisk(adaptOut(from.getDisk()));
        }
        if (from.isSetGroup()) {
            to.setGroup(adaptOut(from.getGroup()));
        }
        if (from.isSetHost()) {
            to.setHost(adaptOut(from.getHost()));
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
        if (from.isSetRole()) {
            to.setRole(adaptOut(from.getRole()));
        }
        if (from.isSetStorageDomain()) {
            to.setStorageDomain(adaptOut(from.getStorageDomain()));
        }
        if (from.isSetTemplate()) {
            to.setTemplate(adaptOut(from.getTemplate()));
        }
        if (from.isSetUser()) {
            to.setUser(adaptOut(from.getUser()));
        }
        if (from.isSetVm()) {
            to.setVm(adaptOut(from.getVm()));
        }
        if (from.isSetVmPool()) {
            to.setVmpool(adaptOut(from.getVmPool()));
        }
        return to;
    }
}
