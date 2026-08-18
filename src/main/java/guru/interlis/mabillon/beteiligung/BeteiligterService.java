package guru.interlis.mabillon.beteiligung;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Beteiligter;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.query.SearchPage;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class BeteiligterService {

    private static final List<String> TYPES = List.of("Person", "Organisation", "Interne_Organisationseinheit");
    private final CayenneUnitOfWork unitOfWork;
    private final AuthorizationService authorizationService;

    public BeteiligterService(CayenneUnitOfWork unitOfWork, AuthorizationService authorizationService) {
        this.unitOfWork = unitOfWork;
        this.authorizationService = authorizationService;
    }

    public BeteiligterView create(CreateBeteiligterCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        requireType(command.typ());
        return unitOfWork.write(context -> {
            Beteiligter value = context.newObject(Beteiligter.class);
            value.setTyp(command.typ());
            value.setAname(command.name());
            value.setVorname(command.vorname());
            value.setOrganisation(command.organisation());
            value.setEmail(command.email());
            value.setTelefon(command.telefon());
            value.setAdresse(command.adresse());
            value.setExternereferenz(command.externeReferenz());
            value.setTBasket(businessBasket(context));
            value.setTIliTid(UUID.randomUUID());
            return toView(value);
        });
    }

    public List<BeteiligterView> findPotentialDuplicates(CreateBeteiligterCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        requireType(command.typ());
        return unitOfWork.read(context -> ObjectSelect.query(Beteiligter.class)
                .where(Beteiligter.TYP.eq(command.typ()))
                .and(duplicateCandidateExpression(command))
                .orderBy(Beteiligter.ANAME.asc())
                .select(context).stream()
                .filter(value -> isPotentialDuplicate(value, command))
                .limit(10)
                .map(this::toView)
                .toList());
    }

    public BeteiligterView update(UpdateBeteiligterCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        requireType(command.typ());
        return unitOfWork.write(context -> {
            Beteiligter value = find(context, command.tid());
            if (value == null) {
                throw new IllegalArgumentException("Unbekannter Beteiligter: " + command.tid());
            }
            value.setTyp(command.typ());
            value.setAname(command.name());
            value.setVorname(command.vorname());
            value.setOrganisation(command.organisation());
            value.setEmail(command.email());
            value.setTelefon(command.telefon());
            value.setAdresse(command.adresse());
            value.setExternereferenz(command.externeReferenz());
            return toView(value);
        });
    }

    public SearchPage<BeteiligterView> search(BeteiligterSearchCriteria criteria, int page, int size) {
        requirePage(page, size);
        BeteiligterSearchCriteria filter = criteria == null ? BeteiligterSearchCriteria.empty() : criteria;
        return unitOfWork.read(context -> {
            ObjectSelect<Beteiligter> query = ObjectSelect.query(Beteiligter.class);
            addFilter(query, filter.name() == null ? null : Beteiligter.ANAME.containsIgnoreCase(filter.name()));
            addFilter(query, filter.typ() == null ? null : Beteiligter.TYP.eq(filter.typ()));
            addFilter(query, filter.externeReferenz() == null ? null
                    : Beteiligter.EXTERNEREFERENZ.contains(filter.externeReferenz()));

            long total = query.selectCount(context);
            long offset = (long) page * size;
            if (offset >= total) {
                return new SearchPage<>(List.of(), page, size, total);
            }
            List<BeteiligterView> items = query
                    .orderBy(Beteiligter.ANAME.asc())
                    .offset(Math.toIntExact(offset))
                    .limit(size)
                    .select(context).stream()
                    .map(this::toView)
                    .toList();
            return new SearchPage<>(items, page, size, total);
        });
    }

    public BeteiligterView get(UUID tid) {
        return unitOfWork.read(context -> {
            Beteiligter value = find(context, tid);
            if (value == null) {
                throw new IllegalArgumentException("Unbekannter Beteiligter: " + tid);
            }
            return toView(value);
        });
    }

    private Beteiligter find(ObjectContext context, UUID tid) {
        return ObjectSelect.query(Beteiligter.class).where(Beteiligter.T_ILI_TID.eq(tid)).selectFirst(context);
    }

    private long businessBasket(ObjectContext context) {
        Geschaeft business = ObjectSelect.query(Geschaeft.class).selectFirst(context);
        if (business != null) {
            return business.getTBasket();
        }
        Dossier dossier = ObjectSelect.query(Dossier.class).selectFirst(context);
        if (dossier != null) {
            return dossier.getTBasket();
        }
        throw new IllegalStateException("Kein Geschäftsdaten-Basket vorhanden.");
    }

    private void requireType(String type) {
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("Unbekannter Beteiligtertyp: " + type);
        }
    }

    private Expression duplicateCandidateExpression(CreateBeteiligterCommand command) {
        List<Expression> candidates = new ArrayList<>();
        if (command.externeReferenz() != null && !command.externeReferenz().isBlank()) {
            candidates.add(Beteiligter.EXTERNEREFERENZ.containsIgnoreCase(command.externeReferenz().trim()));
        }
        if (command.email() != null && !command.email().isBlank()) {
            candidates.add(Beteiligter.EMAIL.containsIgnoreCase(command.email().trim()));
        }
        String normalizedName = normalized(command.name());
        if (!normalizedName.isBlank()) {
            candidates.add(Beteiligter.ANAME.containsIgnoreCase(normalizedName.split(" ")[0]));
        }
        return ExpressionFactory.or(candidates.toArray(Expression[]::new));
    }

    private boolean isPotentialDuplicate(Beteiligter value, CreateBeteiligterCommand command) {
        if (sameNonBlank(value.getExternereferenz(), command.externeReferenz())
                || sameNonBlank(value.getEmail(), command.email())) {
            return true;
        }
        if (!normalized(value.getAname()).equals(normalized(command.name()))) {
            return false;
        }
        if ("Person".equals(command.typ())) {
            return normalized(value.getVorname()).equals(normalized(command.vorname()));
        }
        return true;
    }

    private BeteiligterView toView(Beteiligter value) {
        return new BeteiligterView(value.getTIliTid(), value.getTyp(), value.getAname(), value.getVorname(),
                value.getOrganisation(), value.getEmail(), value.getTelefon(), value.getAdresse(),
                value.getExternereferenz());
    }

    private static <T> void addFilter(ObjectSelect<T> query, Expression expression) {
        if (expression == null) {
            return;
        }
        if (query.getWhere() == null) {
            query.where(expression);
        } else {
            query.and(expression);
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Ungültige Seitendaten.");
        }
    }

    private static boolean sameNonBlank(String left, String right) {
        return right != null && !right.isBlank() && left != null && left.equalsIgnoreCase(right.trim());
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
