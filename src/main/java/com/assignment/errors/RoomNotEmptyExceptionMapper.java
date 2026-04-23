package com.assignment.errors;

import com.assignment.exceptions.RoomNotEmptyException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(java.util.Collections.singletonMap("error", exception.getMessage()))
                .type("application/json")
                .build();
    }
}
