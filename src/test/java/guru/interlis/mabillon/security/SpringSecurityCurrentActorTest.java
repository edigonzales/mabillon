package guru.interlis.mabillon.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SpringSecurityCurrentActorTest {

    private final SpringSecurityCurrentActor actor = new SpringSecurityCurrentActor(
            "admin", "anna.mueller",
            "sachbearbeiter", "a.keller");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsAdminLoginToConfiguredDomainUser() {
        authenticate("admin", "ROLE_MABILLON_ADMIN");

        assertThat(actor.id().value()).isEqualTo("anna.mueller");
        assertThat(actor.username()).isEqualTo("anna.mueller");
        assertThat(actor.roles()).containsExactly(MabillonRole.ADMIN);
    }

    @Test
    void mapsSachbearbeiterLoginToConfiguredDomainUser() {
        authenticate("sachbearbeiter", "ROLE_MABILLON_SACHBEARBEITER");

        assertThat(actor.id().value()).isEqualTo("a.keller");
        assertThat(actor.username()).isEqualTo("a.keller");
        assertThat(actor.roles()).containsExactly(MabillonRole.SACHBEARBEITER);
    }

    @Test
    void keepsAlreadyDomainUsernameUnchanged() {
        authenticate("p.steiner", "ROLE_MABILLON_SACHBEARBEITER");

        assertThat(actor.id().value()).isEqualTo("p.steiner");
        assertThat(actor.username()).isEqualTo("p.steiner");
    }

    @Test
    void requiresAuthenticatedIdentity() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(actor::id)
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("angemeldete Identität");
    }

    private static void authenticate(String username, String authority) {
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                "unused",
                List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
