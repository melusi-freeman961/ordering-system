package com.kasigrill.ordering_system.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    CustomerOrder findByVendorMessageData_MessageId(String messageId);

}
