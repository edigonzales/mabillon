package guru.interlis.mabillon.security;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public final class AuthorizationService {

    private final CurrentActor currentActor;

    public AuthorizationService(CurrentActor currentActor) {
        this.currentActor = currentActor;
    }

    public void require(Permission permission) {
        if (!has(permission)) {
            throw new AuthorizationException("Berechtigung fehlt: " + permission);
        }
    }

    public boolean has(Permission permission) {
        Set<MabillonRole> roles;
        try {
            roles = currentActor.roles();
        } catch (AuthorizationException ignored) {
            return false;
        }
        return roles.stream().anyMatch(role -> permissions(role).contains(permission));
    }

    private Set<Permission> permissions(MabillonRole role) {
        if (role == MabillonRole.ADMIN) {
            return EnumSet.allOf(Permission.class);
        }
        return switch (role) {
            case SACHBEARBEITER -> EnumSet.of(
                    Permission.VIEW_MABILLON,
                    Permission.EDIT_GESCHAEFT,
                    Permission.EDIT_DOSSIER,
                    Permission.EDIT_UNTERLAGE,
                    Permission.EDIT_AUFGABE,
                    Permission.MANAGE_INTERLIS_EXCHANGE);
            case GEVER_VERANTWORTLICHER -> EnumSet.of(
                    Permission.VIEW_MABILLON,
                    Permission.EDIT_GESCHAEFT,
                    Permission.EDIT_DOSSIER,
                    Permission.EDIT_UNTERLAGE,
                    Permission.EDIT_AUFGABE,
                    Permission.CLOSE_DOSSIER,
                    Permission.RUN_DATA_QUALITY,
                    Permission.MANAGE_INTERLIS_EXCHANGE);
            case ARCHIVVERANTWORTLICHER -> EnumSet.of(
                    Permission.VIEW_MABILLON,
                    Permission.RUN_DATA_QUALITY,
                    Permission.MANAGE_ARCHIVE_DELIVERY);
            case ADMIN -> EnumSet.allOf(Permission.class);
        };
    }
}
