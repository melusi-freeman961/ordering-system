package com.kasigrill.ordering_system.whatsapp;

import com.kasigrill.ordering_system.menuitem.MenuItemDto;
import com.kasigrill.ordering_system.order.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

        try {

            ResponseEntity<Map> mapResponseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);
            System.out.println("🚀 [WhatsApp API] Payload successfully transmitted to Meta.");
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

    public boolean sendOrderStatus(String status, String customerNumber, String orderNumber) {
        String text = "";

        if (status.equalsIgnoreCase(OrderStatus.REJECT.name())) {
            text = "Your order " + orderNumber + " has been rejected!";
        } else if (status.equalsIgnoreCase(OrderStatus.PLACED.name())) {
            text = "Your order " + orderNumber + " has been placed!";
        } else if (status.equalsIgnoreCase(OrderStatus.READY.name())) {
            text = "Your order " + orderNumber + " is ready for collection!";
        } else if (status.equalsIgnoreCase(OrderStatus.PREPARING.name())) {
            text = "Your order " + orderNumber + " is being prepared!";
        }
        return sendMessage(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body", "\uD83E\uDD57 " + text)
        ));

    }

    public boolean sendOrderConfirmation(String customerNumber) {


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
                "text", Map.of("body", "Welcome to Kasi Grill! \uD83C\uDF1F What should we call you?")
        );

        return sendMessage(closedMessage);
    }

    public boolean requestLocation(String customerNumber) {

        Map<String, Object> locationRequestMessage = Map.of(
                "messaging_product", "whatsapp",
                "to", customerNumber,
                "type", "text",
                "text", Map.of("body",
                        "🛵 *Awesome , we've got your order details!*\n\n" +
                                "To ensure our motorbike rider brings your food straight to your doorstep sizzling hot, " +
                                "please send us your *Live Location* or *Current Location*.\n\n" +
                                "📎 *How to do it:*\n" +
                                "1. Tap the *Attach* icon (the paperclip 📎 or plus `+` sign next to your text box).\n" +
                                "2. Select *Location* 📍.\n" +
                                "3. Tap *Send Your Current Location* or *Share Live Location*.")
                , "biz_opaque_callback_data", "QUESTION_ASK_LOCATION"
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
}
