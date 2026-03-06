package com.example.bookexchange.controller;

import com.example.bookexchange.entity.User;
import com.example.bookexchange.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private AuthService authService;

    @GetMapping("/admin")
    public String adminDashboard(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "dashboard-admin";
    }

    @GetMapping("/buyer")
    public String buyerDashboard(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "dashboard-buyer";
    }

    @GetMapping("/seller")
    public String sellerDashboard(Model model, Principal principal) {
        addUserToModel(model, principal);
        return "dashboard-seller";
    }

    // Helper: load logged-in user and add to model
    private void addUserToModel(Model model, Principal principal) {
        if (principal != null) {
            Optional<User> user = authService.findByUsername(principal.getName());
            user.ifPresent(u -> {
                model.addAttribute("username", u.getUsername());
                model.addAttribute("firstName", u.getFirstName());
                model.addAttribute("lastName", u.getLastName());
                model.addAttribute("email", u.getEmail());
                model.addAttribute("role", u.getRole().name());
            });
        }
    }
}
