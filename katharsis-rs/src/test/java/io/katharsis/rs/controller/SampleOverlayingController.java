package io.katharsis.rs.controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.WILDCARD)
@Produces(MediaType.TEXT_PLAIN)
@Path("tasks")
public class SampleOverlayingController {

    public static final String NON_KATHARSIS_RESOURCE_OVERLAY_RESPONSE = "NON_KATHARSIS_RESOURCE_OVERLAY_RESPONSE";

    @GET
    @Path("{id}")
    public Response getRequest(@PathParam("id") final int taskId) {
        return Response.ok(NON_KATHARSIS_RESOURCE_OVERLAY_RESPONSE).build();
    }

    @POST
    @Path("{id}")
    public Response postRequest(@PathParam("id") final int taskId) {
        return Response.ok(NON_KATHARSIS_RESOURCE_OVERLAY_RESPONSE).build();
    }

}
