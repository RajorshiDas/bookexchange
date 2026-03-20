package com.example.bookexchange.controller;

import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.service.AuthService;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.service.AdminBookService;
import com.example.bookexchange.entity.ListingStatus;
import com.example.bookexchange.entity.ExchangeRequest;
import com.example.bookexchange.service.ExchangeRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AdminBookService adminBookService;
    private final ExchangeRequestService exchangeRequestService;

    public AdminController(UserRepository userRepository, AuthService authService, AdminBookService adminBookService, ExchangeRequestService exchangeRequestService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.adminBookService = adminBookService;
        this.exchangeRequestService = exchangeRequestService;
    }

    @GetMapping("/users")
    public String listUsers(Model model,
                            org.springframework.security.core.Authentication authentication,
                            @RequestParam(name = "q", required = false) String query,
                            @RequestParam(name = "role", required = false) String role) {
        addUserToModel(model, authentication);

        UserRole roleFilter = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                roleFilter = UserRole.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                roleFilter = null;
            }
        }

        List<User> users;
        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (roleFilter != null && hasQuery) {
            users = userRepository.findByRoleAndUsernameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
                    roleFilter, query.trim(), roleFilter, query.trim());
        } else if (roleFilter != null) {
            users = userRepository.findByRole(roleFilter);
        } else if (hasQuery) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    query.trim(), query.trim());
        } else {
            users = userRepository.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("query", query);
        model.addAttribute("selectedRole", roleFilter != null ? roleFilter.name() : "");
        model.addAttribute("roles", UserRole.values());
        return "admin-users";
    }

    @PostMapping("/users/{id}/enabled")
    public String updateUserEnabled(@PathVariable("id") Long id,
                                    @RequestParam("enabled") boolean enabled,
                                    @RequestParam(name = "q", required = false) String query,
                                    @RequestParam(name = "role", required = false) String role) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEnabled(enabled);
            userRepository.save(user);
        });

        StringBuilder redirect = new StringBuilder("redirect:/admin/users");
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasRole = role != null && !role.trim().isEmpty();
        if (hasQuery || hasRole) {
            redirect.append("?");
            if (hasQuery) {
                redirect.append("q=").append(query.trim());
            }
            if (hasRole) {
                if (hasQuery) {
                    redirect.append("&");
                }
                redirect.append("role=").append(role.trim());
            }
        }
        return redirect.toString();
    }

    @GetMapping("/books")
    public String listBooks(Model model, org.springframework.security.core.Authentication authentication) {
        addUserToModel(model, authentication);
        List<BookListing> listings = adminBookService.getAllListings();
        model.addAttribute("listings", listings);
        return "admin-books";
    }

    @PostMapping("/books/{id}/status")
    public String updateListingStatus(@PathVariable("id") Long id,
                                      @RequestParam("status") String status) {
        ListingStatus newStatus = null;
        if (ListingStatus.RESERVED.name().equalsIgnoreCase(status)) {
            newStatus = ListingStatus.RESERVED;
        } else if (ListingStatus.SOLD.name().equalsIgnoreCase(status)) {
            newStatus = ListingStatus.SOLD;
        } else if (ListingStatus.AVAILABLE.name().equalsIgnoreCase(status)) {
            newStatus = ListingStatus.AVAILABLE;
        }

        if (newStatus != null) {
            adminBookService.updateStatusIfAllowed(id, newStatus);
        }
        return "redirect:/admin/books";
    }

    @GetMapping("/requests")
    public String listRequests(Model model, org.springframework.security.core.Authentication authentication) {
        addUserToModel(model, authentication);
        List<ExchangeRequest> requests = exchangeRequestService.getAllRequests();
        model.addAttribute("requests", requests);
        return "admin-requests";
    }

    private void addUserToModel(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication != null) {
            Optional<User> user = authService.findByUsername(authentication.getName());
            user.ifPresent(u -> {
                model.addAttribute("username", u.getUsername());
                model.addAttribute("firstName", u.getFirstName());
                model.addAttribute("lastName", u.getLastName());
                model.addAttribute("email", u.getEmail());
            });
        }
    }
}
