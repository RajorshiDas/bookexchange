package com.example.bookexchange.controller;

import com.example.bookexchange.dto.AdminDashboardMetrics;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.service.AdminService;
import com.example.bookexchange.service.AuthService;
import com.example.bookexchange.service.BookService;
import com.example.bookexchange.service.ExchangeRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(DashboardControllerIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeRequestService exchangeRequestService;

    @Autowired
    private BookService bookService;

    @Autowired
    private AdminService adminService;

    @BeforeEach
    void setupStubs() {
        User user = userRepository.findByUsername("user").orElseGet(() -> {
            User created = new User();
            created.setUsername("user");
            created.setEmail("user@example.com");
            created.setFirstName("Test");
            created.setLastName("User");
            created.setPassword("password");
            created.setRole(UserRole.BUYER);
            created.setEnabled(true);
            return userRepository.save(created);
        });

        when(authService.findByUsername("user")).thenReturn(Optional.of(user));

        when(exchangeRequestService.getPendingRequestCountForBuyer(user)).thenReturn(0L);
        when(exchangeRequestService.getRequestsByBuyer(user)).thenReturn(Collections.emptyList());
        when(exchangeRequestService.getRequestsForSeller(user)).thenReturn(Collections.emptyList());
        when(bookService.getSellerBooks(user)).thenReturn(Collections.emptyList());

        when(adminService.getDashboardMetrics()).thenReturn(new AdminDashboardMetrics(0, 0, 0, 0));
        when(adminService.getRecentUsers()).thenReturn(Collections.emptyList());
        when(adminService.getRecentListings()).thenReturn(Collections.emptyList());
        when(adminService.getRecentRequests()).thenReturn(Collections.emptyList());
    }

    @Test
    void unauthenticated_user_is_redirected_to_login() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"BUYER"})
    void buyer_role_redirects_to_buyer_dashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/buyer"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"SELLER"})
    void seller_role_redirects_to_seller_dashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/seller"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"ADMIN"})
    void admin_role_redirects_to_admin_dashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"BUYER"})
    void buyer_can_access_buyer_dashboard() throws Exception {
        mockMvc.perform(get("/dashboard/buyer"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"SELLER"})
    void seller_can_access_seller_dashboard() throws Exception {
        mockMvc.perform(get("/dashboard/seller"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"ADMIN"})
    void admin_can_access_admin_dashboard() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        @Primary
        ExchangeRequestService exchangeRequestService() {
            return mock(ExchangeRequestService.class);
        }

        @Bean
        @Primary
        BookService bookService() {
            return mock(BookService.class);
        }

        @Bean
        @Primary
        AdminService adminService() {
            return mock(AdminService.class);
        }
    }
}
