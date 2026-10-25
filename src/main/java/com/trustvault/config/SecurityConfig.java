
package com.trustvault.config;

import com.trustvault.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager trustVaultAuthenticationManager(
            LoginAttemptAuthenticationProvider loginAttemptProvider) {

        return new ProviderManager(loginAttemptProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager trustVaultAuthenticationManager)
            throws Exception {

        http
            .authenticationManager(trustVaultAuthenticationManager)

            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(
                    new CsrfTokenRequestAttributeHandler() {
                        @Override
                        public void handle(
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Supplier<CsrfToken> csrfToken) {

                            csrfToken.get();
                            super.handle(request, response, csrfToken);
                        }
                    })
                .ignoringRequestMatchers(
                    "/api/auth/**",
                    "/api/test",
                    "/login"
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin.html", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/register.html",
                    "/login.html",
                    "/error",
                    "/favicon.ico",
                    "/api/test",
                    "/api/auth/**",
                    "/css/**",
                    "/js/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
    .sessionFixation(fixation -> fixation.changeSessionId())
    .maximumSessions(1)
    .maxSessionsPreventsLogin(false)
)
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/login.html?logout=true")
    .invalidateHttpSession(true)
    .clearAuthentication(true)
    .deleteCookies("JSESSIONID")
    .permitAll()
)

            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard.html", true)
                .failureUrl("/login.html?error=true")
                .permitAll()
            );

        return http.build();
    }
}
