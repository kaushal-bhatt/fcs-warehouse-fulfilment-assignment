package com.fulfilment.application.monolith.fulfilment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  @Inject FulfilmentService fulfilmentService;

  /** Assignments to active warehouses, optionally for a single store. */
  @GET
  public List<FulfilmentAssignment> list(@QueryParam("storeId") Long storeId) {
    return fulfilmentService.listActive(storeId);
  }

  @POST
  public Response assign(FulfilmentAssignment request) {
    return Response.status(201).entity(fulfilmentService.assign(request)).build();
  }

  @DELETE
  @Path("{id}")
  public Response unassign(Long id) {
    fulfilmentService.unassign(id);
    return Response.status(204).build();
  }
}
