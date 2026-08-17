package guru.interlis.mabillon.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Beteiligter;
import guru.interlis.mabillon.persistence.cayenne.Beteiligung;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Fachsystemreferenz;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Geschaeftsart;
import guru.interlis.mabillon.persistence.cayenne.Prozessstatus;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.springframework.stereotype.Service;

@Service
public final class GlobalSearchService {

    private final CayenneUnitOfWork unitOfWork;

    public GlobalSearchService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public GlobalSearchResult search(GlobalSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        return unitOfWork.read(context -> {
            ObjectSelect<Beteiligter> parties = partyQuery(criteria);
            ObjectSelect<Dossier> dossiers = dossierQuery(criteria);
            ObjectSelect<Fachsystemreferenz> references = referenceQuery(criteria);
            ObjectSelect<Geschaeft> businesses = businessQuery(criteria);
            ObjectSelect<Unterlage> documents = documentQuery(criteria);

            long partyCount = count(context, parties);
            long dossierCount = count(context, dossiers);
            long referenceCount = count(context, references);
            long businessCount = count(context, businesses);
            long documentCount = count(context, documents);
            long total = partyCount + dossierCount + referenceCount + businessCount + documentCount;

            long offset = (long) criteria.page() * criteria.size();
            if (offset >= total) {
                return new GlobalSearchResult(List.of(), criteria.page(), criteria.size(), total);
            }

            List<GlobalSearchHit> hits = new ArrayList<>(criteria.size());
            PageCursor cursor = new PageCursor(offset, criteria.size());
            append(context, parties, partyCount, cursor, hits, this::partyHit,
                    Beteiligter.ANAME.ascInsensitive(), Beteiligter.T_ILI_TID.asc());
            append(context, dossiers, dossierCount, cursor, hits, this::dossierHit,
                    Dossier.TITEL.ascInsensitive(), Dossier.DOSSIERNUMMER.ascInsensitive());
            append(context, references, referenceCount, cursor, hits, this::referenceHit,
                    Fachsystemreferenz.SYSTEMCODE.ascInsensitive(), Fachsystemreferenz.OBJEKTTYP.ascInsensitive(),
                    Fachsystemreferenz.OBJEKTID.ascInsensitive());
            append(context, businesses, businessCount, cursor, hits, this::businessHit,
                    Geschaeft.TITEL.ascInsensitive(), Geschaeft.GESCHAEFTSNUMMER.ascInsensitive());
            append(context, documents, documentCount, cursor, hits, this::documentHit,
                    Unterlage.TITEL.ascInsensitive(), Unterlage.T_ILI_TID.asc());

            return new GlobalSearchResult(hits, criteria.page(), criteria.size(), total);
        });
    }

