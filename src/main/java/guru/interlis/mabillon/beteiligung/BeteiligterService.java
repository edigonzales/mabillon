package guru.interlis.mabillon.beteiligung;

import java.util.Comparator;
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
        return unitOfWork.read(context -> ObjectSelect.query(Beteiligter.class).select(context).stream()
                .filter(value -> command.typ().equals(value.getTyp()))
                .filter(value -> isPotentialDuplicate(value, command))
                .sorted(Comparator.comparing(Beteiligter::getAname))
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
        if (criteria == null) {
            criteria = BeteiligterSearchCriteria.empty();
        }
        final BeteiligterSearchCriteria filter = criteria;
        return unitOfWork.read(context -> {
            List<BeteiligterView> values = ObjectSelect.query(Beteiligter.class).select(context).stream()
                    .filter(value -> containsIgnoreCase(value.getAname(), filter.name()))
                    .filter(value -> filter.typ() == null || filter.typ().equals(value.getTyp()))
                    .filter(value -> contains(value.getExternereferenz(), filter.externeReferenz()))
                    .sorted(Comparator.comparing(Beteiligter::getAname))
                    .map(this::toView)
                    .toList();
            return page(values, page, size);
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

    private static boolean contains(String value, String filter) {
        return filter == null || (value != null && value.contains(filter));
    }

    private static boolean containsIgnoreCase(String value, String filter) {
        return filter == null || (value != null && value.toLowerCase(Locale.ROOT)
                .contains(filter.toLowerCase(Locale.ROOT)));
    }

    private static boolean sameNonBlank(String left, String right) {
        return right != null && !right.isBlank() && left != null && left.equalsIgnoreCase(right.trim());
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static <T> SearchPage<T> page(List<T> values, int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Ungültige Seitendaten.");
        }
        int from = Math.min(page * size, values.size());
        int to = Math.min(from + size, values.size());
        return new SearchPage<>(values.subList(from, to), page, size, values.size());
    }
}
