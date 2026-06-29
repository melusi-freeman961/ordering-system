package com.kasigrill.ordering_system.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<CustomerSession,Long>{
}
