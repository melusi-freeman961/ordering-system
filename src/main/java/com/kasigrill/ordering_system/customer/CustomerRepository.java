package com.kasigrill.ordering_system.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Customer findByCustomerIdentifierId(String id);

}
