package com.kasigrill.ordering_system.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentifierRepository extends JpaRepository<CustomerIdentifier,Long> {
}
