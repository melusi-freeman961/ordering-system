package com.kasigrill.ordering_system.telegram;

import com.kasigrill.ordering_system.customer.IncomingVendorMessage;

public record TelegramMessageDto(String status, String messageId,String callbackId) implements IncomingVendorMessage {
    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public String getStatus() {
        return status;
    }

}
