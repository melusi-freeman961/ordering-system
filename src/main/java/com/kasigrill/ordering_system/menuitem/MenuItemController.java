package com.kasigrill.ordering_system.menuitem;

import com.kasigrill.ordering_system.resturant.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/api/v2/menu-items")
@CrossOrigin(origins = "*")
public class MenuItemController {

    @Autowired
    private MenuRepository repository;
    @Autowired
    StoreService service;


    @PostMapping
    public ResponseEntity<?> create(@RequestBody MenuItemRequest request) {
       service.addMenuItem(request);

       return ResponseEntity.ok().build();
    }
    // Your JS also fires a GET request when the page loads, so you need a basic GET endpoint to prevent a 404 error
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }
}
