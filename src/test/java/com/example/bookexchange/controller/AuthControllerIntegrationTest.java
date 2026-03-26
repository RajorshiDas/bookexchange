package com.example.bookexchange.controller;

import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.repository.BookListingRepository;
import com.example.bookexchange.repository.BookRepository;
import com.example.bookexchange.repository.ExchangeRequestRepository;
import com.example.bookexchange.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private BookListingRepository bookListingRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        exchangeRequestRepository.deleteAll();
        bookListingRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getLogin_returns_login_view() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void getRegister_returns_register_view() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_with_valid_data_redirects_to_login() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", "buyer1")
                        .param("email", "buyer1@example.com")
                        .param("password", "Secret123")
                        .param("confirmPassword", "Secret123")
                        .param("firstName", "Buyer")
                        .param("lastName", "One")
                        .param("role", UserRole.BUYER.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void postRegister_with_invalid_data_returns_register_view() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", " ")
                        .param("email", "buyer2@example.com")
                        .param("password", "Secret123")
                        .param("confirmPassword", "Secret123")
                        .param("firstName", "Buyer")
                        .param("lastName", "Two")
                        .param("role", UserRole.BUYER.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }
}
