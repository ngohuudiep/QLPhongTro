package com.qlphongtro.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.http.HttpMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Cấu hình bảo mật Spring Security cho toàn bộ hệ thống QLPhongTro.
 * Hỗ trợ 3 Workspace (Admin, Landlord, Tenant), phân quyền RBAC và Method Security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final String rememberMeKey;

    @Autowired
    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            @Value("${REMEMBER_ME_KEY:${app.security.remember-me-key:test-only-remember-me-key}}") String rememberMeKey) {
        this.userDetailsService = userDetailsService;
        this.rememberMeKey = (rememberMeKey == null || rememberMeKey.isBlank())
                ? "test-only-remember-me-key"
                : rememberMeKey.trim();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            AuthenticationException statusFailure = findAccountStatusFailure(exception);
            String message = statusFailure == null
                    ? "Thông tin tài khoản hoặc mật khẩu không chính xác."
                    : statusFailure.getMessage();
            request.getSession(true).setAttribute("loginErrorMessage", message);
            response.sendRedirect(request.getContextPath() + "/login?error=true");
        };
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            boolean isLandlord = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_LANDLORD")
                            || authority.getAuthority().equals("ROLE_STAFF"));
            boolean isTenant = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_TENANT")
                            || authority.getAuthority().equals("ROLE_USER")
                            || authority.getAuthority().equals("ROLE_NGUOITHUE")
                            || authority.getAuthority().equals("ROLE_NGUOI_THUE"));

            String targetUrl = "/access-denied";
            if (isAdmin) {
                targetUrl = "/admin/dashboard";
            } else if (isLandlord) {
                targetUrl = "/landlord/dashboard";
            } else if (isTenant) {
                targetUrl = "/tenant/dashboard";
            }

            try {
                response.sendRedirect(request.getContextPath() + targetUrl);
            } catch (IOException e) {
                throw new IllegalStateException("Không thể chuyển hướng sau đăng nhập", e);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/room-images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/register", "/forgot-password/**", "/error", "/access-denied").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/rooms/list", "/ranking").permitAll()
                .requestMatchers("/api/ranking/**", "/api/reviews/**").permitAll()
                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "\\A/rooms/[0-9]+\\z")).permitAll()

                // Phân quyền theo vai trò
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/landlord/**").hasAnyRole("LANDLORD", "STAFF")
                .requestMatchers("/staff/**").hasAnyRole("STAFF", "LANDLORD")
                .requestMatchers("/tenant/**").hasAnyRole("TENANT", "USER", "NGUOITHUE", "NGUOI_THUE")
                .requestMatchers("/rooms/booking", "/rooms/booking/**").hasAnyRole("TENANT", "USER")

                // Legacy application routes
                .requestMatchers("/tenants", "/tenants/**").hasAnyRole("ADMIN", "STAFF", "LANDLORD")
                .requestMatchers(
                    "/profile", "/profile/**",
                    "/rooms", "/rooms/**",
                    "/buildings", "/buildings/**",
                    "/contracts", "/contracts/**",
                    "/invoices", "/invoices/**",
                    "/meter-readings", "/meter-readings/**",
                    "/incidents", "/incidents/**",
                    "/assets", "/assets/**").authenticated()

                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(roleBasedAuthenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            )
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .tokenValiditySeconds(7 * 24 * 3600)
                .rememberMeParameter("remember-me")
            )
            .sessionManagement(session -> session
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/login?expired=true")
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    private static AuthenticationException findAccountStatusFailure(AuthenticationException exception) {
        Throwable cause = exception;
        while (cause instanceof AuthenticationException authenticationException) {
            if (authenticationException instanceof DisabledException
                    || authenticationException instanceof LockedException) {
                return authenticationException;
            }
            cause = authenticationException.getCause();
        }
        return null;
    }
}
