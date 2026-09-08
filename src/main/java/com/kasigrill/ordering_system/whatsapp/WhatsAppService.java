package com.kasigrill.ordering_system.whatsapp;

import com.kasigrill.ordering_system.config.CustomerNotificationService;
import com.kasigrill.ordering_system.customer.CustomerMessage;
import com.kasigrill.ordering_system.customer.CustomerMessageImp;
import com.kasigrill.ordering_system.order.OrderDto;
import com.kasigrill.ordering_system.resturant.StoreService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WhatsAppService implements CustomerNotificationService {


    private final StoreService storeService;
    private final WhatsappPayloadParser payloadParser;
    private final WhatsAppGateway whatsAppGateway;
    private final Map<String, String> activeSessionsLastStatusMessageId = new ConcurrentHashMap<>();


    public WhatsAppService(@Lazy StoreService storeService
            , WhatsAppGateway whatsAppGateway
            , WhatsappPayloadParser payloadParser
    ) {
        this.storeService = storeService;
        this.payloadParser = payloadParser;
        this.whatsAppGateway = whatsAppGateway;
    }

    @SuppressWarnings("unchecked")
    public void deserializeAndProcessWhatsappMessage(Map<String, Object> payload) {
        String updatedStatus = payloadParser.isStatusUpdatePayload(payload);

        if (updatedStatus != null) {

            if (updatedStatus.isBlank()) return;

            String recipient_number = payloadParser.getNumberFromStatus(payload);
            if (recipient_number == null) return;
            if (recipient_number.isBlank()) return;

            activeSessionsLastStatusMessageId.put(recipient_number, updatedStatus);
            return;
        }

        Map<String, Object> message = payloadParser.getMessage(payload);
        System.out.println("RAW MESSAGE MAP: " + message);

        String messageType = (String) message.get("type");
        System.out.println("EXTRACTED TYPE: " + messageType);
        String senderNumber = (String) message.get("from");

        if (messageType.isBlank()) return;

        CustomerMessageImp csMessage = new CustomerMessageImp();
        csMessage.customerIdentifierId = senderNumber;

        System.out.println(messageType);
        System.out.println(senderNumber);

        switch (messageType) {
            case "text" -> {


                Map<String, Object> txtBody = (Map<String, Object>) message.get("text");
                String txtMessage = ((String) txtBody.get("body")).trim();

                if (txtMessage.equalsIgnoreCase("GoBite")) {
                    csMessage.message = "GoBite";

                    storeService.publishCustomerMessage(csMessage);
                } else if (txtMessage.equalsIgnoreCase("Menu")) {

                    storeService.menuRequestedAgain(csMessage);

                } else if (txtMessage.equalsIgnoreCase("1")) {

                    activeSessionsLastStatusMessageId.put(senderNumber, "text_number");
                    csMessage.message = senderNumber;
                    storeService.publishCustomerMessage(csMessage);
                } else if (txtMessage.equalsIgnoreCase("2")) {
                    activeSessionsLastStatusMessageId.put(senderNumber, "text_number2");
                    whatsAppGateway.requestCustomerNumber(senderNumber);
                } else {

                    String textId = activeSessionsLastStatusMessageId.get(senderNumber);
                    if (textId == null || textId.isBlank()) return;


                    if (textId.equalsIgnoreCase("text_name")) {
                        csMessage.message = txtMessage;
                        storeService.publishCustomerMessage(csMessage);
                    } else if (textId.equalsIgnoreCase("text_number1")) {

                        if (!txtMessage.equalsIgnoreCase("1") && !txtMessage.equalsIgnoreCase("2")) {
                            whatsAppGateway.publishInvalidOptionsMessage(senderNumber);
                        } else {
                            String num = txtMessage.trim().replaceAll("\\s+", "");
                            String preferredNumber = isNumberValid(senderNumber, num);

                            if (!preferredNumber.isBlank()) {
                                csMessage.message = preferredNumber;
                                storeService.publishCustomerMessage(csMessage);
                            }
                        }
                    } else if (textId.equalsIgnoreCase("text_number2")) {

                        String num = txtMessage.trim().replaceAll("\\s+", "");
                        String preferredNumber = isNumberValid(senderNumber, num);

                        if (!preferredNumber.isBlank()) {
                            csMessage.message = preferredNumber;
                            storeService.publishCustomerMessage(csMessage);
                        }

                    } else {
                        if (textId.equalsIgnoreCase("interactive_main_menu")) {
                            whatsAppGateway.publishInvalidMainMenuOption(senderNumber);
                        } else if (textId.equalsIgnoreCase("catalog")) {
                            whatsAppGateway.publishInvalidOnViewCatalog(senderNumber);
                        }
                        else if (textId.equalsIgnoreCase("text_location")) {
                            whatsAppGateway.publishInvalidLocation(senderNumber);
                        }
                    }
                }
            }
            case "location" -> {

                Map<String, Object> locationData = (Map<String, Object>) message.get("location");

                Double latitude = (Double) locationData.get("latitude");
                Double longitude = (Double) locationData.get("longitude");

                // 3. Generate the driver maps link and reply
                String driverMapsLink = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;

                whatsAppGateway.publishLocationReceivedConfirmation(senderNumber);
                OrderDto orderDetails = storeService.getOrderDetails(senderNumber);

                if (orderDetails != null) {
                    whatsAppGateway.sendOrderConfirmation(senderNumber, orderDetails.orderNumber());
                }

                csMessage.message = locationData;
                storeService.publishCustomerMessage(csMessage);


            }
            case "interactive" -> {

                String id = retrieveBtnId(message);

                if (id.equalsIgnoreCase("btn_terminate")) {
                    boolean terminated = storeService.terminateProcess(senderNumber);

                    if (terminated) {
                        activeSessionsLastStatusMessageId.remove(senderNumber);
                        whatsAppGateway.confirmTermination(senderNumber);
                    }
                } else if (id.equalsIgnoreCase("btn_back_menu")) {
                    storeService.backToManu(senderNumber);
                } else {
                    csMessage.message = message;
                    storeService.publishCustomerMessage(csMessage);
                }

            }
            case "order" -> {

                Map<String, Object> orderObj = (Map<String, Object>) message.get("order");

                if (orderObj != null && orderObj.containsKey("product_items")) {
                    // Extract the list of items from the shopping cart
                    List<Map<String, Object>> productItems = (List<Map<String, Object>>) orderObj.get("product_items");

                    // Loop through the cart to extract the SKUs and quantities
                    for (Map<String, Object> item : productItems) {

                        String sku = (String) item.get("product_retailer_id");
                        String quantity = String.valueOf(item.get("quantity"));
                        storeService.addOderItem(senderNumber, sku, Integer.parseInt(quantity));
                    }
                    storeService.publishOrder(senderNumber);
                }
            }
        }

    }

    @Override
    public boolean publishTerminationConfirmation(String customerNumber) {
        return whatsAppGateway.publishTermination(customerNumber);
    }

    @Override
    public void publishStatusUpdateToUser(String customerNumber, String status) {
        whatsAppGateway.publishStatusUpdate(customerNumber,status);
    }


    private String isNumberValid(String customerNumber, String preferredNumber) {
        if (preferredNumber.matches("^(\\+27|27|0)[6-8][0-9]{8}$")) {

            String standardizedNumber = preferredNumber;
            if (standardizedNumber.startsWith("0")) {
                standardizedNumber = "27" + standardizedNumber.substring(1);
            } else if (standardizedNumber.startsWith("+27")) {
                standardizedNumber = standardizedNumber.substring(1);
            }


            whatsAppGateway.publishNumberValidConfirmation(customerNumber, standardizedNumber);
            return standardizedNumber;

        } else {
            // Validation failed
            whatsAppGateway.publishNumberValidationFailure(customerNumber);
            return "";
        }
    }


    @SuppressWarnings("unchecked")
    private String retrieveBtnId(Map<String, Object> message) {
        // Now we know it's safe to look for the "interactive" object
        Map<String, Object> interactiveObj = (Map<String, Object>) message.get("interactive");

        // Check if it's specifically a button reply
        if ("button_reply".equals(interactiveObj.get("type"))) {
            Map<String, Object> buttonReply = (Map<String, Object>) interactiveObj.get("button_reply");
            return (String) buttonReply.get("id");

        }
        return "";
    }

    @Override
    public void publishStoreClosedStatus(String customerNumber) {
        whatsAppGateway.storeClosedMessage(customerNumber);
    }


    @Override
    public boolean sendMainMenu(String customerNumber) {
        Map<String, Object> payload = whatsAppGateway.createGreetingMenu(customerNumber);
        return whatsAppGateway.sendMessage(payload);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMainManuInput(CustomerMessage scMessage) {

        CustomerMessageImp messageImp = (CustomerMessageImp) scMessage;

        Map<String, Object> message = (Map<String, Object>) messageImp.message;

        String id = retrieveBtnId(message);

        return switch (id) {
            case "btn_view_menu" -> {
                Map<String, Object> payload = whatsAppGateway.createFullCatalog(messageImp.getCustomerIdentifier());
                yield whatsAppGateway.publishCatalog(payload);
            }
            case "btn_my_orders" -> viewOrders(messageImp.getCustomerIdentifier());
            case "btn_help" -> publishHelpLine(messageImp.getCustomerIdentifier());
            default -> false;
        };

    }

    @Override
    public boolean sendHelp(String helpNumber, String customerIdentifier) {
        return whatsAppGateway.publishHelpLine(helpNumber, customerIdentifier);
    }

    @Override
    public boolean requestMobileNumber(CustomerMessage message) {
        return whatsAppGateway.publishNumberPermission(message);
    }

    @Override
    public boolean requestLocation(String customerIdentifier) {
        return whatsAppGateway.requestLocation(customerIdentifier);
    }

    @Override
    public boolean requestCustomerName(String customerIdentifier) {
        return whatsAppGateway.requestCustomerName(customerIdentifier);
    }

    private boolean publishHelpLine(String customerIdentifier) {
        return storeService.publishHelp(customerIdentifier);
    }

    private boolean viewOrders(String customerIdentifier) {
        List<OrderDto> orders = storeService.publishCustomerOrders(customerIdentifier);

        if (orders == null || orders.isEmpty()) {
            return whatsAppGateway.publishNoOrderNotification(customerIdentifier);
        }
        return whatsAppGateway.publishOrders(orders, customerIdentifier);
    }


}
