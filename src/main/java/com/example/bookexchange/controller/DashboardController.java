package com.example.bookexchange.controller;

import com.example.bookexchange.dto.AdminDashboardMetrics;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.service.AdminService;
import com.example.bookexchange.service.AuthService;
import com.example.bookexchange.service.BookService;
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

    @Autowired
    private BookService bookService;

    @Autowired
    private AdminService adminService;

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
        AdminDashboardMetrics metrics = adminService.getDashboardMetrics();
        model.addAttribute("totalUsers", metrics.getTotalUsers());
        model.addAttribute("totalListings", metrics.getTotalListings());
        model.addAttribute("totalRequests", metrics.getTotalRequests());
        model.addAttribute("pendingRequests", metrics.getPendingRequests());
        model.addAttribute("recentUsers", adminService.getRecentUsers());
        model.addAttribute("recentListings", adminService.getRecentListings());
        model.addAttribute("recentRequests", adminService.getRecentRequests());
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

        // Add book count for seller
        if (authentication != null) {
            Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
            if (userOpt.isPresent()) {
                User seller = userOpt.get();
                long bookCount = bookService.getSellerBooks(seller).size();
                long pendingRequestCount = bookService.getSellerBooks(seller).stream()
                        .flatMap(listing -> exchangeRequestService.getRequestsForSeller(seller).stream()
                                .filter(req -> req.getListing().getId().equals(listing.getId()) && req.getStatus().name().equals("PENDING")))
                        .count();
                model.addAttribute("bookCount", bookCount);
                model.addAttribute("pendingRequestCount", pendingRequestCount);
            }
        }

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
