package com.kasigrill.ordering_system.menuitem;

import org.springframework.data.jpa.repository.JpaRepository;


public interface MenuRepository extends JpaRepository<MenuItem, Long> {

    MenuItem findBySku(String skus);

}
