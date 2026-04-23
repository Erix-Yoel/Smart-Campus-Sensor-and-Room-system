package com.assignment.resources;

import com.assignment.exceptions.LinkedResourceNotFoundException;
import com.assignment.models.ApiResponse;
import com.assignment.models.Sensor;
import com.assignment.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Path("/sensors")
public class SensorResource {

    private static final Logger LOGGER = Logger.getLogger(SensorResource.class.getName());
    private final DataStore store = DataStore.getInstance();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSensors(@QueryParam("type") String type) {
        LOGGER.info("Fetching sensors, type filter: " + type);
        List<Sensor> allSensors = new java.util.ArrayList<>(store.getSensors().values());
        
        if (type != null && !type.isEmpty()) {
            List<Sensor> filtered = allSensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
            return Response.ok(new ApiResponse<>("Sensors retrieved successfully", filtered)).build();
        }

        return Response.ok(new ApiResponse<>("Sensors retrieved successfully", allSensors)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        LOGGER.info("Creating sensor for room ID: " + sensor.getRoomId());
        if (sensor.getRoomId() == null || store.getRooms().get(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException("The specified room ID does not exist.");
        }

        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(store.generateSensorId());
        }

        store.getSensors().put(sensor.getId(), sensor);
        return Response.status(Response.Status.CREATED).entity(new ApiResponse<>("Sensor created successfully", sensor)).build();
    }

    // Sub-Resource Locator
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        LOGGER.info("Accessing readings sub-resource for sensor ID: " + sensorId);
        Sensor parentSensor = store.getSensors().get(sensorId);
        if (parentSensor == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return new SensorReadingResource(parentSensor);
    }
}
