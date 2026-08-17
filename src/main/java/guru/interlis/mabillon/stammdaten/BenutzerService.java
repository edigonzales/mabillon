package guru.interlis.mabillon.stammdaten;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class BenutzerService {

    private static final String ACTIVE = "aktiv";
    private static final String INACTIVE = "inaktiv";

    private final CayenneUnitOfWork unitOfWork;
    private final AuthorizationService authorizationService;

    public BenutzerService(CayenneUnitOfWork unitOfWork, AuthorizationService authorizationService) {
        this.unitOfWork = unitOfWork;
        this.authorizationService = authorizationService;
    }

    public List<BenutzerView> list(boolean includeInactive) {
        return unitOfWork.read(context -> ObjectSelect.query(Benutzer.class).select(context).stream()
                .filter(value -> includeInactive || ACTIVE.equalsIgnoreCase(value.getAstatus()))
                .map(this::toView)
                .sorted(Comparator.comparing(BenutzerView::username))
                .toList());
    }

    public BenutzerView create(BenutzerCreateCommand command) {
        authorizationService.require(Permission.MANAGE_MASTERDATA);
        return unitOfWork.write(context -> {
            if (find(context, command.username()) != null) {
                throw new IllegalArgumentException("Username ist bereits vorhanden: " + command.username());
            }
            Organisationseinheit organisationseinheit = ObjectSelect.query(Organisationseinheit.class)
                    .where(Organisationseinheit.KUERZEL.eq(command.organisationseinheit()))
                    .selectFirst(context);
            if (organisationseinheit == null || !ACTIVE.equalsIgnoreCase(organisationseinheit.getAstatus())) {
                throw new IllegalArgumentException("Aktive Organisationseinheit fehlt: "
                        + command.organisationseinheit());
            }
            Benutzer entity = context.newObject(Benutzer.class);
            entity.setUsername(command.username());
            entity.setAname(command.name());
            entity.setEmail(command.email());
            entity.setAstatus(ACTIVE);
            entity.setTBasket(organisationseinheit.getTBasket());
            entity.setTIliTid(UUID.randomUUID());
            entity.setOrganisationseinheit(organisationseinheit);
            return toView(entity);
        });
    }

    public void deactivate(String username) {
        authorizationService.require(Permission.MANAGE_MASTERDATA);
        unitOfWork.write(context -> {
            Benutzer entity = find(context, username);
            if (entity == null) {
                throw new IllegalArgumentException("Unbekannter Benutzer: " + username);
            }
            entity.setAstatus(INACTIVE);
        });
    }

    private Benutzer find(ObjectContext context, String username) {
        return ObjectSelect.query(Benutzer.class)
                .where(Benutzer.USERNAME.eq(username))
                .selectFirst(context);
    }

    private BenutzerView toView(Benutzer value) {
        return new BenutzerView(
                value.getUsername(),
                value.getAname(),
                value.getEmail(),
                value.getAstatus(),
                value.getOrganisationseinheit() == null ? null : value.getOrganisationseinheit().getKuerzel());
    }
}
