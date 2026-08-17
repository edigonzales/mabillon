package guru.interlis.mabillon.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Aufgabentyp;
import guru.interlis.mabillon.persistence.cayenne.Beteiligungsrolle;
import guru.interlis.mabillon.persistence.cayenne.Geschaeftsart;
import guru.interlis.mabillon.persistence.cayenne.Prozessstatus;
import guru.interlis.mabillon.persistence.cayenne.Resultatstatus;
import guru.interlis.mabillon.persistence.cayenne.Unterlagentyp;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class CatalogService {

    private static final String ACTIVE = "aktiv";
    private static final String INACTIVE = "inaktiv";

    private final CayenneUnitOfWork unitOfWork;
    private final AuthorizationService authorizationService;
    private final long defaultCatalogBasketId;

    public CatalogService(
            CayenneUnitOfWork unitOfWork,
            AuthorizationService authorizationService,
            @Value("${mabillon.cayenne.catalog-basket-id:2}") long defaultCatalogBasketId) {
        this.unitOfWork = unitOfWork;
        this.authorizationService = authorizationService;
        this.defaultCatalogBasketId = defaultCatalogBasketId;
    }

    public List<CatalogEntryView> list(CatalogType type, boolean includeInactive) {
        Objects.requireNonNull(type, "type");
        return unitOfWork.read(context -> listEntities(context, type).stream()
                .filter(includeInactive ? ignored -> true : this::isActive)
                .map(entity -> toView(type, entity))
                .sorted(Comparator.comparing(CatalogEntryView::code))
                .toList());
    }

    public CatalogEntryView get(CatalogType type, String code) {
        return unitOfWork.read(context -> {
            Object entity = listEntities(context, type).stream()
                    .filter(candidate -> code.equals(codeOf(candidate)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unbekannter Katalogcode: " + code));
            return toView(type, entity);
        });
    }

    public CatalogEntryView create(CatalogCreateCommand command) {
        authorizationService.require(Permission.MANAGE_CATALOGS);
        return unitOfWork.write(context -> {
            ensureCodeIsFree(context, command.type(), command.code());
            Object entity = createEntity(context, command);
            return toView(command.type(), entity);
        });
    }

    public CatalogEntryView update(CatalogUpdateCommand command) {
        authorizationService.require(Permission.MANAGE_CATALOGS);
        return unitOfWork.write(context -> {
            Object entity = findEntity(context, command.type(), command.code());
            setNameAndDescription(entity, command.name(), command.description());
            switch (entity) {
                case Geschaeftsart value -> value.setResultaterforderlich(command.resultatErforderlich());
                case Prozessstatus value -> {
                    Geschaeftsart geschaeftsart = requireGeschaeftsart(context, command.geschaeftsartCode());
                    if (command.initial() && countOtherInitialStatuses(context, value, geschaeftsart.getAcode()) > 0) {
                        throw new IllegalStateException("Eine Geschäftsart darf nur einen Initialstatus haben.");
                    }
                    value.setGeschaeftsart(geschaeftsart);
                    value.setSortierung(command.sortierung());
                    value.setAinitial(command.initial());
                    value.setTerminal(command.terminal());
                }
                case Resultatstatus value -> {
                    value.setGeschaeftsart(requireGeschaeftsart(context, command.geschaeftsartCode()));
                    value.setSortierung(command.sortierung());
                    value.setTerminal(command.terminal());
                }
                case Beteiligungsrolle ignored -> { }
                case Unterlagentyp ignored -> { }
                case Aufgabentyp ignored -> { }
                default -> throw new IllegalArgumentException("Nicht unterstützter Katalogtyp: " + entity.getClass());
            }
            return toView(command.type(), entity);
        });
    }

    public void deactivate(CatalogType type, String code) {
        authorizationService.require(Permission.MANAGE_CATALOGS);
        unitOfWork.write(context -> {
            Object entity = findEntity(context, type, code);
            if (entity instanceof Prozessstatus processStatus && processStatus.isAinitial()
                    && countInitialStatuses(context, processStatus.getGeschaeftsart().getAcode()) == 1) {
                throw new IllegalStateException("Der einzige Initialstatus einer Geschäftsart darf nicht deaktiviert werden.");
            }
            setStatus(entity, INACTIVE);
        });
    }

    public void activate(CatalogType type, String code) {
        authorizationService.require(Permission.MANAGE_CATALOGS);
        unitOfWork.write(context -> {
            Object entity = findEntity(context, type, code);
            setStatus(entity, ACTIVE);
        });
    }

    public List<CatalogEntryView> processStatusesForGeschaeftsart(String geschaeftsartCode) {
        return list(CatalogType.PROZESSSTATUS, false).stream()
                .filter(status -> geschaeftsartCode.equals(status.geschaeftsartCode()))
                .sorted(Comparator.comparing(CatalogEntryView::sortierung, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<CatalogEntryView> resultStatusesForGeschaeftsart(String geschaeftsartCode) {
        return list(CatalogType.RESULTATSTATUS, false).stream()
                .filter(status -> geschaeftsartCode.equals(status.geschaeftsartCode()))
                .sorted(Comparator.comparing(CatalogEntryView::sortierung, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public CatalogEntryView initialProcessStatus(String geschaeftsartCode) {
        List<CatalogEntryView> initialStatuses = list(CatalogType.PROZESSSTATUS, true).stream()
                .filter(status -> geschaeftsartCode.equals(status.geschaeftsartCode()) && status.initial())
                .toList();
        if (initialStatuses.size() != 1) {
            throw new IllegalStateException("Geschäftsart " + geschaeftsartCode
                    + " muss genau einen Initialstatus haben, gefunden: " + initialStatuses.size());
        }
        return initialStatuses.getFirst();
    }

    private Object createEntity(ObjectContext context, CatalogCreateCommand command) {
        return switch (command.type()) {
            case GESCHAEFTSART -> {
                Geschaeftsart entity = context.newObject(Geschaeftsart.class);
                setCommon(entity, command, context);
                entity.setResultaterforderlich(command.resultatErforderlich());
                yield entity;
            }
            case PROZESSSTATUS -> {
                Geschaeftsart geschaeftsart = requireGeschaeftsart(context, command.geschaeftsartCode());
                if (command.initial() && countInitialStatuses(context, command.geschaeftsartCode()) > 0) {
                    throw new IllegalStateException("Eine Geschäftsart darf nur einen Initialstatus haben.");
                }
                Prozessstatus entity = context.newObject(Prozessstatus.class);
                setCommon(entity, command, context);
                entity.setGeschaeftsart(geschaeftsart);
                entity.setSortierung(command.sortierung());
                entity.setAinitial(command.initial());
                entity.setTerminal(command.terminal());
                yield entity;
            }
            case RESULTATSTATUS -> {
                Resultatstatus entity = context.newObject(Resultatstatus.class);
                setCommon(entity, command, context);
                entity.setGeschaeftsart(requireGeschaeftsart(context, command.geschaeftsartCode()));
                entity.setSortierung(command.sortierung());
                entity.setTerminal(command.terminal());
                yield entity;
            }
            case BETEILIGUNGSROLLE -> {
                Beteiligungsrolle entity = context.newObject(Beteiligungsrolle.class);
                setCommon(entity, command, context);
                yield entity;
            }
            case UNTERLAGENTYP -> {
                Unterlagentyp entity = context.newObject(Unterlagentyp.class);
                setCommon(entity, command, context);
                yield entity;
            }
            case AUFGABENTYP -> {
                Aufgabentyp entity = context.newObject(Aufgabentyp.class);
                setCommon(entity, command, context);
                yield entity;
            }
        };
    }

    private void setCommon(Object entity, CatalogCreateCommand command, ObjectContext context) {
        long basket = catalogBasket(context);
        if (entity instanceof Geschaeftsart value) {
            value.setAcode(command.code());
            value.setAname(command.name());
            value.setBeschreibung(command.description());
            value.setAstatus(ACTIVE);
            value.setTBasket(basket);
            value.setTIliTid(UUID.randomUUID());
        } else if (entity instanceof Prozessstatus value) {
            value.setAcode(command.code());
            value.setAname(command.name());
            value.setBeschreibung(command.description());
            value.setAstatus(ACTIVE);
            value.setTBasket(basket);
            value.setTIliTid(UUID.randomUUID());
        } else if (entity instanceof Resultatstatus value) {
            value.setAcode(command.code());
            value.setAname(command.name());
            value.setBeschreibung(command.description());
            value.setAstatus(ACTIVE);
            value.setTBasket(basket);
            value.setTIliTid(UUID.randomUUID());
        } else if (entity instanceof Beteiligungsrolle value) {
            value.setAcode(command.code());
            value.setAname(command.name());
            value.setBeschreibung(command.description());
            value.setAstatus(ACTIVE);
            value.setTBasket(basket);
            value.setTIliTid(UUID.randomUUID());
        } else if (entity instanceof Unterlagentyp value) {
            value.setAcode(command.code());
            value.setAname(command.name());
            value.setBeschreibung(command.description());
            value.setAstatus(ACTIVE);
            value.setTBasket(basket);
            value.setTIliTid(UUID.randomUUID());
        } else if (entity instanceof Aufgabentyp value) {
            value.setAcode(command.code());
            value.setAname(command.name());
            value.setBeschreibung(command.description());
            value.setAstatus(ACTIVE);
            value.setTBasket(basket);
            value.setTIliTid(UUID.randomUUID());
        } else {
            throw new IllegalArgumentException("Nicht unterstützter Katalogtyp: " + entity.getClass());
        }
    }

    private void setNameAndDescription(Object entity, String name, String description) {
        switch (entity) {
            case Geschaeftsart value -> { value.setAname(name); value.setBeschreibung(description); }
            case Prozessstatus value -> { value.setAname(name); value.setBeschreibung(description); }
            case Resultatstatus value -> { value.setAname(name); value.setBeschreibung(description); }
            case Beteiligungsrolle value -> { value.setAname(name); value.setBeschreibung(description); }
            case Unterlagentyp value -> { value.setAname(name); value.setBeschreibung(description); }
            case Aufgabentyp value -> { value.setAname(name); value.setBeschreibung(description); }
            default -> throw new IllegalArgumentException("Unbekannter Katalogeintrag: " + entity);
        }
    }

    private long catalogBasket(ObjectContext context) {
        Geschaeftsart existing = ObjectSelect.query(Geschaeftsart.class).selectFirst(context);
        return existing == null ? defaultCatalogBasketId : existing.getTBasket();
    }

    private void ensureCodeIsFree(ObjectContext context, CatalogType type, String code) {
        if (listEntities(context, type).stream().anyMatch(candidate -> code.equals(codeOf(candidate)))) {
            throw new IllegalArgumentException("Katalogcode ist bereits vorhanden: " + code);
        }
    }

    private Object findEntity(ObjectContext context, CatalogType type, String code) {
        return listEntities(context, type).stream()
                .filter(candidate -> code.equals(codeOf(candidate)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Katalogcode: " + code));
    }

    private Geschaeftsart findGeschaeftsart(ObjectContext context, String code) {
        return ObjectSelect.query(Geschaeftsart.class)
                .where(Geschaeftsart.ACODE.eq(code))
                .selectFirst(context);
    }

    private Geschaeftsart requireGeschaeftsart(ObjectContext context, String code) {
        Geschaeftsart value = code == null ? null : findGeschaeftsart(context, code);
        if (value == null) {
            throw new IllegalArgumentException("Unbekannte Geschäftsart: " + code);
        }
        return value;
    }

    private int countInitialStatuses(ObjectContext context, String geschaeftsartCode) {
        return ObjectSelect.query(Prozessstatus.class).select(context).stream()
                .filter(status -> status.isAinitial()
                        && status.getGeschaeftsart() != null
                        && geschaeftsartCode.equals(status.getGeschaeftsart().getAcode()))
                .mapToInt(ignored -> 1)
                .sum();
    }

    private int countOtherInitialStatuses(ObjectContext context, Prozessstatus current, String geschaeftsartCode) {
        return ObjectSelect.query(Prozessstatus.class).select(context).stream()
                .filter(status -> status != current)
                .filter(Prozessstatus::isAinitial)
                .filter(status -> status.getGeschaeftsart() != null
                        && geschaeftsartCode.equals(status.getGeschaeftsart().getAcode()))
                .mapToInt(ignored -> 1)
                .sum();
    }

    private List<?> listEntities(ObjectContext context, CatalogType type) {
        return switch (type) {
            case GESCHAEFTSART -> ObjectSelect.query(Geschaeftsart.class).select(context);
            case PROZESSSTATUS -> ObjectSelect.query(Prozessstatus.class).select(context);
            case RESULTATSTATUS -> ObjectSelect.query(Resultatstatus.class).select(context);
            case BETEILIGUNGSROLLE -> ObjectSelect.query(Beteiligungsrolle.class).select(context);
            case UNTERLAGENTYP -> ObjectSelect.query(Unterlagentyp.class).select(context);
            case AUFGABENTYP -> ObjectSelect.query(Aufgabentyp.class).select(context);
        };
    }

    private boolean isActive(Object entity) {
        return ACTIVE.equalsIgnoreCase(statusOf(entity));
    }

    private String codeOf(Object entity) {
        return switch (entity) {
            case Geschaeftsart value -> value.getAcode();
            case Prozessstatus value -> value.getAcode();
            case Resultatstatus value -> value.getAcode();
            case Beteiligungsrolle value -> value.getAcode();
            case Unterlagentyp value -> value.getAcode();
            case Aufgabentyp value -> value.getAcode();
            default -> throw new IllegalArgumentException("Unbekannter Katalogeintrag: " + entity);
        };
    }

    private String statusOf(Object entity) {
        return switch (entity) {
            case Geschaeftsart value -> value.getAstatus();
            case Prozessstatus value -> value.getAstatus();
            case Resultatstatus value -> value.getAstatus();
            case Beteiligungsrolle value -> value.getAstatus();
            case Unterlagentyp value -> value.getAstatus();
            case Aufgabentyp value -> value.getAstatus();
            default -> throw new IllegalArgumentException("Unbekannter Katalogeintrag: " + entity);
        };
    }

    private void setStatus(Object entity, String status) {
        switch (entity) {
            case Geschaeftsart value -> value.setAstatus(status);
            case Prozessstatus value -> value.setAstatus(status);
            case Resultatstatus value -> value.setAstatus(status);
            case Beteiligungsrolle value -> value.setAstatus(status);
            case Unterlagentyp value -> value.setAstatus(status);
            case Aufgabentyp value -> value.setAstatus(status);
            default -> throw new IllegalArgumentException("Unbekannter Katalogeintrag: " + entity);
        }
    }

    private CatalogEntryView toView(CatalogType type, Object entity) {
        return switch (entity) {
            case Geschaeftsart value -> new CatalogEntryView(type, value.getAcode(), value.getAname(),
                    value.getBeschreibung(), value.getAstatus(), null, null, false, false,
                    value.isResultaterforderlich());
            case Prozessstatus value -> new CatalogEntryView(type, value.getAcode(), value.getAname(),
                    value.getBeschreibung(), value.getAstatus(), value.getGeschaeftsart().getAcode(),
                    value.getSortierung(), value.isAinitial(), value.isTerminal(), false);
            case Resultatstatus value -> new CatalogEntryView(type, value.getAcode(), value.getAname(),
                    value.getBeschreibung(), value.getAstatus(), value.getGeschaeftsart().getAcode(),
                    value.getSortierung(), false, value.isTerminal(), false);
            case Beteiligungsrolle value -> commonView(type, value.getAcode(), value.getAname(),
                    value.getBeschreibung(), value.getAstatus());
            case Unterlagentyp value -> commonView(type, value.getAcode(), value.getAname(),
                    value.getBeschreibung(), value.getAstatus());
            case Aufgabentyp value -> commonView(type, value.getAcode(), value.getAname(),
                    value.getBeschreibung(), value.getAstatus());
            default -> throw new IllegalArgumentException("Unbekannter Katalogeintrag: " + entity);
        };
    }

    private CatalogEntryView commonView(CatalogType type, String code, String name, String description, String status) {
        return new CatalogEntryView(type, code, name, description, status, null, null, false, false, false);
    }
}