    private static ObjectSelect<Dossier> dossierQuery(GlobalSearchCriteria criteria) {
        ObjectSelect<Dossier> query = ObjectSelect.query(Dossier.class);
        addFilter(query, textFilterForDossier(criteria.text()));
        addFilter(query, dossierHasBusinessField(Geschaeft.GESCHAEFTSNUMMER, criteria.geschaeftsnummer()));
        addFilter(query, ci(Dossier.DOSSIERNUMMER, criteria.dossiernummer()));
        addFilter(query, ci(Dossier.TITEL, criteria.titel()));
        addFilter(query, dossierHasParticipantName(criteria.beteiligterName()));
        addFilter(query, dossierHasParticipantOrganisation(criteria.organisation()));
        addFilter(query, dossierHasBusinessField(
                Geschaeft.GESCHAEFTSART.dot(Geschaeftsart.ACODE), criteria.geschaeftsartCode()));
        addFilter(query, dossierHasBusinessField(
                Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ACODE), criteria.processStatusCode()));
        addFilter(query, dossierHasDocumentTitle(criteria.unterlagentitel()));
        addFilter(query, dossierHasReferenceId(criteria.fachsystemId()));
        return query;
    }

    private static ObjectSelect<Geschaeft> businessQuery(GlobalSearchCriteria criteria) {
        ObjectSelect<Geschaeft> query = ObjectSelect.query(Geschaeft.class);
        addFilter(query, textFilterForBusiness(criteria.text()));
        addFilter(query, ci(Geschaeft.GESCHAEFTSNUMMER, criteria.geschaeftsnummer()));
        addFilter(query, criteria.dossiernummer() == null ? null
                : Geschaeft.DOSSIER.dot(Dossier.DOSSIERNUMMER).containsIgnoreCase(criteria.dossiernummer()));
        addFilter(query, ci(Geschaeft.TITEL, criteria.titel()));
        addFilter(query, businessHasParticipantName(criteria.beteiligterName()));
        addFilter(query, businessHasParticipantOrganisation(criteria.organisation()));
        addFilter(query, criteria.geschaeftsartCode() == null ? null
                : Geschaeft.GESCHAEFTSART.dot(Geschaeftsart.ACODE).containsIgnoreCase(criteria.geschaeftsartCode()));
        addFilter(query, criteria.processStatusCode() == null ? null
                : Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ACODE).containsIgnoreCase(criteria.processStatusCode()));
        addFilter(query, businessHasDocumentTitle(criteria.unterlagentitel()));
        addFilter(query, businessHasReferenceId(criteria.fachsystemId()));
        return query;
    }

    private static ObjectSelect<Beteiligter> partyQuery(GlobalSearchCriteria criteria) {
        if (criteria.geschaeftsnummer() != null || criteria.dossiernummer() != null || criteria.titel() != null
                || criteria.geschaeftsartCode() != null || criteria.processStatusCode() != null
                || criteria.unterlagentitel() != null || criteria.fachsystemId() != null) {
            return null;
        }
        ObjectSelect<Beteiligter> query = ObjectSelect.query(Beteiligter.class);
        addFilter(query, textFilterForParty(criteria.text()));
        addFilter(query, rootPartyNameExpression(criteria.beteiligterName()));
        addFilter(query, ci(Beteiligter.ORGANISATION, criteria.organisation()));
        return query;
    }

    private static ObjectSelect<Unterlage> documentQuery(GlobalSearchCriteria criteria) {
        if (criteria.beteiligterName() != null || criteria.organisation() != null
                || criteria.geschaeftsartCode() != null || criteria.processStatusCode() != null
                || criteria.fachsystemId() != null) {
            return null;
        }
        ObjectSelect<Unterlage> query = ObjectSelect.query(Unterlage.class);
        addFilter(query, textFilterForDocument(criteria.text()));
        addFilter(query, criteria.geschaeftsnummer() == null ? null
                : Unterlage.GESCHAEFT.dot(Geschaeft.GESCHAEFTSNUMMER).containsIgnoreCase(criteria.geschaeftsnummer()));
        addFilter(query, criteria.dossiernummer() == null ? null
                : Unterlage.DOSSIER.dot(Dossier.DOSSIERNUMMER).containsIgnoreCase(criteria.dossiernummer()));
        addFilter(query, ci(Unterlage.TITEL, criteria.titel()));
        addFilter(query, ci(Unterlage.TITEL, criteria.unterlagentitel()));
        return query;
    }

    private static ObjectSelect<Fachsystemreferenz> referenceQuery(GlobalSearchCriteria criteria) {
        if (criteria.titel() != null || criteria.beteiligterName() != null || criteria.organisation() != null
                || criteria.geschaeftsartCode() != null || criteria.processStatusCode() != null
                || criteria.unterlagentitel() != null) {
            return null;
        }
        ObjectSelect<Fachsystemreferenz> query = ObjectSelect.query(Fachsystemreferenz.class)
                .where(ExpressionFactory.or(
                        Fachsystemreferenz.GESCHAEFT.isNotNull(),
                        Fachsystemreferenz.DOSSIER.isNotNull()));
        addFilter(query, textFilterForReference(criteria.text()));
        addFilter(query, criteria.geschaeftsnummer() == null ? null
                : Fachsystemreferenz.GESCHAEFT.dot(Geschaeft.GESCHAEFTSNUMMER)
                        .containsIgnoreCase(criteria.geschaeftsnummer()));
        addFilter(query, referenceDossierNumber(criteria.dossiernummer()));
        addFilter(query, referenceIdExpression(criteria.fachsystemId()));
        return query;
    }

    private static Expression textFilterForDossier(String text) {
        if (text == null) {
            return null;
        }
        return or(
                Dossier.DOSSIERNUMMER.containsIgnoreCase(text),
                Dossier.TITEL.containsIgnoreCase(text),
                dossierHasBusinessField(Geschaeft.GESCHAEFTSNUMMER, text),
                dossierHasBusinessField(Geschaeft.TITEL, text),
                dossierHasBusinessField(Geschaeft.GESCHAEFTSART.dot(Geschaeftsart.ACODE), text),
                dossierHasBusinessField(Geschaeft.GESCHAEFTSART.dot(Geschaeftsart.ANAME), text),
                dossierHasBusinessField(Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ACODE), text),
                dossierHasBusinessField(Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ANAME), text),
                dossierHasDocumentTitle(text),
                dossierHasParticipantName(text),
                dossierHasParticipantOrganisation(text),
                dossierHasReferenceId(text));
    }

    private static Expression textFilterForBusiness(String text) {
        if (text == null) {
            return null;
        }
        return or(
                Geschaeft.GESCHAEFTSNUMMER.containsIgnoreCase(text),
                Geschaeft.TITEL.containsIgnoreCase(text),
                Geschaeft.DOSSIER.dot(Dossier.DOSSIERNUMMER).containsIgnoreCase(text),
                Geschaeft.GESCHAEFTSART.dot(Geschaeftsart.ACODE).containsIgnoreCase(text),
                Geschaeft.GESCHAEFTSART.dot(Geschaeftsart.ANAME).containsIgnoreCase(text),
                Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ACODE).containsIgnoreCase(text),
                Geschaeft.PROZESSSTATUS.dot(Prozessstatus.ANAME).containsIgnoreCase(text),
                businessHasDocumentTitle(text),
                businessHasReferenceId(text),
                businessHasParticipantName(text),
                businessHasParticipantOrganisation(text));
    }

    private static Expression textFilterForParty(String text) {
        if (text == null) {
            return null;
        }
        List<Expression> expressions = new ArrayList<>();
        expressions.add(Beteiligter.TYP.containsIgnoreCase(text));
        expressions.add(rootPartyNameExpression(text));
        expressions.add(Beteiligter.ORGANISATION.containsIgnoreCase(text));
        expressions.add(Beteiligter.EMAIL.containsIgnoreCase(text));
        expressions.add(Beteiligter.TELEFON.containsIgnoreCase(text));
        expressions.add(Beteiligter.EXTERNEREFERENZ.containsIgnoreCase(text));
        addUuidExpression(expressions, Beteiligter.T_ILI_TID, text);
        return or(expressions.toArray(Expression[]::new));
    }

    private static Expression textFilterForDocument(String text) {
        if (text == null) {
            return null;
        }
        List<Expression> expressions = new ArrayList<>(List.of(
                Unterlage.TITEL.containsIgnoreCase(text),
                Unterlage.DATEINAME.containsIgnoreCase(text),
                Unterlage.MIMETYPE.containsIgnoreCase(text),
                Unterlage.GESCHAEFT.dot(Geschaeft.GESCHAEFTSNUMMER).containsIgnoreCase(text),
                Unterlage.DOSSIER.dot(Dossier.DOSSIERNUMMER).containsIgnoreCase(text)));
        addUuidExpression(expressions, Unterlage.T_ILI_TID, text);
        return or(expressions.toArray(Expression[]::new));
    }

    private static Expression textFilterForReference(String text) {
        if (text == null) {
            return null;
        }
        List<Expression> expressions = new ArrayList<>(List.of(
                Fachsystemreferenz.SYSTEMCODE.containsIgnoreCase(text),
                Fachsystemreferenz.OBJEKTTYP.containsIgnoreCase(text),
                Fachsystemreferenz.OBJEKTID.containsIgnoreCase(text),
                Fachsystemreferenz.MUTATIONID.containsIgnoreCase(text),
                Fachsystemreferenz.GESCHAEFT.dot(Geschaeft.GESCHAEFTSNUMMER).containsIgnoreCase(text),
                Fachsystemreferenz.DOSSIER.dot(Dossier.DOSSIERNUMMER).containsIgnoreCase(text),
                Fachsystemreferenz.GESCHAEFT.dot(Geschaeft.DOSSIER).dot(Dossier.DOSSIERNUMMER)
                        .containsIgnoreCase(text)));
        addUuidExpression(expressions, Fachsystemreferenz.T_ILI_TID, text);
        return or(expressions.toArray(Expression[]::new));
    }

    private static Expression dossierHasBusinessField(StringProperty<String> property, String filter) {
        if (filter == null) {
            return null;
        }
        var dossierIds = ObjectSelect.columnQuery(
                        Geschaeft.class,
                        Geschaeft.DOSSIER.dot(Dossier.T_ID_PK_PROPERTY))
                .where(property.containsIgnoreCase(filter));
        return Dossier.T_ID_PK_PROPERTY.in(dossierIds);
    }

    private static Expression dossierHasDocumentTitle(String filter) {
        if (filter == null) {
            return null;
        }
        var dossierIds = ObjectSelect.columnQuery(
                        Unterlage.class,
                        Unterlage.DOSSIER.dot(Dossier.T_ID_PK_PROPERTY))
                .where(Unterlage.TITEL.containsIgnoreCase(filter));
        return Dossier.T_ID_PK_PROPERTY.in(dossierIds);
    }

    private static Expression businessHasDocumentTitle(String filter) {
        if (filter == null) {
            return null;
        }
        var businessIds = ObjectSelect.columnQuery(
                        Unterlage.class,
                        Unterlage.GESCHAEFT.dot(Geschaeft.T_ID_PK_PROPERTY))
                .where(Unterlage.TITEL.containsIgnoreCase(filter));
        return Geschaeft.T_ID_PK_PROPERTY.in(businessIds);
    }

    private static Expression dossierHasReferenceId(String filter) {
        if (filter == null) {
            return null;
        }
        var dossierIds = ObjectSelect.columnQuery(
                        Fachsystemreferenz.class,
                        Fachsystemreferenz.DOSSIER.dot(Dossier.T_ID_PK_PROPERTY))
                .where(referenceIdExpression(filter));
        return Dossier.T_ID_PK_PROPERTY.in(dossierIds);
    }

    private static Expression businessHasReferenceId(String filter) {
        if (filter == null) {
            return null;
        }
        var businessIds = ObjectSelect.columnQuery(
                        Fachsystemreferenz.class,
                        Fachsystemreferenz.GESCHAEFT.dot(Geschaeft.T_ID_PK_PROPERTY))
                .where(referenceIdExpression(filter));
        return Geschaeft.T_ID_PK_PROPERTY.in(businessIds);
    }

    private static Expression dossierHasParticipantName(String filter) {
        if (filter == null) {
            return null;
        }
        var dossierIds = ObjectSelect.columnQuery(
                        Beteiligung.class,
                        Beteiligung.GESCHAEFT.dot(Geschaeft.DOSSIER).dot(Dossier.T_ID_PK_PROPERTY))
                .where(participationPartyNameExpression(filter));
        return Dossier.T_ID_PK_PROPERTY.in(dossierIds);
    }

    private static Expression businessHasParticipantName(String filter) {
        if (filter == null) {
            return null;
        }
        var businessIds = ObjectSelect.columnQuery(
                        Beteiligung.class,
                        Beteiligung.GESCHAEFT.dot(Geschaeft.T_ID_PK_PROPERTY))
                .where(participationPartyNameExpression(filter));
        return Geschaeft.T_ID_PK_PROPERTY.in(businessIds);
    }

    private static Expression dossierHasParticipantOrganisation(String filter) {
        if (filter == null) {
            return null;
        }
        var dossierIds = ObjectSelect.columnQuery(
                        Beteiligung.class,
                        Beteiligung.GESCHAEFT.dot(Geschaeft.DOSSIER).dot(Dossier.T_ID_PK_PROPERTY))
                .where(Beteiligung.BETEILIGTER.dot(Beteiligter.ORGANISATION).containsIgnoreCase(filter));
        return Dossier.T_ID_PK_PROPERTY.in(dossierIds);
    }

    private static Expression businessHasParticipantOrganisation(String filter) {
        if (filter == null) {
            return null;
        }
        var businessIds = ObjectSelect.columnQuery(
                        Beteiligung.class,
                        Beteiligung.GESCHAEFT.dot(Geschaeft.T_ID_PK_PROPERTY))
                .where(Beteiligung.BETEILIGTER.dot(Beteiligter.ORGANISATION).containsIgnoreCase(filter));
        return Geschaeft.T_ID_PK_PROPERTY.in(businessIds);
    }

    private static Expression participationPartyNameExpression(String filter) {
        return filter == null ? null : or(
                Beteiligung.BETEILIGTER.dot(Beteiligter.ANAME).containsIgnoreCase(filter),
                Beteiligung.BETEILIGTER.dot(Beteiligter.VORNAME).containsIgnoreCase(filter));
    }

    private static Expression rootPartyNameExpression(String filter) {
        if (filter == null) {
            return null;
        }
        List<Expression> alternatives = new ArrayList<>();
        alternatives.add(Beteiligter.ANAME.containsIgnoreCase(filter));
        alternatives.add(Beteiligter.VORNAME.containsIgnoreCase(filter));
        String normalized = normalizeSpaces(filter);
        int separator = normalized.indexOf(' ');
        if (separator > 0 && separator < normalized.length() - 1) {
            String firstName = normalized.substring(0, separator);
            String lastName = normalized.substring(separator + 1);
            alternatives.add(ExpressionFactory.and(
                    Beteiligter.VORNAME.containsIgnoreCase(firstName),
                    Beteiligter.ANAME.containsIgnoreCase(lastName)));
        }
        return or(alternatives.toArray(Expression[]::new));
    }

    private static String normalizeSpaces(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private static Expression referenceDossierNumber(String filter) {
        if (filter == null) {
            return null;
        }
        return or(
                Fachsystemreferenz.DOSSIER.dot(Dossier.DOSSIERNUMMER).containsIgnoreCase(filter),
                Fachsystemreferenz.GESCHAEFT.dot(Geschaeft.DOSSIER).dot(Dossier.DOSSIERNUMMER)
                        .containsIgnoreCase(filter));
    }

    private static Expression referenceIdExpression(String filter) {
        return filter == null ? null : or(
                Fachsystemreferenz.OBJEKTID.containsIgnoreCase(filter),
                Fachsystemreferenz.MUTATIONID.containsIgnoreCase(filter));
    }

    private static Expression ci(StringProperty<String> property, String filter) {
        return filter == null ? null : property.containsIgnoreCase(filter);
    }

    private static Expression or(Expression... expressions) {
        return ExpressionFactory.or(expressions);
    }

    private static <T> void addFilter(ObjectSelect<T> query, Expression expression) {
        if (query == null || expression == null) {
            return;
        }
        if (query.getWhere() == null) {
            query.where(expression);
        } else {
            query.and(expression);
        }
    }

    private static void addUuidExpression(List<Expression> expressions, BaseProperty<UUID> property, String text) {
        try {
            expressions.add(property.eq(UUID.fromString(text)));
        } catch (IllegalArgumentException ignored) {
            // UUID is an additional exact free-text key; textual fields keep contains semantics.
        }
    }

    private static long count(ObjectContext context, ObjectSelect<?> query) {
        return query == null ? 0 : query.selectCount(context);
    }

    private static <T> void append(
            ObjectContext context,
            ObjectSelect<T> query,
            long count,
            PageCursor cursor,
            List<GlobalSearchHit> target,
            Function<T, GlobalSearchHit> mapper,
            Ordering... orderings) {
        if (query == null || count == 0 || cursor.remaining == 0) {
            return;
        }
        if (cursor.offset >= count) {
            cursor.offset -= count;
            return;
        }
        int take = (int) Math.min(cursor.remaining, count - cursor.offset);
        for (Ordering ordering : orderings) {
            query.orderBy(ordering);
        }
        List<T> values = query.offset(Math.toIntExact(cursor.offset)).limit(take).select(context);
        values.stream().map(mapper).forEach(target::add);
        cursor.remaining -= values.size();
        cursor.offset = 0;
    }

    private GlobalSearchHit dossierHit(Dossier dossier) {
        return new GlobalSearchHit("Dossier", dossier.getTIliTid(), dossier.getDossiernummer(), dossier.getTitel(),
                dossier.getAstatus(), "/dossiers/" + dossier.getDossiernummer());
    }

    private GlobalSearchHit businessHit(Geschaeft business) {
        return new GlobalSearchHit("Geschaeft", business.getTIliTid(), business.getGeschaeftsnummer(), business.getTitel(),
                business.getLifecyclestatus(), "/geschaefte/" + business.getGeschaeftsnummer());
    }

    private GlobalSearchHit partyHit(Beteiligter party) {
        return new GlobalSearchHit("Beteiligter", party.getTIliTid(), party.getTIliTid().toString(), party.getAname(),
                party.getOrganisation(), "/beteiligte/" + party.getTIliTid());
    }

    private GlobalSearchHit documentHit(Unterlage document) {
        return new GlobalSearchHit("Unterlage", document.getTIliTid(), document.getTIliTid().toString(),
                document.getTitel(), document.getDossier() == null ? null : document.getDossier().getDossiernummer(),
                "/unterlagen/" + document.getTIliTid());
    }

    private GlobalSearchHit referenceHit(Fachsystemreferenz reference) {
        String targetUrl = reference.getGeschaeft() != null
                ? "/geschaefte/" + reference.getGeschaeft().getGeschaeftsnummer()
                : "/dossiers/" + reference.getDossier().getDossiernummer();
        return new GlobalSearchHit("FachsystemReferenz", reference.getTIliTid(), reference.getObjektid(),
                reference.getSystemcode() + ": " + reference.getObjekttyp(), reference.getObjektid(), targetUrl);
    }

    private static final class PageCursor {
        private long offset;
        private int remaining;

        private PageCursor(long offset, int remaining) {
            this.offset = offset;
            this.remaining = remaining;
        }
    }
}
