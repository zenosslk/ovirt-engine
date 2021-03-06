/*
 * Copyright oVirt Authors
 * SPDX-License-Identifier: Apache-2.0
*/

package org.ovirt.engine.api.v3.servers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.ovirt.engine.api.resource.StatisticsResource;
import org.ovirt.engine.api.v3.V3Server;
import org.ovirt.engine.api.v3.types.V3Statistics;

@Produces({"application/xml", "application/json"})
public class V3StatisticsServer extends V3Server<StatisticsResource> {
    public V3StatisticsServer(StatisticsResource delegate) {
        super(delegate);
    }

    @GET
    public V3Statistics list() {
        return adaptList(getDelegate()::list);
    }

    @Path("{id}")
    public V3StatisticServer getStatisticResource(@PathParam("id") String id) {
        return new V3StatisticServer(getDelegate().getStatisticResource(id));
    }
}
