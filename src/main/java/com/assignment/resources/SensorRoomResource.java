package com.assignment.resources;

import com.assignment.exceptions.RoomNotEmptyException;
import com.assignment.models.ApiResponse;
import com.assignment.models.Room;
import com.assignment.models.Sensor;
import com.assignment.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Path("/rooms")
public class SensorRoomResource {

    private static final Logger LOGGER = Logger.getLogger(SensorRoomResource.class.getName());
    private final DataStore store = DataStore.getInstance();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRooms() {
        LOGGER.info("Fetching all rooms");
        return Response.ok(new ApiResponse<>("Rooms retrieved successfully", new ArrayList<>(store.getRooms().values()))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        LOGGER.info("Creating room with ID: " + room.getId());
        store.getRooms().put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(new ApiResponse<>("Room created successfully", room)).build();
    }

    @GET
    @Path("/{roomId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoom(@PathParam("roomId") String roomId) {
        LOGGER.info("Fetching room with ID: " + roomId);
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponse<>("Room not found", null))
                    .build();
        }
        return Response.ok(new ApiResponse<>("Room retrieved successfully", room)).build();
    }

    @DELETE
    @Path("/{roomId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        LOGGER.info("Deleting room with ID: " + roomId);
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponse<>("Room not found", null))
                    .build();
        }

        List<Sensor> activeSensors = store.getSensorsByRoom(roomId);
        if (!activeSensors.isEmpty()) {
            throw new RoomNotEmptyException("Cannot delete room currently occupied by active hardware.");
        }

        store.getRooms().remove(roomId);
        return Response.ok(new ApiResponse<>("Room deleted successfully", null)).build();
    }
}
