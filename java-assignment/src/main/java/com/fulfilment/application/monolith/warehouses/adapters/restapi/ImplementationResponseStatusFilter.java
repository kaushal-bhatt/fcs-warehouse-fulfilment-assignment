package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * RESTEasy reads {@link ResponseStatus} from the generated interface method, where we cannot add
 * it. This filter applies the annotation when it is declared on the implementing method instead
 * (e.g. 201 for creates), and only for successful default responses.
 */
public class ImplementationResponseStatusFilter {

  @ServerResponseFilter
  public void applyResponseStatus(ContainerResponseContext response, ResourceInfo resourceInfo) {
    if (response.getStatus() != 200 || resourceInfo.getResourceClass() == null) {
      return;
    }
    Method declared = resourceInfo.getResourceMethod();
    try {
      Method implementation =
          resourceInfo
              .getResourceClass()
              .getMethod(declared.getName(), declared.getParameterTypes());
      ResponseStatus status = implementation.getAnnotation(ResponseStatus.class);
      if (status != null) {
        response.setStatus(status.value());
      }
    } catch (NoSuchMethodException e) {
      // not an interface-based resource method: nothing to apply
    }
  }
}
