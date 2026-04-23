package com.assignment.store;

import com.assignment.models.Room;
import com.assignment.models.Sensor;
import com.assignment.models.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DataStore {
    private static final DataStore instance = new DataStore();

    // Use ConcurrentHashMap for thread-safe operations across concurrent requests
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();
    
    private final AtomicInteger sensorIdCounter = new AtomicInteger(1);

    private DataStore() {
        // Pre-populate some dummy info if needed
    }

    public static DataStore getInstance() {
        return instance;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }
    
    public List<Sensor> getSensorsByRoom(String roomId) {
        return sensors.values().stream()
                .filter(s -> s.getRoomId().equals(roomId))
                .collect(Collectors.toList());
    }

    public Map<String, List<SensorReading>> getReadings() {
        return readings;
    }

    public void addReadingForSensor(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
    
    public String generateSensorId() {
        return "sen_" + sensorIdCounter.getAndIncrement();
    }
}
