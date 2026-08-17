package guru.interlis.mabillon.registraturplan;

import java.util.UUID;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Ordnungssystem;
import guru.interlis.mabillon.persistence.cayenne.Ordnungssystemposition;
import guru.interlis.mabillon.persistence.cayenne.Organisationseinheit;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class RegistraturplanAdminService {

    private static final String ACTIVE = "Aktiv";
    private static final String INACTIVE = "inaktiv";

    private final CayenneUnitOfWork unitOfWork;
    private final AuthorizationService authorizationService;

    public RegistraturplanAdminService(
            CayenneUnitOfWork unitOfWork,
            AuthorizationService authorizationService) {
        this.unitOfWork = unitOfWork;
        this.authorizationService = authorizationService;
    }

    public RegistraturplanView createPlan(CreateRegistraturplanCommand command) {
        authorizationService.require(Permission.MANAGE_REGISTRATURPLAN);
        return unitOfWork.write(context -> {
            if (findPlan(context, command.code()) != null) {
                throw new IllegalArgumentException("Registraturplancode ist bereits vorhanden: " + command.code());
            }
            Organisationseinheit organisationseinheit = findOrganisationseinheit(
                    context, command.organisationseinheit());
            if (organisationseinheit == null || !isActive(organisationseinheit.getAstatus())) {
                throw new IllegalArgumentException("Aktive Organisationseinheit fehlt: "
                        + command.organisationseinheit());
            }
            Ordnungssystem plan = context.newObject(Ordnungssystem.class);
            plan.setAcode(command.code());
            plan.setAname(command.name());
            plan.setGueltigvon(command.gueltigVon());
            plan.setAstatus(ACTIVE);
            plan.setTBasket(organisationseinheit.getTBasket());
            plan.setTIliTid(UUID.randomUUID());
            plan.setOrganisationseinheit(organisationseinheit);
            return new RegistraturplanView(plan.getAcode(), plan.getAname(), plan.getGueltigvon(),
                    plan.getGueltigbis(), plan.getAstatus(), organisationseinheit.getKuerzel());
        });
    }

    public RegistraturplanPositionView createPosition(CreatePositionCommand command) {
        authorizationService.require(Permission.MANAGE_REGISTRATURPLAN);
        return unitOfWork.write(context -> {
            Ordnungssystem plan = findPlan(context, command.planCode());
            if (plan == null) {
                throw new IllegalArgumentException("Unbekannter Registraturplan: " + command.planCode());
            }
            if (findPosition(context, command.code()) != null) {
                throw new IllegalArgumentException("Positionscode ist bereits vorhanden: " + command.code());
            }
            Organisationseinheit organisationseinheit = findOrganisationseinheit(
                    context, command.federfuehrendeEinheit());
            if (organisationseinheit == null || !isActive(organisationseinheit.getAstatus())) {
                throw new IllegalArgumentException("Aktive federführende Organisationseinheit fehlt: "
                        + command.federfuehrendeEinheit());
            }
            Ordnungssystemposition parent = command.parentCode() == null || command.parentCode().isBlank()
                    ? null : findPosition(context, command.parentCode());
            if (command.parentCode() != null && parent == null) {
                throw new IllegalArgumentException("Unbekannte Oberposition: " + command.parentCode());
            }
            if (parent != null && parent.getOrdnungssystem() != plan) {
                throw new IllegalArgumentException("Oberposition gehört zu einem anderen Registraturplan.");
            }
            Ordnungssystemposition position = context.newObject(Ordnungssystemposition.class);
            position.setAcode(command.code());
            position.setTitel(command.titel());
            position.setBeschreibung(command.beschreibung());
            position.setAstatus(ACTIVE.toLowerCase());
            position.setTBasket(plan.getTBasket());
            position.setTIliTid(UUID.randomUUID());
            position.setOrdnungssystem(plan);
            position.setOrganisationseinheit(organisationseinheit);
            position.setOrdnungssystemposition(parent == null ? position : parent);
            return toView(position);
        });
    }

    public RegistraturplanPositionView updatePosition(UpdatePositionCommand command) {
        authorizationService.require(Permission.MANAGE_REGISTRATURPLAN);
        return unitOfWork.write(context -> {
            Ordnungssystemposition position = findPosition(context, command.code());
            if (position == null) {
                throw new IllegalArgumentException("Unbekannte Registraturplanposition: " + command.code());
            }
            position.setTitel(command.titel());
            position.setBeschreibung(command.beschreibung());
            position.setAstatus(command.status());
            return toView(position);
        });
    }

    public void movePosition(String code, String newParentCode) {
        authorizationService.require(Permission.MANAGE_REGISTRATURPLAN);
        unitOfWork.write(context -> {
            Ordnungssystemposition position = findPosition(context, code);
            if (position == null) {
                throw new IllegalArgumentException("Unbekannte Registraturplanposition: " + code);
            }
            Ordnungssystemposition newParent = newParentCode == null || newParentCode.isBlank()
                    ? null : findPosition(context, newParentCode);
            if (newParentCode != null && !newParentCode.isBlank() && newParent == null) {
                throw new IllegalArgumentException("Unbekannte Oberposition: " + newParentCode);
            }
            if (newParent != null && newParent.getOrdnungssystem() != position.getOrdnungssystem()) {
                throw new IllegalArgumentException("Oberposition gehört zu einem anderen Registraturplan.");
            }
            if (newParent != null && createsCycle(position, newParent)) {
                throw new IllegalStateException("Die Verschiebung würde einen Registraturplan-Zyklus erzeugen.");
            }
            position.setOrdnungssystemposition(newParent == null ? position : newParent);
        });
    }

    public void activatePlan(String code) {
        authorizationService.require(Permission.MANAGE_REGISTRATURPLAN);
        unitOfWork.write(context -> {
            Ordnungssystem plan = requirePlan(context, code);
            plan.setAstatus(ACTIVE);
        });
    }

    public void replacePlan(String code, java.time.LocalDate validTo) {
        authorizationService.require(Permission.MANAGE_REGISTRATURPLAN);
        unitOfWork.write(context -> {
            Ordnungssystem plan = requirePlan(context, code);
            plan.setGueltigbis(validTo);
            plan.setAstatus("Ersetzt");
        });
    }

    public void deactivatePosition(String code) {
        authorizationService.require(Permission.MANAGE_REGISTRATURPLAN);
        unitOfWork.write(context -> {
            Ordnungssystemposition position = findPosition(context, code);
            if (position == null) {
                throw new IllegalArgumentException("Unbekannte Registraturplanposition: " + code);
            }
            position.setAstatus(INACTIVE);
        });
    }

    private boolean createsCycle(Ordnungssystemposition position, Ordnungssystemposition proposedParent) {
        Ordnungssystemposition cursor = proposedParent;
        while (cursor != null) {
            if (cursor == position) {
                return true;
            }
            Ordnungssystemposition parent = cursor.getOrdnungssystemposition();
            if (parent == null || parent == cursor) {
                return false;
            }
            cursor = parent;
        }
        return false;
    }

    private Ordnungssystem requirePlan(ObjectContext context, String code) {
        Ordnungssystem plan = findPlan(context, code);
        if (plan == null) {
            throw new IllegalArgumentException("Unbekannter Registraturplan: " + code);
        }
        return plan;
    }

    private Ordnungssystem findPlan(ObjectContext context, String code) {
        return ObjectSelect.query(Ordnungssystem.class)
                .where(Ordnungssystem.ACODE.eq(code))
                .selectFirst(context);
    }

    private Ordnungssystemposition findPosition(ObjectContext context, String code) {
        return ObjectSelect.query(Ordnungssystemposition.class)
                .where(Ordnungssystemposition.ACODE.eq(code))
                .selectFirst(context);
    }

    private Organisationseinheit findOrganisationseinheit(ObjectContext context, String code) {
        return ObjectSelect.query(Organisationseinheit.class)
                .where(Organisationseinheit.KUERZEL.eq(code))
                .selectFirst(context);
    }

    private boolean isActive(String status) {
        return "aktiv".equalsIgnoreCase(status) || "Aktiv".equalsIgnoreCase(status);
    }

    private RegistraturplanPositionView toView(Ordnungssystemposition position) {
        Ordnungssystemposition parent = position.getOrdnungssystemposition();
        return new RegistraturplanPositionView(
                position.getAcode(),
                position.getTitel(),
                position.getBeschreibung(),
                position.getAstatus(),
                position.getOrdnungssystem() == null ? null : position.getOrdnungssystem().getAcode(),
                parent == null || parent == position ? null : parent.getAcode(),
                position.getOrganisationseinheit() == null ? null : position.getOrganisationseinheit().getKuerzel(),
                position.getOrdnungssystempositions().stream().allMatch(child -> child == position),
                position.getOrdnungssystempositions().stream()
                        .filter(child -> child != position)
                        .map(this::toView)
                        .toList());
    }
}
