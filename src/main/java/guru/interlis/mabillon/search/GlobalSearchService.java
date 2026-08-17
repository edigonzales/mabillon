package guru.interlis.mabillon.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Beteiligter;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Fachsystemreferenz;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
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
        List<Geschaeft> businesses = dossier.getGeschaefts();
        List<String> businessNumbers = businesses.stream().map(Geschaeft::getGeschaeftsnummer).toList();
        List<String> businessTypes = businesses.stream()
                .map(Geschaeft::getGeschaeftsart)
                .filter(Objects::nonNull)
                .map(type -> type.getAcode())
                .toList();
        List<String> processStatuses = businesses.stream()
                .map(Geschaeft::getProzessstatus)
                .filter(Objects::nonNull)
                .map(status -> status.getAcode())
                .toList();
        List<String> processStatusNames = businesses.stream()
                .map(Geschaeft::getProzessstatus)
                .filter(Objects::nonNull)
                .map(status -> status.getAname())
                .toList();
        List<String> documentTitles = dossier.getUnterlages().stream().map(Unterlage::getTitel).toList();
        List<String> partyNames = businesses.stream()
                .flatMap(business -> business.getBeteiligungs().stream())
                .map(participation -> participation.getBeteiligter())
                .filter(Objects::nonNull)
                .flatMap(party -> strings(party.getAname(), party.getVorname()).stream())
                .toList();
        List<String> organisations = businesses.stream()
                .flatMap(business -> business.getBeteiligungs().stream())
                .map(participation -> participation.getBeteiligter())
                .filter(Objects::nonNull)
                .map(Beteiligter::getOrganisation)
                .filter(Objects::nonNull)
                .toList();
        List<String> referenceIds = dossier.getFachsystemreferenzes().stream()
                .flatMap(reference -> strings(reference.getObjektid(), reference.getMutationid()).stream())
                .toList();
        List<String> freeText = concat(
                strings(dossier.getDossiernummer(), dossier.getTitel(), dossier.getBeschreibung()),
                businessNumbers,
                businesses.stream().flatMap(business -> strings(
                        business.getTitel(),
                        business.getKurzbeschreibung(),
                        business.getGeschaeftsart() == null ? null : business.getGeschaeftsart().getAname()).stream()).toList(),
                businessTypes,
                processStatuses,
                processStatusNames,
                documentTitles,
                partyNames,
                organisations,
                referenceIds);

        return containsAny(criteria.text(), freeText)
                && containsAny(criteria.geschaeftsnummer(), businessNumbers)
                && contains(criteria.dossiernummer(), dossier.getDossiernummer())
                && contains(criteria.titel(), dossier.getTitel())
                && containsAny(criteria.beteiligterName(), partyNames)
                && containsAny(criteria.organisation(), organisations)
                && containsAny(criteria.geschaeftsartCode(), businessTypes)
                && containsAny(criteria.processStatusCode(), processStatuses)
                && containsAny(criteria.unterlagentitel(), documentTitles)
                && containsAny(criteria.fachsystemId(), referenceIds);
    }

    private boolean matchesBusiness(Geschaeft business, GlobalSearchCriteria criteria) {
        List<String> documentTitles = business.getUnterlages().stream().map(Unterlage::getTitel).toList();
        List<String> referenceIds = business.getFachsystemreferenzes().stream()
                .flatMap(reference -> strings(reference.getObjektid(), reference.getMutationid()).stream())
                .toList();
        List<Beteiligter> parties = business.getBeteiligungs().stream()
                .map(participation -> participation.getBeteiligter())
                .filter(Objects::nonNull)
                .toList();
        List<String> partyNames = parties.stream()
                .flatMap(party -> strings(party.getAname(), party.getVorname()).stream())
                .toList();
        List<String> organisations = parties.stream()
                .map(Beteiligter::getOrganisation)
                .filter(Objects::nonNull)
                .toList();
        String dossierNumber = business.getDossier() == null ? null : business.getDossier().getDossiernummer();
        String businessType = business.getGeschaeftsart() == null ? null : business.getGeschaeftsart().getAcode();
        String businessTypeName = business.getGeschaeftsart() == null ? null : business.getGeschaeftsart().getAname();
        String processStatus = business.getProzessstatus() == null ? null : business.getProzessstatus().getAcode();
        String processStatusName = business.getProzessstatus() == null ? null : business.getProzessstatus().getAname();
        List<String> freeText = concat(
                strings(business.getGeschaeftsnummer(), business.getTitel(), business.getKurzbeschreibung(), dossierNumber,
                        businessType, businessTypeName, processStatus, processStatusName),
                documentTitles, referenceIds, partyNames, organisations);

        return containsAny(criteria.text(), freeText)
                && contains(criteria.geschaeftsnummer(), business.getGeschaeftsnummer())
                && contains(criteria.dossiernummer(), dossierNumber)
                && contains(criteria.titel(), business.getTitel())
                && containsAny(criteria.beteiligterName(), partyNames)
                && containsAny(criteria.organisation(), organisations)
                && contains(criteria.geschaeftsartCode(), businessType)
                && contains(criteria.processStatusCode(), processStatus)
                && containsAny(criteria.unterlagentitel(), documentTitles)
                && containsAny(criteria.fachsystemId(), referenceIds);
    }

    private boolean matchesParty(Beteiligter party, GlobalSearchCriteria criteria) {
        List<String> partyNames = strings(party.getAname(), party.getVorname(), fullName(party));
        List<String> organisations = strings(party.getOrganisation());
        List<String> freeText = strings(
                party.getTIliTid() == null ? null : party.getTIliTid().toString(),
                party.getTyp(),
                party.getAname(),
                party.getVorname(),
                fullName(party),
                party.getOrganisation(),
                party.getEmail(),
                party.getTelefon(),
                party.getAdresse(),
                party.getExternereferenz());

        return containsAny(criteria.text(), freeText)
                && absent(criteria.geschaeftsnummer())
                && absent(criteria.dossiernummer())
                && absent(criteria.titel())
                && containsAny(criteria.beteiligterName(), partyNames)
                && containsAny(criteria.organisation(), organisations)
                && absent(criteria.geschaeftsartCode())
                && absent(criteria.processStatusCode())
                && absent(criteria.unterlagentitel())
                && absent(criteria.fachsystemId());
    }

    private boolean matchesDocument(Unterlage document, GlobalSearchCriteria criteria) {
        Geschaeft business = document.getGeschaeft();
        String businessNumber = business == null ? null : business.getGeschaeftsnummer();
        String dossierNumber = document.getDossier() == null ? null : document.getDossier().getDossiernummer();
        List<String> freeText = strings(
                document.getTIliTid() == null ? null : document.getTIliTid().toString(),
                document.getTitel(),
                document.getBemerkungen(),
                document.getDateiname(),
                document.getMimetype(),
                businessNumber,
                dossierNumber);

        return containsAny(criteria.text(), freeText)
                && contains(criteria.geschaeftsnummer(), businessNumber)
                && contains(criteria.dossiernummer(), dossierNumber)
                && contains(criteria.titel(), document.getTitel())
                && absent(criteria.beteiligterName())
                && absent(criteria.organisation())
                && absent(criteria.geschaeftsartCode())
                && absent(criteria.processStatusCode())
                && contains(criteria.unterlagentitel(), document.getTitel())
                && absent(criteria.fachsystemId());
    }

    private boolean matchesReference(Fachsystemreferenz reference, GlobalSearchCriteria criteria) {
        if (reference.getGeschaeft() == null && reference.getDossier() == null) {
            return false;
        }
        String businessNumber = reference.getGeschaeft() == null ? null : reference.getGeschaeft().getGeschaeftsnummer();
        String dossierNumber = reference.getDossier() != null
                ? reference.getDossier().getDossiernummer()
                : reference.getGeschaeft().getDossier() == null
                        ? null
                        : reference.getGeschaeft().getDossier().getDossiernummer();
        List<String> referenceIds = strings(reference.getObjektid(), reference.getMutationid());
        List<String> freeText = strings(
                reference.getTIliTid() == null ? null : reference.getTIliTid().toString(),
                reference.getSystemcode(),
                reference.getObjekttyp(),
                reference.getObjektid(),
                reference.getMutationid(),
                reference.getBeschreibung(),
                businessNumber,
                dossierNumber);

        return containsAny(criteria.text(), freeText)
                && contains(criteria.geschaeftsnummer(), businessNumber)
                && contains(criteria.dossiernummer(), dossierNumber)
                && absent(criteria.titel())
                && absent(criteria.beteiligterName())
                && absent(criteria.organisation())
                && absent(criteria.geschaeftsartCode())
                && absent(criteria.processStatusCode())
                && absent(criteria.unterlagentitel())
                && containsAny(criteria.fachsystemId(), referenceIds);
    }

    private static boolean containsAny(String filter, List<String> values) {
        return filter == null || values.stream().anyMatch(value -> contains(filter, value));
    }

    private static boolean contains(String filter, String value) {
        return filter == null || value != null
                && value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private static boolean absent(String filter) {
        return filter == null;
    }

    private static String fullName(Beteiligter party) {
        if (party.getVorname() == null || party.getVorname().isBlank()) {
            return party.getAname();
        }
        return party.getVorname() + " " + party.getAname();
    }

    private static List<String> strings(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).toList();
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        List<String> values = new ArrayList<>();
        for (List<String> list : lists) {
            values.addAll(list);
        }
        return values;
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
}
