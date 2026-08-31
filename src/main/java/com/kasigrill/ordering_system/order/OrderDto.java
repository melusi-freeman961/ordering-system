package com.kasigrill.ordering_system.order;

import java.time.LocalDateTime;

public record OrderDto(int orderNumber, String status, LocalDateTime dateTime) {
}
