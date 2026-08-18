package guru.interlis.mabillon.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health", "/actuator/health/**",
                                "/mabillon.css", "/mabillon.js", "/htmx-2.0.10.min.js", "/favicon.ico",
                                "/fonts/**")
                        .permitAll()
                        .requestMatchers("/actuator", "/actuator/**").hasRole("MABILLON_ADMIN")
                        .requestMatchers("/admin", "/admin/**").hasRole("MABILLON_ADMIN")
                        .anyRequest().hasAnyRole(
                                "MABILLON_SACHBEARBEITER",
                                "MABILLON_ADMIN",
                                "MABILLON_GEVER_VERANTWORTLICHER",
                                "MABILLON_ARCHIVVERANTWORTLICHER"))
                .httpBasic(httpBasic -> {})
                .formLogin(form -> form.disable())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'; "
                                        + "img-src 'self' data:; object-src 'none'; base-uri 'self'; "
                                        + "frame-ancestors 'none'; form-action 'self'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            CsrfTokenRequestHandler delegate = StringUtils.hasText(headerValue) ? plain : xor;
            return delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    @Bean
    @Profile({"dev", "test"})
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

    @Bean
    @Profile("!dev & !test")
    UserDetailsService noLocalUsers() {
        return username -> {
            throw new UsernameNotFoundException("Local Mabillon users are disabled outside dev/test profiles.");
        };
    }
}
