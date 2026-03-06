package com.example.bookexchange.controller;

import com.example.bookexchange.dto.LoginRequest;
import com.example.bookexchange.dto.RegisterRequest;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // Display login page
    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "login";
    }

    // Handle login form submission
    @PostMapping("/login")
    public String handleLogin(@ModelAttribute LoginRequest loginRequest, Model model) {
        Optional<User> user = authService.authenticate(loginRequest);
        if (user.isPresent()) {
            return "redirect:/dashboard/" + user.get().getRole().toString().toLowerCase();
        } else {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("loginRequest", loginRequest);
            return "login";
        }
    }

    // Display registration page
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    // Handle registration form submission
    @PostMapping("/register")
    public String handleRegister(@ModelAttribute RegisterRequest registerRequest,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            authService.register(registerRequest);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registerRequest", registerRequest);
            return "register";
        }
    }
}
