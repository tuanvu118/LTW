package _2.LTW.config;

import _2.LTW.exception.CustomAccessDeniedHandler;
import _2.LTW.exception.CustomAuthenticationEntryPoint;
import _2.LTW.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration class cho Spring Security
 * 
 * Luồng xử lý:
 * 1. Chưa đăng nhập hoặc token lỗi → CustomAuthenticationEntryPoint → 401 Unauthorized
 * 2. Token OK nhưng không có quyền → CustomAccessDeniedHandler → 403 Forbidden
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] POST_PUBLIC_ENDPOINT = {
            "/auth/register", "/auth/login"
    };

    private static final String[] GET_PUBLIC_ENDPOINT = {
            "/medical-services", "/medical-services/{id}"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF cho REST API
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless session
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, POST_PUBLIC_ENDPOINT).permitAll()
                .requestMatchers(HttpMethod.GET, GET_PUBLIC_ENDPOINT).permitAll()
                .requestMatchers(HttpMethod.GET, "/roles").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/roles").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/users/all").hasAuthority("ROLE_ADMIN")
//                .requestMatchers(HttpMethod.PUT, "/users/{id}").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
