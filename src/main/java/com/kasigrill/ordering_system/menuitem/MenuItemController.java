package com.kasigrill.ordering_system.menuitem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/vendor/dashboard")
public class MenuItemController {

    @Autowired
    private MenuRepository menuItemRepository; // Ensure your JpaRepository interface matches MenuItem

    @GetMapping
    public String showDashboard(Model model) {
        model.addAttribute("products", menuItemRepository.findAll());
        model.addAttribute("productForm", new MenuItem());
        return "vendor-dashboard";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("productForm") MenuItem menuItem) {
        // Explicitly handle default flags for KasiGrill menu rules
        if (menuItem.getId() == null) {
            menuItem.setAvailable(true);
            menuItem.setStatus("ACTIVE");
        } else {
            // Keep existing statuses intact during edits
            Optional<MenuItem> existing = menuItemRepository.findById(menuItem.getId());
            existing.ifPresent(item -> {
                menuItem.setAvailable(item.isAvailable());
                menuItem.setStatus(item.getStatus());
            });
        }
        menuItemRepository.save(menuItem);
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id) {
        Optional<MenuItem> itemOpt = menuItemRepository.findById(id);
        if (itemOpt.isPresent()) {
            MenuItem item = itemOpt.get();
            // Safety check: If someone has ordered it, don't break the database; just archive it
            if (item.getOrderItems() != null && !item.getOrderItems().isEmpty()) {
                item.setAvailable(false);
                item.setStatus("ARCHIVED");
                menuItemRepository.save(item);
            } else {
                // If it has never been ordered, remove it entirely
                menuItemRepository.deleteById(id);
            }
        }
        return "redirect:/vendor/dashboard";
    }
}
