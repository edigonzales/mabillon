package guru.interlis.mabillon.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AuthorizationPermissionMatrixTest {

    @Test
    void sachbearbeiterHasOperationalButNoAdministrativeArchiveOrQualityPermissions() {
        AuthorizationService service = serviceFor(MabillonRole.SACHBEARBEITER);

        assertThat(allowed(service)).containsExactlyInAnyOrder(
                Permission.VIEW_MABILLON,
                Permission.EDIT_GESCHAEFT,
                Permission.EDIT_DOSSIER,
                Permission.EDIT_UNTERLAGE,
                Permission.EDIT_AUFGABE,
                Permission.MANAGE_INTERLIS_EXCHANGE);
    }

    @Test
    void geverResponsibleCanCloseAndRunQualityButCannotAdministerOrArchive() {
        AuthorizationService service = serviceFor(MabillonRole.GEVER_VERANTWORTLICHER);

        assertThat(allowed(service)).containsExactlyInAnyOrder(
                Permission.VIEW_MABILLON,
                Permission.EDIT_GESCHAEFT,
                Permission.EDIT_DOSSIER,
                Permission.EDIT_UNTERLAGE,
                Permission.EDIT_AUFGABE,
                Permission.MANAGE_INTERLIS_EXCHANGE,
                Permission.CLOSE_DOSSIER,
                Permission.RUN_DATA_QUALITY);
    }

    @Test
    void archiveResponsibleHasOnlyArchiveRelevantCapabilities() {
        AuthorizationService service = serviceFor(MabillonRole.ARCHIVVERANTWORTLICHER);

        assertThat(allowed(service)).containsExactlyInAnyOrder(
                Permission.VIEW_MABILLON,
                Permission.RUN_DATA_QUALITY,
                Permission.MANAGE_ARCHIVE_DELIVERY);
    }

    @Test
    void administratorHasEveryDeclaredPermission() {
        AuthorizationService service = serviceFor(MabillonRole.ADMIN);
        assertThat(allowed(service)).containsExactlyInAnyOrder(Permission.values());
    }

    private static Set<Permission> allowed(AuthorizationService service) {
        EnumSet<Permission> result = EnumSet.noneOf(Permission.class);
        for (Permission permission : Permission.values()) {
            if (service.has(permission)) {
                result.add(permission);
            }
        }
        return result;
    }

    private static AuthorizationService serviceFor(MabillonRole role) {
        CurrentActor actor = new CurrentActor() {
            @Override
            public ActorId id() {
                return ActorId.of("permission-test");
            }

            @Override
            public String username() {
                return "permission-test";
            }

            @Override
            public String displayName() {
                return "Permission Test";
            }

            @Override
            public Set<MabillonRole> roles() {
                return Set.of(role);
            }
        };
        return new AuthorizationService(actor);
    }
}
