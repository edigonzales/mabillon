package guru.interlis.mabillon.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public final class SpringSecurityCurrentActor implements CurrentActor {

    private final Map<String, String> principalMappings;

    public SpringSecurityCurrentActor(
            @Value("${mabillon.security.admin-username:admin}") String adminUsername,
            @Value("${mabillon.security.admin-actor-username:anna.mueller}") String adminActorUsername,
            @Value("${mabillon.security.sachbearbeiter-username:sachbearbeiter}") String sachbearbeiterUsername,
            @Value("${mabillon.security.sachbearbeiter-actor-username:a.keller}") String sachbearbeiterActorUsername) {
        this.principalMappings = Map.of(
                adminUsername, adminActorUsername,
                sachbearbeiterUsername, sachbearbeiterActorUsername);
    }

    @Override
    public ActorId id() {
        return new ActorId(actorUsername());
    }

    @Override
    public String username() {
        return actorUsername();
    }

    @Override
    public String displayName() {
        return username();
    }

    @Override
    public Set<MabillonRole> roles() {
        Authentication authentication = authentication();
        EnumSet<MabillonRole> roles = EnumSet.noneOf(MabillonRole.class);
        authentication.getAuthorities().forEach(authority -> {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_MABILLON_")) {
                String role = name.substring("ROLE_MABILLON_".length());
                Arrays.stream(MabillonRole.values())
                        .filter(candidate -> candidate.name().equals(role))
                        .findFirst()
                        .ifPresent(roles::add);
            }
        });
        return Set.copyOf(roles);
    }

    private String actorUsername() {
        String principal = authentication().getName();
        return principalMappings.getOrDefault(principal, principal);
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthorizationException("Eine angemeldete Identität ist erforderlich.");
        }
        return authentication;
    }
}
