/*
 * Copyright oVirt Authors
 * SPDX-License-Identifier: Apache-2.0
*/

package org.ovirt.engine.api.v3.adapters;

import static org.ovirt.engine.api.v3.adapters.V3OutAdapters.adaptOut;

import org.ovirt.engine.api.model.Version;
import org.ovirt.engine.api.v3.V3Adapter;
import org.ovirt.engine.api.v3.types.V3Version;

public class V3VersionOutAdapter implements V3Adapter<Version, V3Version> {
    @Override
    public V3Version adapt(Version from) {
        V3Version to = new V3Version();
        if (from.isSetLinks()) {
            to.getLinks().addAll(adaptOut(from.getLinks()));
        }
        if (from.isSetActions()) {
            to.setActions(adaptOut(from.getActions()));
        }
        if (from.isSetBuild()) {
            to.setBuild(from.getBuild());
        }
        if (from.isSetComment()) {
            to.setComment(from.getComment());
        }
        if (from.isSetDescription()) {
            to.setDescription(from.getDescription());
        }
        if (from.isSetFullVersion()) {
            to.setFullVersion(from.getFullVersion());
        }
        if (from.isSetId()) {
            to.setId(from.getId());
        }
        if (from.isSetHref()) {
            to.setHref(from.getHref());
        }
        if (from.isSetMajor()) {
            to.setMajor(from.getMajor());
        }
        if (from.isSetMinor()) {
            to.setMinor(from.getMinor());
        }
        if (from.isSetName()) {
            to.setName(from.getName());
        }
        if (from.isSetRevision()) {
            to.setRevision(from.getRevision());
        }
        return to;
    }
}
