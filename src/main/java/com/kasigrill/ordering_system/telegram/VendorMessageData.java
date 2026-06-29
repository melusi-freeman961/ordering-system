package com.kasigrill.ordering_system.telegram;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Embeddable
public class VendorMessageData {

    String messageId;

    public VendorMessageData() {
    }

    public VendorMessageData(String messageId) {
        this.messageId = messageId;
    }
}
