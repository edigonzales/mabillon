package guru.interlis.mabillon.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public final class SpringSecurityCurrentActor implements CurrentActor {

    @Override
    public ActorId id() {
        return new ActorId(authentication().getName());
    }

    @Override
    public String username() {
        return authentication().getName();
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

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthorizationException("Eine angemeldete Identität ist erforderlich.");
        }
        return authentication;
    }
}
