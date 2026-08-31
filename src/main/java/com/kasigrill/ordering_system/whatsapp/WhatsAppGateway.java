package com.kasigrill.ordering_system.whatsapp;

import com.kasigrill.ordering_system.customer.CustomerMessage;
import com.kasigrill.ordering_system.menuitem.MenuItemDto;
import com.kasigrill.ordering_system.order.OrderDto;
import com.kasigrill.ordering_system.order.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WhatsAppGateway {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${whatsapp.mobile.id}")
    private String phoneNumberId;
    @Value("${whatsapp.access.token}")
    private String accessToken;


    public boolean sendMessage(Map<String, Object> payload) {

        String url = "https://graph.facebook.com/v25.0/" + phoneNumberId + "/messages";
        var requestEntity = setUpHttp(payload);


        // Print the request before sending
        System.out.println("URL: " + url);
        System.out.println("Payload: " + payload);

        try {

            ResponseEntity<Map> mapResponseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);
            System.out.println(mapResponseEntity.getStatusCode());
            return mapResponseEntity.getStatusCode().is2xxSuccessful();


        } catch (Exception e) {
            System.err.println("❌ [WhatsApp API] Error transmitting payload to Meta: " + e.getMessage());
        }
        return false;
    }

    private HttpEntity<Map<String, Object>> setUpHttp(Map<String, Object> payload) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        return new HttpEntity<>(payload, headers);

    }

    public void sendOrderConfirmation(String customerNumber,int orderNumber) {

        sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "Your order number is #" + orderNumber+", keep an eye on the driver.")
        ));

    }

    public boolean requestAnotherOrder(String customerNumber) {

        return sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "Press 1 if you want to order something else.")
        ));

    }

    public boolean requestCustomerName(String customerNumber) {

        Map<String, Object> closedMessage = Map.of(
                "messaging_product", "whatsapp",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "\uD83C\uDF1F We have your order, what should we call you?")
        );

        return sendMessage(closedMessage);
    }

    public Map<String, Object> createGreetingMenu(String recipientNumber) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientNumber);
        payload.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");

        Map<String, String> body = new HashMap<>();
        body.put("text", "Hi, Welcome, Wamukelekile, Welkom to GoBites!\nWhat would you like to do today?");
        interactive.put("body", body);

        Map<String, String> footer = new HashMap<>();
        footer.put("text", " You can type Menu at any time to return to this screen.");
        interactive.put("footer", footer);

        Map<String, Object> action = new HashMap<>();
        List<Map<String, Object>> buttons = new ArrayList<>();

        buttons.add(createReplyButton("btn_view_menu", "🍽️ View Menu"));
        buttons.add(createReplyButton("btn_my_orders", "📋 My Orders"));
        buttons.add(createReplyButton("btn_help", "ℹ️ Help"));

        action.put("buttons", buttons);
        interactive.put("action", action);
        payload.put("interactive", interactive);

        return payload;
    }

    public Map<String, Object> sendFullCatalog(String recipientNumber) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientNumber);
        payload.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        // Tell WhatsApp to open the full catalog
        interactive.put("type", "catalog_message");

        // The message bubble text
        Map<String, String> body = new HashMap<>();
        body.put("text", "🍔 Welcome to eas! Tap below to view our full menu.");
        interactive.put("body", body);

        // Meta automatically pulls the catalog linked to your WhatsApp number
        Map<String, Object> action = new HashMap<>();
        action.put("name", "catalog_message");

        // Optional: Set a specific item's image to show on the message bubble
