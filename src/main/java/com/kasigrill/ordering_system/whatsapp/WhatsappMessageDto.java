package com.kasigrill.ordering_system.whatsapp;

import com.kasigrill.ordering_system.customer.IncomingCustomerMessage;

import java.util.Map;

public record WhatsappMessageDto(String customerNumber, String messageText, Map<String, Object> location,
                                 String itemId) implements IncomingCustomerMessage {
    @Override
    public String getItemId() {
        return itemId;
    }

    @Override
    public String getChannelId() {
        return customerNumber;
    }

    @Override
    public String getMessage() {
        return messageText;
    }

    @Override
    public Map<String, Object> getLocation() {
        return location;
    }

}
