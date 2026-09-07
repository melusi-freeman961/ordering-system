package com.kasigrill.ordering_system.customer;

import com.kasigrill.ordering_system.order.CustomerOrder;

public record CustomerCreatedEvent(CustomerOrder order) {


}
