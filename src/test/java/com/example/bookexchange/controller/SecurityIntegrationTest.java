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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class SecurityIntegrationTest {

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
    void unauthenticated_admin_is_redirected_to_login() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/auth/login*"));
    }

    @Test
    void unauthenticated_seller_is_redirected_to_login() throws Exception {
        mockMvc.perform(get("/dashboard/seller"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/auth/login*"));
    }

    @Test
    void unauthenticated_buyer_is_redirected_to_login() throws Exception {
        mockMvc.perform(get("/dashboard/buyer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/auth/login*"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"BUYER"})
    void buyer_accessing_admin_is_forbidden_or_redirected() throws Exception {
        int status = mockMvc.perform(get("/dashboard/admin"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertTrue(status == 403 || (status >= 300 && status < 400));
    }

    @Test
    @WithMockUser(username = "user", roles = {"SELLER"})
    void seller_accessing_buyer_is_forbidden_or_redirected() throws Exception {
        int status = mockMvc.perform(get("/dashboard/buyer"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertTrue(status == 403 || (status >= 300 && status < 400));
    }

    @Test
    @WithMockUser(username = "user", roles = {"ADMIN"})
    void admin_accessing_admin_returns_ok() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"SELLER"})
    void seller_accessing_seller_returns_ok() throws Exception {
        mockMvc.perform(get("/dashboard/seller"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"BUYER"})
    void buyer_accessing_buyer_returns_ok() throws Exception {
        mockMvc.perform(get("/dashboard/buyer"))
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
