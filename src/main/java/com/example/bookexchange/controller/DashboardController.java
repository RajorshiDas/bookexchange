package com.example.bookexchange.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("role", "ADMIN");
        return "dashboard-admin";
    }

    @GetMapping("/buyer")
    public String buyerDashboard(Model model) {
        model.addAttribute("role", "BUYER");
        return "dashboard-buyer";
    }

    @GetMapping("/seller")
    public String sellerDashboard(Model model) {
        model.addAttribute("role", "SELLER");
        return "dashboard-seller";
    }
}
