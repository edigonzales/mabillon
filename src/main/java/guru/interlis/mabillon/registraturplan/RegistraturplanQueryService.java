package guru.interlis.mabillon.registraturplan;

import java.util.Comparator;
import java.util.List;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Ordnungssystem;
import guru.interlis.mabillon.persistence.cayenne.Ordnungssystemposition;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class RegistraturplanQueryService {

    private final CayenneUnitOfWork unitOfWork;

    public RegistraturplanQueryService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public List<RegistraturplanView> listPlans(boolean includeReplaced) {
        return unitOfWork.read(context -> ObjectSelect.query(Ordnungssystem.class).select(context).stream()
                .filter(plan -> includeReplaced || !"Ersetzt".equalsIgnoreCase(plan.getAstatus()))
                .map(this::toPlanView)
                .sorted(Comparator.comparing(RegistraturplanView::code))
                .toList());
    }

    public RegistraturplanTreeView getTree(String planCode) {
        return unitOfWork.read(context -> {
            Ordnungssystem plan = findPlan(context, planCode);
            if (plan == null) {
                throw new IllegalArgumentException("Unbekannter Registraturplan: " + planCode);
            }
            List<Ordnungssystemposition> positions = plan.getOrdnungssystempositions();
            List<RegistraturplanPositionView> roots = positions.stream()
                    .filter(position -> isRoot(position, positions))
                    .sorted(Comparator.comparing(Ordnungssystemposition::getAcode))
                    .map(this::toPositionView)
                    .toList();
            return new RegistraturplanTreeView(toPlanView(plan), roots);
        });
    }

    public RegistraturplanPositionView getPosition(String code) {
        return unitOfWork.read(context -> {
            Ordnungssystemposition position = findPosition(context, code);
            if (position == null) {
                throw new IllegalArgumentException("Unbekannte Registraturplanposition: " + code);
            }
            return toPositionView(position);
        });
    }

    public List<RegistraturplanPositionView> activeLeafPositions() {
        return unitOfWork.read(context -> ObjectSelect.query(Ordnungssystemposition.class).select(context).stream()
                .filter(position -> position.getOrdnungssystem() != null
                        && isActive(position.getOrdnungssystem().getAstatus())
                        && isActive(position.getAstatus())
                        && position.getOrdnungssystempositions().isEmpty())
                .map(this::toPositionView)
                .sorted(Comparator.comparing(RegistraturplanPositionView::code))
                .toList());
    }

    private RegistraturplanView toPlanView(Ordnungssystem plan) {
        return new RegistraturplanView(
                plan.getAcode(),
                plan.getAname(),
                plan.getGueltigvon(),
                plan.getGueltigbis(),
                plan.getAstatus(),
                plan.getOrganisationseinheit() == null ? null : plan.getOrganisationseinheit().getKuerzel());
    }

    private RegistraturplanPositionView toPositionView(Ordnungssystemposition position) {
        Ordnungssystemposition parent = position.getOrdnungssystemposition();
        String parentCode = parent == null || parent == position ? null : parent.getAcode();
        List<RegistraturplanPositionView> children = position.getOrdnungssystempositions().stream()
                .filter(child -> child != position)
                .sorted(Comparator.comparing(Ordnungssystemposition::getAcode))
                .map(this::toPositionView)
                .toList();
        return new RegistraturplanPositionView(
                position.getAcode(),
                position.getTitel(),
                position.getBeschreibung(),
                position.getAstatus(),
                position.getOrdnungssystem() == null ? null : position.getOrdnungssystem().getAcode(),
                parentCode,
                position.getOrganisationseinheit() == null ? null : position.getOrganisationseinheit().getKuerzel(),
                children.isEmpty(),
                children);
    }

    private boolean isRoot(Ordnungssystemposition position, List<Ordnungssystemposition> positions) {
        Ordnungssystemposition parent = position.getOrdnungssystemposition();
        return parent == null || parent == position || !positions.contains(parent);
    }

    private Ordnungssystem findPlan(org.apache.cayenne.ObjectContext context, String code) {
        return ObjectSelect.query(Ordnungssystem.class)
                .where(Ordnungssystem.ACODE.eq(code))
                .selectFirst(context);
    }

    private Ordnungssystemposition findPosition(org.apache.cayenne.ObjectContext context, String code) {
        return ObjectSelect.query(Ordnungssystemposition.class)
                .where(Ordnungssystemposition.ACODE.eq(code))
                .selectFirst(context);
    }

    private boolean isActive(String status) {
        return "aktiv".equalsIgnoreCase(status) || "Aktiv".equalsIgnoreCase(status);
    }
}
