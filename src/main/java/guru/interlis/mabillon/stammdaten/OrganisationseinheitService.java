package guru.interlis.mabillon.stammdaten;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class OrganisationseinheitService {

    private static final String ACTIVE = "aktiv";
    private static final String INACTIVE = "inaktiv";

    private final CayenneUnitOfWork unitOfWork;
    private final AuthorizationService authorizationService;

    public OrganisationseinheitService(CayenneUnitOfWork unitOfWork, AuthorizationService authorizationService) {
        this.unitOfWork = unitOfWork;
        this.authorizationService = authorizationService;
    }

    public List<OrganisationseinheitView> list(boolean includeInactive) {
        return unitOfWork.read(context -> ObjectSelect.query(Organisationseinheit.class).select(context).stream()
                .filter(value -> includeInactive || ACTIVE.equalsIgnoreCase(value.getAstatus()))
                .map(this::toView)
                .sorted(Comparator.comparing(OrganisationseinheitView::kuerzel))
                .toList());
    }

    public OrganisationseinheitView create(OrganisationseinheitCreateCommand command) {
        authorizationService.require(Permission.MANAGE_MASTERDATA);
        return unitOfWork.write(context -> {
            if (find(context, command.kuerzel()) != null) {
                throw new IllegalArgumentException("Kürzel ist bereits vorhanden: " + command.kuerzel());
            }
            Organisationseinheit entity = context.newObject(Organisationseinheit.class);
            entity.setKuerzel(command.kuerzel());
            entity.setAname(command.name());
            entity.setBeschreibung(command.beschreibung());
            entity.setAstatus(ACTIVE);
            entity.setTBasket(stammdatenBasket(context));
            entity.setTIliTid(UUID.randomUUID());
            if (command.uebergeordneteEinheit() != null && !command.uebergeordneteEinheit().isBlank()) {
                Organisationseinheit parent = find(context, command.uebergeordneteEinheit());
                if (parent == null) {
                    throw new IllegalArgumentException("Unbekanntes übergeordnetes Kürzel: "
                            + command.uebergeordneteEinheit());
                }
                entity.setOrganisationseinheit(parent);
            }
            return toView(entity);
        });
    }

    public void deactivate(String kuerzel) {
        authorizationService.require(Permission.MANAGE_MASTERDATA);
        unitOfWork.write(context -> {
            Organisationseinheit entity = find(context, kuerzel);
            if (entity == null) {
                throw new IllegalArgumentException("Unbekanntes Organisationskürzel: " + kuerzel);
            }
            entity.setAstatus(INACTIVE);
        });
    }

    private Organisationseinheit find(ObjectContext context, String kuerzel) {
        return ObjectSelect.query(Organisationseinheit.class)
                .where(Organisationseinheit.KUERZEL.eq(kuerzel))
                .selectFirst(context);
    }

    private long stammdatenBasket(ObjectContext context) {
        Organisationseinheit existing = ObjectSelect.query(Organisationseinheit.class).selectFirst(context);
        if (existing == null) {
            throw new IllegalStateException("Kein Stammdaten-Basket ist vorhanden.");
        }
        return existing.getTBasket();
    }

    private OrganisationseinheitView toView(Organisationseinheit value) {
        return new OrganisationseinheitView(
                value.getKuerzel(),
                value.getAname(),
                value.getBeschreibung(),
                value.getAstatus(),
                value.getOrganisationseinheit() == null ? null : value.getOrganisationseinheit().getKuerzel());
    }
}
