package com.qlphongtro.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
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
        provider.setHideUserNotFoundExceptions(false);
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
            // ---- Cấu hình phân quyền URL ----
            .authorizeHttpRequests(auth -> auth

                // Tài nguyên tĩnh: CSS, JS, ảnh - không cần đăng nhập
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                // Trang đăng nhập / đăng ký - không cần đăng nhập
                .requestMatchers("/login", "/register", "/error").permitAll()

                // Xem danh sách phòng và chi tiết phòng - KHÔNG cần đăng nhập
                // Cả khách vãng lai và USER đều xem được
                .requestMatchers("/", "/rooms/list", "/rooms/{id}").permitAll()

                // Khu vực ADMIN - chỉ role ADMIN
                // Bao gồm: thêm/sửa/xóa phòng, đổi giá, quản lý user
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Khu vực STAFF - role STAFF và ADMIN đều vào được
                // Bao gồm: cập nhật trạng thái phòng, xem báo cáo
                .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")

                // Tất cả URL còn lại - phải đăng nhập (bất kỳ role nào)
                .anyRequest().authenticated()
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
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // URL kích hoạt đăng xuất
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
                .key("QLPhongTroSecretKey2026")         // Key bí mật để mã hóa token
                .tokenValiditySeconds(7 * 24 * 3600)    // Token có hiệu lực 7 ngày
                .rememberMeParameter("remember-me")     // Tên checkbox trong form login
            )

            // ---- Liên kết AuthenticationProvider ----
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
