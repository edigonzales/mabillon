package guru.interlis.mabillon.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health", "/actuator/health/**",
                                "/mabillon.css", "/mabillon.js", "/htmx-2.0.10.min.js", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/actuator", "/actuator/**").hasRole("MABILLON_ADMIN")
                        .requestMatchers("/admin", "/admin/**").hasRole("MABILLON_ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> {})
                .formLogin(form -> form.disable())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'; "
                                        + "img-src 'self' data:; object-src 'none'; base-uri 'self'; "
                                        + "frame-ancestors 'none'; form-action 'self'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    @Bean
    UserDetailsService devUsers(
            @Value("${mabillon.security.admin-username:admin}") String adminUsername,
            @Value("${mabillon.security.admin-password:admin}") String adminPassword,
            @Value("${mabillon.security.sachbearbeiter-username:sachbearbeiter}") String sachbearbeiterUsername,
            @Value("${mabillon.security.sachbearbeiter-password:sachbearbeiter}") String sachbearbeiterPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(adminUsername)
                        .password("{noop}" + adminPassword)
                        .roles("MABILLON_ADMIN")
                        .build(),
                User.withUsername(sachbearbeiterUsername)
                        .password("{noop}" + sachbearbeiterPassword)
                        .roles("MABILLON_SACHBEARBEITER")
                        .build());
    }
}
