package com.assignment.errors;

import com.assignment.exceptions.SensorUnavailableException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(java.util.Collections.singletonMap("error", exception.getMessage()))
                .type("application/json")
                .build();
    }
}
