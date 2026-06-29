package com.kasigrill.ordering_system.menuitem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByAvailableTrue();
    List<MenuItem> findByStatus(String status);
    void deleteAllByStatus(String status);
}
