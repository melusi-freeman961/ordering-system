package com.kasigrill.ordering_system.whatsapp;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WhatsappPayloadParser {

    public String isStatusUpdatePayload(Map<String, Object> payload) {
        // Dig into the map structure to see if "statuses" is present instead of "messages"
        // Meta uses {"entry": [{"changes": [{"value": {"statuses": [...]}}]}]} for receipts
        try {
            List<?> entry = (List<?>) payload.get("entry");
            Map<?, ?> entryMap = (Map<?, ?>) entry.getFirst();
            List<?> changes = (List<?>) entryMap.get("changes");
            Map<?, ?> changeMap = (Map<?, ?>) changes.getFirst();
            Map<?, ?> value = (Map<?, ?>) changeMap.get("value");

            if (value.containsKey("statuses") && value.get("statuses") != null) {
                System.out.println("====== METRIC STATUS UPDATE DETECTED ======");
                System.out.println(value.get("statuses"));
                System.out.println("===========================================");

                String textId = extractCustomId(payload);

                if (textId != null) {
                    if (textId.equalsIgnoreCase("text_name") || textId.equalsIgnoreCase("text_number")) {
                        return textId;
                    }
                }

                return "";
            }
            return null;// Returns true if it's just a read/delivery receipt
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public String extractCustomId(Map<String, Object> payload) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
        Map<String, Object> firstEntry = entries.getFirst();

        List<Map<String, Object>> changes = (List<Map<String, Object>>) firstEntry.get("changes");
        Map<String, Object> value = (Map<String, Object>) changes.getFirst().get("value");

        List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");
        if (statuses == null || statuses.isEmpty()) return null;

        return (String) statuses.getFirst().get("biz_opaque_callback_data");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMessage(Map<String, Object> payload) {

        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
        Map<String, Object> changes = (Map<String, Object>) ((List<Map<String, Object>>) entries.getFirst().get("changes")).getFirst();
        Map<String, Object> value = (Map<String, Object>) changes.get("value");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

        // If there are no messages (e.g., it's a delivery status update), exit early
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // 2. Get the first message object
        return messages.getFirst();
    }
    @SuppressWarnings("unchecked")
    public String getNumberFromStatus(Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            Map<String, Object> changes = (Map<String, Object>) ((List<Map<String, Object>>) entries.getFirst().get("changes")).getFirst();
            Map<String, Object> value = (Map<String, Object>) changes.get("value");

            // Target the "statuses" array instead of "messages"
            List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");

            if (statuses != null && !statuses.isEmpty()) {
                Map<String, Object> status = statuses.getFirst();

                // Meta stores the number here for sent/delivered/read receipts
                return (String) status.get("recipient_id");
            }
        } catch (Exception e) {
            // Safely exit if this payload doesn't contain a status
        }

        return null;
    }

}
