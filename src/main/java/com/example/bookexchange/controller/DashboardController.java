package com.example.bookexchange.controller;

import com.example.bookexchange.entity.User;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.service.AuthService;
import com.example.bookexchange.service.ExchangeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeRequestService exchangeRequestService;

    // Constructor injection
    public DashboardController(AuthService authService) {
        this.authService = authService;
    }

    // ----------------------------------------------------------------
    // GET /dashboard — redirect to the correct dashboard based on role
    // ----------------------------------------------------------------
    @GetMapping
    public String dashboard(Authentication authentication) {
        String role = authentication.getAuthorities()
                .iterator().next().getAuthority();

        return switch (role) {
            case "ROLE_ADMIN"  -> "redirect:/dashboard/admin";
            case "ROLE_SELLER" -> "redirect:/dashboard/seller";
            default            -> "redirect:/dashboard/buyer";
        };
    }

    // ----------------------------------------------------------------
    // GET /dashboard/admin
    // ----------------------------------------------------------------
    @GetMapping("/admin")
    public String adminDashboard(Model model, Authentication authentication) {
        addUserToModel(model, authentication);
        model.addAttribute("role", "ADMIN");
        return "dashboard-admin";
    }

    // ----------------------------------------------------------------
    // GET /dashboard/buyer
    // ----------------------------------------------------------------
    @GetMapping("/buyer")
    public String buyerDashboard(Model model, Authentication authentication) {
        addUserToModel(model, authentication);
        model.addAttribute("role", "BUYER");

        // Add exchange request counts
        if (authentication != null) {
            Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
            if (userOpt.isPresent()) {
                User buyer = userOpt.get();
                long pendingCount = exchangeRequestService.getPendingRequestCountForBuyer(buyer);
                long acceptedCount = exchangeRequestService.getRequestsByBuyer(buyer).stream()
                        .filter(req -> req.getStatus().name().equals("ACCEPTED"))
                        .count();
                model.addAttribute("pendingCount", pendingCount);
                model.addAttribute("acceptedCount", acceptedCount);
            }
        }

        return "dashboard-buyer";
    }

    // ----------------------------------------------------------------
    // GET /dashboard/seller
    // ----------------------------------------------------------------
    @GetMapping("/seller")
    public String sellerDashboard(Model model, Authentication authentication) {
        addUserToModel(model, authentication);
        model.addAttribute("role", "SELLER");
        return "dashboard-seller";
    }

    // ----------------------------------------------------------------
    // Helper: load the logged-in user from DB and add fields to model
    // ----------------------------------------------------------------
    private void addUserToModel(Model model, Authentication authentication) {
        if (authentication != null) {
            Optional<User> user = authService.findByUsername(authentication.getName());
            user.ifPresent(u -> {
                model.addAttribute("username",  u.getUsername());
                model.addAttribute("firstName", u.getFirstName());
                model.addAttribute("lastName",  u.getLastName());
                model.addAttribute("email",     u.getEmail());
            });
        }
    }
}