//        Map<String, Object> parameters = new HashMap<>();
//        parameters.put("thumbnail_product_retailer_id", "v7933et4vv");
//        action.put("parameters", parameters);

        interactive.put("action", action);
        payload.put("interactive", interactive);

        return payload;
    }


    private Map<String, Object> createReplyButton(String id, String title) {
        Map<String, Object> button = new HashMap<>();
        button.put("type", "reply");

        Map<String, String> reply = new HashMap<>();
        reply.put("id", id);
        reply.put("title", title);

        button.put("reply", reply);
        return button;
    }

    public boolean requestLocation(String customerNumber) {

        Map<String, Object> locationRequestMessage = Map.of(
                "messaging_product", "whatsapp",
                "to", customerNumber,
                "type", "interactive",
                "interactive", Map.of(
                        "type", "location_request_message",
                        "body", Map.of("text", "Please share your location with us to make the delivery easy."),
                        "action", Map.of("name", "send_location")
                )
        );

        return sendMessage(locationRequestMessage);
    }

    public boolean storeClosedMessage(String customerNumber) {
        Map<String, Object> closedMessage = Map.of(
                "messaging_product", "whatsapp",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "👋 Thanks for reaching out to Kasi Grill! We are currently CLOSED. Our operating hours are daily from 10:00 to 21:00. See you tomorrow! 🔥")
        );
        return sendMessage(closedMessage);
    }

    public boolean menuResponseMessage(String messageText, String customerNumber, List<MenuItemDto> menu) {

        if (menu.isEmpty()) {
//            menuEmptyNotification(customerNumber);

            sendMessage(createGreetingMenu(customerNumber));
        }

        List<Map<String, Object>> rows = menu.stream()
                .map(item -> Map.<String, Object>of(
                        "id", "item_" + item.id(),
                        "title", item.name(),
                        "description", "Price: R" + item.price()
                ))
                .toList();

        List<Map<String, Object>> formattedSections = List.of(
                Map.of(
                        "title", "Kasi Grill Delights",
                        "rows", rows
                )
        );
        if (messageText != null && (messageText.equalsIgnoreCase("Show me the menu") || messageText.equalsIgnoreCase("1"))) {
            return sendMessage(Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", customerNumber,
                    "type", "interactive",
                    "interactive", Map.of(
                            "type", "list",
                            "header", Map.of("type", "text", "text", "Kasi Grill Menu"),
                            "body", Map.of("text", "Hungry? Choose an option below:"),
                            "action", Map.of(
                                    "button", "View Menu",
                                    "sections", formattedSections
                            )
                    )
            ));
        }

        return false;
    }

    public boolean getCustomerNumber(String customerNumber) {
        return sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "Could you please share your mobile number so we can give you a quick call or text the moment it's ready for collection? Thanks!")
        ));
    }

    public void menuEmptyNotification(String customerNumber) {
        sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "👋 Thanks for contacting Kasi Grill! We are currently updating our menu items. Please check back in a few minutes! 🔥")
        ));
    }

    public boolean publishOrders(List<OrderDto> orders, String customerNumber) {


        boolean sent = false;

        for (OrderDto order : orders) {

            String orderNum = String.valueOf(order.orderNumber());
            String status = order.status();
            String date = String.valueOf(order.dateTime());

            String message = "Order " + orderNum + "was placed on " + date + " and its current status is: " + status + ".";
            sent = sendMessage(Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", customerNumber,
                    "type", "text",
                    "text", Map.of("body", message)
            ));
        }
        return sent;
    }

    public boolean publishHelpLine(String helpLine, String customerNumber) {


        return sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "Got questions? Give us a call at " + helpLine + " — we're happy to help!")
        ));
    }

    public boolean publishNoOrderNotification(String customerNumber) {
        return sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "Looks like you haven't placed any orders yet!")
        ));
    }

    public boolean publishNumberPermission(CustomerMessage message) {

        String name = (String) message.getCustomerMessage();

        return sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", message.getCustomerIdentifier(),
                "type", "text",
                "text", Map.of("body", "Ok " + name + " can we use this number to send you updates?\n\n1️⃣ Yes\n2️⃣ No")
        ));


    }

    public void requestCustomerNumber(String customerNumber) {

        sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "No problem! 📱 Please reply with the phone number you would prefer us to use for updates (e.g., 0712345678).")
        ));

    }

    public void publishNumberValidConfirmation(String customerNumber, String preferredNumber) {
        sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "Got it! We've updated your preferred contact number to " + preferredNumber + " ✅")
        ));
    }

    public void publishNumberValidationFailure(String customerNumber) {
        sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "That doesn't look like a valid South African cell number. Please try again (e.g., 0712345678).")
        ));
    }

    public void publishLocationReceivedConfirmation(String customerNumber) {
        sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "Good news we received your location.")
        ));
    }

    public boolean publishTermination(String customerNumber) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", customerNumber); // Replace with dynamic number
        payload.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");

        interactive.put("body", Map.of("text", "Would you like to cancel or go back?"));

        interactive.put("action", Map.of(
                "buttons", new Object[]{
                        Map.of(
                                "type", "reply",
                                "reply", Map.of("id", "btn_terminate", "title", "Terminate")
                        ),
                        Map.of(
                                "type", "reply",
                                "reply", Map.of("id", "btn_back_menu", "title", "Back to Menu")
                        )
                }
        ));

        payload.put("interactive", interactive);
        return sendMessage(payload);
    }

    public void confirmTermination(String customerNumber) {

        sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", " Process terminated. Let us know when you are ready to order again by typing Menu! \uD83D\uDC4B")
        ));
    }
}
