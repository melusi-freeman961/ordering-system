package com.kasigrill.ordering_system.whatsapp;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WhatsappPayloadParser {

    public boolean isStatusUpdatePayload(Map<String, Object> payload) {
        // Dig into the map structure to see if "statuses" is present instead of "messages"
        // Meta uses {"entry": [{"changes": [{"value": {"statuses": [...]}}]}]} for receipts
        try {
            List<?> entry = (List<?>) payload.get("entry");
            Map<?, ?> entryMap = (Map<?, ?>) entry.get(0);
            List<?> changes = (List<?>) entryMap.get("changes");
            Map<?, ?> changeMap = (Map<?, ?>) changes.get(0);
            Map<?, ?> value = (Map<?, ?>) changeMap.get("value");

            if (value.containsKey("statuses") && value.get("statuses") != null) {
                System.out.println("====== METRIC STATUS UPDATE DETECTED ======");
                System.out.println(value.get("statuses"));
                System.out.println("===========================================");
                return true;
            }
            return false;// Returns true if it's just a read/delivery receipt
        } catch (Exception e) {
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    public Map<String,Object> getMessage(Map<String, Object> payload){

        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
        Map<String, Object> changes = (Map<String, Object>) ((List<Map<String, Object>>) entries.getFirst().get("changes")).getFirst();
        Map<String, Object> value = (Map<String, Object>) changes.get("value");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

        // If there are no messages (e.g., it's a delivery status update), exit early
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // 2. Get the first message object
        Map<String, Object> message = messages.getFirst();
        return message;
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> extractLocation(Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entry = (List<Map<String, Object>>) payload.get("entry");
            if (entry != null && !entry.isEmpty()) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get(0).get("changes");
                if (changes != null && !changes.isEmpty()) {
                    Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
                    if (value != null && value.containsKey("messages")) {
                        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                        if (messages != null && !messages.isEmpty()) {
                            Map<String, Object> message = messages.get(0);

                            // 1. Check if the incoming message type is a location
                            if ("location".equals(message.get("type"))) {
                                // 2. Extract the nested location map object
                                Map<String, Object> locationBlock = (Map<String, Object>) message.get("location");

                                if (locationBlock != null) {
                                    // Meta sends coordinates as Numbers (Double)
                                    Double latitude = (Double) locationBlock.get("latitude");
                                    Double longitude = (Double) locationBlock.get("longitude");
                                    String name = (String) locationBlock.get("name"); // Optional: e.g. "Pretoria Central"
                                    String address = (String) locationBlock.get("address"); // Optional

                                    // 3. Wrap them up nicely to return
                                    Map<String, Object> coordinates = new HashMap<>();
                                    coordinates.put("latitude", latitude);
                                    coordinates.put("longitude", longitude);
                                    coordinates.put("name", name);
                                    coordinates.put("address", address);

                                    return coordinates;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting location: " + e.getMessage());
        }
        return null;
    }
}
