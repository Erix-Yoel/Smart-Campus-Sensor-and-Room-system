package com.assignment.errors;

import com.assignment.exceptions.LinkedResourceNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        // HTTP 422 is typically distinct in some JAX-RS containers, but mapping it manually via 422 integer.
        return Response.status(422)
                .entity(java.util.Collections.singletonMap("error", exception.getMessage()))
                .type("application/json")
                .build();
    }
}
