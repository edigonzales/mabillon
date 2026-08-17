package guru.interlis.mabillon.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Beteiligter;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Fachsystemreferenz;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
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
            List<GlobalSearchHit> hits = new ArrayList<>();
            ObjectSelect.query(Dossier.class).select(context).stream()
                    .filter(dossier -> matchesDossier(dossier, criteria))
                    .map(this::dossierHit)
                    .forEach(hits::add);
            ObjectSelect.query(Geschaeft.class).select(context).stream()
                    .filter(business -> matchesBusiness(business, criteria))
                    .map(this::businessHit)
                    .forEach(hits::add);
            ObjectSelect.query(Beteiligter.class).select(context).stream()
                    .filter(party -> matchesParty(party, criteria))
                    .map(this::partyHit)
                    .forEach(hits::add);
            ObjectSelect.query(Unterlage.class).select(context).stream()
                    .filter(document -> matchesDocument(document, criteria))
                    .map(this::documentHit)
                    .forEach(hits::add);
            ObjectSelect.query(Fachsystemreferenz.class).select(context).stream()
                    .filter(reference -> matchesReference(reference, criteria))
                    .map(this::referenceHit)
                    .forEach(hits::add);
            hits.sort(Comparator.comparing(GlobalSearchHit::objectType)
                    .thenComparing(GlobalSearchHit::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(GlobalSearchHit::identifier, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            int from = Math.min(criteria.page() * criteria.size(), hits.size());
            int to = Math.min(from + criteria.size(), hits.size());
            return new GlobalSearchResult(hits.subList(from, to), criteria.page(), criteria.size(), hits.size());
        });
    }

    private boolean matchesDossier(Dossier dossier, GlobalSearchCriteria criteria) {
        return matchesCommon(criteria, dossier.getDossiernummer(), dossier.getTitel(), dossier.getBeschreibung(),
                dossier.getDossiernummer(), null, null, null, null,
                () -> dossier.getGeschaefts().stream().flatMap(business -> business.getUnterlages().stream())
                        .map(Unterlage::getTitel).toList(),
                () -> dossier.getFachsystemreferenzes().stream().flatMap(reference -> java.util.stream.Stream.of(
                        reference.getObjektid(), reference.getMutationid())).toList(),
                () -> dossier.getGeschaefts().stream().flatMap(business -> business.getBeteiligungs().stream())
                        .map(beteiligung -> beteiligung.getBeteiligter().getAname()).toList(),
                () -> dossier.getGeschaefts().stream().flatMap(business -> business.getBeteiligungs().stream())
                        .map(beteiligung -> beteiligung.getBeteiligter().getOrganisation()).toList());
    }

    private boolean matchesBusiness(Geschaeft business, GlobalSearchCriteria criteria) {
        return matchesCommon(criteria, business.getGeschaeftsnummer(), business.getTitel(), business.getKurzbeschreibung(),
                business.getGeschaeftsnummer(), business.getDossier() == null ? null : business.getDossier().getDossiernummer(),
                business.getGeschaeftsart() == null ? null : business.getGeschaeftsart().getAcode(),
                business.getProzessstatus() == null ? null : business.getProzessstatus().getAcode(),
                business.getProzessstatus() == null ? null : business.getProzessstatus().getAname(),
                () -> business.getUnterlages().stream().map(Unterlage::getTitel).toList(),
                () -> business.getFachsystemreferenzes().stream().flatMap(reference -> java.util.stream.Stream.of(
                        reference.getObjektid(), reference.getMutationid())).toList(),
                () -> business.getBeteiligungs().stream().map(item -> item.getBeteiligter().getAname()).toList(),
                () -> business.getBeteiligungs().stream().map(item -> item.getBeteiligter().getOrganisation()).toList());
    }

    private boolean matchesParty(Beteiligter party, GlobalSearchCriteria criteria) {
        return matchesCommon(criteria, party.getTIliTid().toString(), party.getAname(), party.getOrganisation(),
                null, null, null, null, null,
                () -> List.of(), () -> List.of(), () -> List.of(party.getAname()),
                () -> party.getOrganisation() == null ? List.of() : List.of(party.getOrganisation()));
    }

    private boolean matchesDocument(Unterlage document, GlobalSearchCriteria criteria) {
        return matchesCommon(criteria, document.getTIliTid().toString(), document.getTitel(), document.getBemerkungen(),
                null, document.getDossier() == null ? null : document.getDossier().getDossiernummer(), null, null, null,
                () -> List.of(document.getTitel()), () -> List.of(), () -> List.of(), () -> List.of());
    }

    private boolean matchesReference(Fachsystemreferenz reference, GlobalSearchCriteria criteria) {
        return matchesCommon(criteria, reference.getTIliTid().toString(), reference.getObjektid(),
                reference.getBeschreibung(), null,
                reference.getDossier() == null ? null : reference.getDossier().getDossiernummer(), null, null,
                reference.getSystemcode(), () -> List.of(),
                () -> java.util.Arrays.asList(reference.getObjektid(), reference.getMutationid(), reference.getSystemcode()),
                () -> List.of(), () -> List.of());
    }

    private boolean matchesCommon(
            GlobalSearchCriteria criteria,
            String identifier,
            String title,
            String description,
            String businessNumber,
            String dossierNumber,
            String businessType,
            String processStatus,
            String processStatusName,
            java.util.function.Supplier<List<String>> documentTitles,
            java.util.function.Supplier<List<String>> referenceIds,
            java.util.function.Supplier<List<String>> partyNames,
            java.util.function.Supplier<List<String>> organisations) {
        List<String> searchable = new ArrayList<>(java.util.Arrays.asList(
                identifier, title, description, businessNumber, dossierNumber,
                businessType, processStatus, processStatusName));
        searchable.addAll(documentTitles.get());
        searchable.addAll(referenceIds.get());
        searchable.addAll(partyNames.get());
        searchable.addAll(organisations.get());
        return matches(criteria.text(), searchable)
                && contains(criteria.geschaeftsnummer(), businessNumber)
                && contains(criteria.dossiernummer(), dossierNumber)
                && containsIgnoreCase(criteria.titel(), title)
                && listContainsIgnoreCase(criteria.beteiligterName(), partyNames.get())
                && listContainsIgnoreCase(criteria.organisation(), organisations.get())
                && containsIgnoreCase(criteria.geschaeftsartCode(), businessType)
                && (criteria.processStatusCode() == null
                        || containsIgnoreCase(criteria.processStatusCode(), processStatus)
                        || containsIgnoreCase(criteria.processStatusCode(), processStatusName))
                && listContainsIgnoreCase(criteria.unterlagentitel(), documentTitles.get())
                && listContainsIgnoreCase(criteria.fachsystemId(), referenceIds.get());
    }

    private boolean matches(String query, List<String> fields) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return fields.stream().filter(java.util.Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(value -> value.contains(normalized));
    }

    private boolean contains(String filter, String value) {
        return filter == null || value != null && value.contains(filter);
    }

    private boolean containsIgnoreCase(String filter, String value) {
        return filter == null || value != null && value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private boolean listContainsIgnoreCase(String filter, List<String> values) {
        return filter == null || values.stream().filter(java.util.Objects::nonNull)
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)));
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
        return new GlobalSearchHit("FachsystemReferenz", reference.getTIliTid(), reference.getObjektid(),
                reference.getSystemcode() + ": " + reference.getObjekttyp(), reference.getObjektid(),
                reference.getGeschaeft() == null
                        ? "/dossiers/" + reference.getDossier().getDossiernummer()
                        : "/geschaefte/" + reference.getGeschaeft().getGeschaeftsnummer());
    }
}
