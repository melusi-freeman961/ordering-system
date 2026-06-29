package com.kasigrill.ordering_system.config;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository  extends JpaRepository<Admin,Long> {
}
