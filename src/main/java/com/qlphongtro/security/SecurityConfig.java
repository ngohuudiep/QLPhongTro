package com.qlphongtro.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * Cấu hình bảo mật Spring Security cho toàn bộ ứng dụng.
 *
 * Phân quyền URL:
 *  - PUBLIC  : /rooms/list, /rooms/{id}, /login, /css/**, /js/**, /images/**
 *  - USER    : Chỉ xem danh sách phòng, không có quyền chỉnh sửa
 *  - STAFF   : /staff/** - thêm/sửa thông tin cơ bản, không xóa
 *  - ADMIN   : /admin/** - toàn quyền: thêm/sửa/xóa phòng, đổi giá
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Cho phép dùng @PreAuthorize, @Secured trên method
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final String rememberMeKey;

    @Autowired
    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            @Value("${REMEMBER_ME_KEY}") String rememberMeKey) {
        this.userDetailsService = userDetailsService;
        if (rememberMeKey.isBlank()) {
            throw new IllegalArgumentException("REMEMBER_ME_KEY must not be blank.");
        }
        this.rememberMeKey = rememberMeKey;
    }

    // ==============================================================
    // 1. BCrypt Password Encoder - dùng để mã hóa và xác thực mật khẩu
    // ==============================================================
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt với strength 12 - cân bằng giữa bảo mật và tốc độ
        return new BCryptPasswordEncoder(12);
    }

    // ==============================================================
    // 2. Authentication Provider - liên kết UserDetailsService với Encoder
    // ==============================================================
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Ẩn thông báo "User not found" để tránh lộ thông tin hệ thống
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    // ==============================================================
    // 3. Authentication Manager
    // ==============================================================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // ==============================================================
    // 4. Security Filter Chain - Quy tắc phân quyền URL chính
    // ==============================================================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(Customizer.withDefaults())
            .headers(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession()))
            // ---- Cấu hình phân quyền URL ----
            .authorizeHttpRequests(auth -> auth

                // Tài nguyên tĩnh: CSS, JS, ảnh - không cần đăng nhập
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                // Trang đăng nhập / đăng ký / quên mật khẩu - không cần đăng nhập
                .requestMatchers("/login", "/register", "/forgot-password", "/forgot-password/**", "/error").permitAll()

                // Xem danh sách phòng và chi tiết phòng - KHÔNG cần đăng nhập
                // Cả khách vãng lai và USER đều xem được
                .requestMatchers(HttpMethod.GET, "/", "/rooms/list").permitAll()
                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "\\A/rooms/[0-9]+\\z")).permitAll()

                // Khu vực ADMIN - chỉ role ADMIN
                // Bao gồm: thêm/sửa/xóa phòng, đổi giá, quản lý user
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")

                // Khu vực STAFF - role STAFF và ADMIN đều vào được
                // Bao gồm: cập nhật trạng thái phòng, xem báo cáo
                .requestMatchers("/staff", "/staff/**").hasAnyRole("STAFF", "ADMIN")

                .requestMatchers("/tenants", "/tenants/**").hasAnyRole("ADMIN", "STAFF")

                // Known authenticated application routes retain their current access behavior.
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

            // ---- Cấu hình trang đăng nhập ----
            .formLogin(form -> form
                .loginPage("/login")                    // URL hiển thị trang đăng nhập
                .loginProcessingUrl("/login")           // URL POST xử lý submit form login
                .usernameParameter("username")          // Tên field input username trong form
                .passwordParameter("password")          // Tên field input password trong form
                .defaultSuccessUrl("/", true)           // Sau đăng nhập thành công -> trang chủ
                .failureUrl("/login?error=true")        // Sai mật khẩu -> quay lại với thông báo lỗi
                .permitAll()
            )

            // ---- Cấu hình đăng xuất ----
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true") // Sau đăng xuất -> trang login
                .invalidateHttpSession(true)            // Xóa session hiện tại
                .deleteCookies("JSESSIONID")            // Xóa cookie session
                .permitAll()
            )

            // ---- Cấu hình xử lý lỗi quyền truy cập ----
            .exceptionHandling(ex -> ex
                // Nếu đã đăng nhập nhưng không có quyền -> chuyển đến trang /access-denied
                .accessDeniedPage("/access-denied")
            )

            // ---- Cấu hình Remember Me (Ghi nhớ đăng nhập) ----
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .tokenValiditySeconds(7 * 24 * 3600)    // Token có hiệu lực 7 ngày
                .rememberMeParameter("remember-me")     // Tên checkbox trong form login
            )

            // ---- Liên kết AuthenticationProvider ----
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
