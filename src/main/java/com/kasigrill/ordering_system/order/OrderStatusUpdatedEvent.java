package com.kasigrill.ordering_system.order;

public record OrderStatusUpdatedEvent(CustomerOrder order, String status) {
}
