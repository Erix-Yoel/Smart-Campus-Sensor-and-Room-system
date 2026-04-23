package com.assignment.errors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }

        // Log the actual exception details internally for devs
        LOGGER.log(Level.SEVERE, "Unexpected internal server error intercepted", exception);

        // Hide it from the user
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(java.util.Collections.singletonMap("error", "An unexpected internal server error occurred."))
                .type("application/json")
                .build();
    }
}
