package com.example.bookexchange.config;

import com.example.bookexchange.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    // Constructor injection
    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            // --- URL Authorization ---
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers(
                    "/", "/auth/login", "/auth/register",
                    "/css/**", "/js/**", "/images/**"
                ).permitAll()

                // Admin only
                .requestMatchers("/dashboard/admin", "/dashboard/admin/**", "/admin/**").hasRole("ADMIN")

                // Seller only
                .requestMatchers("/dashboard/seller", "/dashboard/seller/**", "/seller/**").hasRole("SELLER")

                // Buyer only
                .requestMatchers("/dashboard/buyer", "/dashboard/buyer/**", "/buyer/**").hasRole("BUYER")

                // Everything else requires login
                .anyRequest().authenticated()
            )

            // --- Form Login ---
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                // Role-based redirect on success
                .successHandler((request, response, authentication) -> {
                    String role = authentication.getAuthorities()
                            .iterator().next().getAuthority();
                    switch (role) {
                        case "ROLE_ADMIN"  -> response.sendRedirect("/dashboard/admin");
                        case "ROLE_SELLER" -> response.sendRedirect("/dashboard/seller");
                        default            -> response.sendRedirect("/dashboard/buyer");
                    }
                })
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )

            // --- Logout ---
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // --- Access Denied: redirect to own dashboard instead of 403 ---
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    // --- Custom Access Denied Handler ---
    // When a logged-in user tries to visit a page they are not allowed to,
    // redirect them silently to their own dashboard instead of showing 403.
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String role = request.getUserPrincipal() != null
                    ? request.isUserInRole("ADMIN")  ? "ROLE_ADMIN"
                    : request.isUserInRole("SELLER") ? "ROLE_SELLER"
                    : "ROLE_BUYER"
                    : null;

            if ("ROLE_ADMIN".equals(role)) {
                response.sendRedirect("/dashboard/admin");
            } else if ("ROLE_SELLER".equals(role)) {
                response.sendRedirect("/dashboard/seller");
            } else {
                response.sendRedirect("/dashboard/buyer");
            }
        };
    }

    // --- Authentication Provider ---
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
