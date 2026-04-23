package com.assignment.resources;

import com.assignment.exceptions.SensorUnavailableException;
import com.assignment.models.ApiResponse;
import com.assignment.models.Sensor;
import com.assignment.models.SensorReading;
import com.assignment.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class SensorReadingResource {

    private static final Logger LOGGER = Logger.getLogger(SensorReadingResource.class.getName());
    private final DataStore store = DataStore.getInstance();
    private final Sensor parentSensor;

    // Required by JAX-RS validation during classpath scanning
    public SensorReadingResource() {
        this.parentSensor = null;
    }

    // Injected by the Sub-Resource Locator (SensorResource)
    public SensorReadingResource(Sensor parentSensor) {
        this.parentSensor = parentSensor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadingHistory() {
        LOGGER.info("Fetching reading history for sensor ID: " + parentSensor.getId());
        List<SensorReading> history = store.getReadings().get(parentSensor.getId());
        if (history == null || history.isEmpty()) {
            return Response.ok(new ApiResponse<>("Reading history retrieved successfully", java.util.Collections.emptyList())).build();
        }
        return Response.ok(new ApiResponse<>("Reading history retrieved successfully", history)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response appendReading(SensorReading reading) {
        LOGGER.info("Appending reading for sensor ID: " + parentSensor.getId());
        if ("MAINTENANCE".equalsIgnoreCase(parentSensor.getStatus())) {
            throw new SensorUnavailableException("Sensor is under maintenance and cannot accept readings.");
        }

        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.addReadingForSensor(parentSensor.getId(), reading);
        
        // Side Effect: update parent sensor current value
        parentSensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(new ApiResponse<>("Reading appended successfully", reading)).build();
    }
}
