package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import guru.interlis.mabillon.aufgabe.AufgabeView;
import guru.interlis.mabillon.aufgabe.CompleteAufgabeCommand;
import guru.interlis.mabillon.aufgabe.CreateAufgabeCommand;
import guru.interlis.mabillon.aufgabe.DelegateAufgabeCommand;
import guru.interlis.mabillon.aufgabe.UpdateAufgabeCommand;
import guru.interlis.mabillon.beteiligung.AddBeteiligungCommand;
import guru.interlis.mabillon.beteiligung.BeteiligterView;
import guru.interlis.mabillon.beteiligung.BeteiligungView;
import guru.interlis.mabillon.catalog.CatalogCreateCommand;
import guru.interlis.mabillon.catalog.CatalogType;
import guru.interlis.mabillon.dashboard.MyWorkView;
import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.geschaeft.ChangeProcessStatusCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.GeschaeftskontrolleCriteria;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.geschaeft.SetResultCommand;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class GeschaeftIntegrationTest extends MabillonIntegrationTestSupport {

    @Test
    void queryNomenklaturGeschaeft() {
        GeschaeftView geschaeft = geschaeftQueryService.findByNumber("AGI-G-2026-000421").orElseThrow();
        assertThat(geschaeft.title()).contains("Musterwil");
        assertThat(geschaeft.dossierNumber()).isEqualTo("AGI-D-2026-000007");
        assertThat(geschaeft.unterlagen()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void normalHttpAndHtmxUseTheSameReadOnlyUseCase() throws Exception {
        mockMvc.perform(get("/dossiers/AGI-D-2026-000007"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"mabillon-topbar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/suche\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"dossier-detail\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gemeinde Musterwil")));
        mockMvc.perform(get("/geschaefte/AGI-G-2026-000421").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"geschaeft-detail\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("class=\"mabillon-topbar\""))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void numberingIsUniqueUnderConcurrency() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = IntStream.range(0, 24)
                    .mapToObj(ignored -> executor.submit(() -> numberingService
                            .nextGeschaeftNumber("PHASE3", LocalDate.of(2026, 8, 16)).value()))
                    .toList();
            Set<String> numbers = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            }).collect(Collectors.toSet());
            assertThat(numbers).hasSize(24);
            assertThat(numbers).allMatch(number -> number.matches("PHASE3-G-2026-\\d{6}"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void openingBusinessAdvancesLifecycleAndJournalsAtomically() {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 3 Dossier", "Dossier für den Kern-Use-Case.", "4.3.3", "AGI-NOM", "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Phase 3 Geschäft", "Kern-Use-Case für Status und Journal.",
                "NOMENKLATURMUTATION", "AGI-NOM", "anna.mueller", LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 16), null, 1));

        assertThat(business.lifecycleStatus()).isEqualTo("Eroeffnet");
        assertThat(business.processStatusCode()).isEqualTo("ANTRAG_EINGEGANGEN");
        assertThat(journalQueryService.findForGeschaeft(GeschaeftNumber.parse(business.number()), 10)).hasSize(1);

        GeschaeftView changed = geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                GeschaeftNumber.parse(business.number()), "FORMELLE_PRUEFUNG", "Formelle Prüfung begonnen."));
        assertThat(changed.lifecycleStatus()).isEqualTo("In_Bearbeitung");
        assertThat(changed.processStatusCode()).isEqualTo("FORMELLE_PRUEFUNG");
        assertThat(journalQueryService.findForGeschaeft(GeschaeftNumber.parse(business.number()), 10))
                .hasSize(2).anyMatch(entry -> entry.typ().name().equals("Status_geaendert"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void processStatusEndpointSupportsNormalHttpAndHtmxFallback() throws Exception {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 3 HTTP Dossier", null, "4.3.3", "AGI-NOM", "anna.mueller", LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Phase 3 HTTP Geschäft", null, "NOMENKLATURMUTATION",
                "AGI-NOM", "anna.mueller", null, LocalDate.of(2026, 8, 16), null, null));

        mockMvc.perform(post("/geschaefte/{number}/prozessstatus", business.number())
                        .with(httpBasic("admin", "admin")).with(csrf())
                        .param("processStatusCode", "FORMELLE_PRUEFUNG").param("comment", "Normale HTTP-Anfrage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/geschaefte/" + business.number()));
        mockMvc.perform(post("/geschaefte/{number}/prozessstatus", business.number())
                        .with(httpBasic("admin", "admin")).with(csrf()).header("HX-Request", "true")
                        .param("processStatusCode", "FORMELLE_PRUEFUNG").param("comment", "HTMX-Anfrage"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"geschaeft-status-panel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FORMELLE_PRUEFUNG")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void processAndResultStatusesOfAnotherBusinessTypeAreRejectedWithoutJournal() {
        catalogService.create(new CatalogCreateCommand(CatalogType.PROZESSSTATUS, "ONLY_AUSKUNFT_PHASE3",
                "Nur Auskunft Prozessstatus", null, "NOMENKLATURAUSKUNFT", 999, false, false, false));
        catalogService.create(new CatalogCreateCommand(CatalogType.RESULTATSTATUS, "ONLY_AUSKUNFT_RESULT_PHASE3",
                "Nur Auskunft Resultat", null, "NOMENKLATURAUSKUNFT", 999, false, true, false));

        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 3 Validierungsdossier", null, "4.3.3", "AGI-NOM", "anna.mueller", LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Phase 3 Validierungsgeschäft", null,
                "NOMENKLATURMUTATION", "AGI-NOM", "anna.mueller", null, LocalDate.of(2026, 8, 16), null, null));
        GeschaeftNumber number = GeschaeftNumber.parse(business.number());
        int journalBefore = journalQueryService.findForGeschaeft(number, 10).size();

        assertThatThrownBy(() -> geschaeftService.changeProcessStatus(
                new ChangeProcessStatusCommand(number, "ONLY_AUSKUNFT_PHASE3", "falscher Typ")))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Geschäftsart");
        assertThatThrownBy(() -> geschaeftService.setResult(
                new SetResultCommand(number, "ONLY_AUSKUNFT_RESULT_PHASE3", "falscher Typ")))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Geschäftsart");
        assertThat(journalQueryService.findForGeschaeft(number, 10)).hasSize(journalBefore);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFourCreatesAndValidatesBeteiligung() {
        GeschaeftView business = newBusiness("Beteiligung");
        BeteiligterView party = beteiligterService.create(new guru.interlis.mabillon.beteiligung.CreateBeteiligterCommand(
                "Organisation", "Phase 4 Gemeinde", null, "Gemeindeverwaltung", null, null, null, "PHASE4-PARTY"));
        BeteiligungView value = beteiligungService.add(new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "ANTRAGSTELLERIN", null,
                LocalDate.of(2026, 8, 16), null, "Antrag eingereicht."));

        assertThat(value.beteiligterName()).isEqualTo("Phase 4 Gemeinde");
        assertThat(beteiligungService.listForGeschaeft(GeschaeftNumber.parse(business.number())))
                .anyMatch(item -> item.tid().equals(value.tid()));
        assertThat(journalQueryService.findForObject(EreignisObjektTyp.Beteiligung, value.tid().toString(), 10))
                .anyMatch(entry -> entry.typ().name().equals("Zugewiesen"));
        assertThatThrownBy(() -> beteiligungService.add(new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "NICHT_AKTIVE_ROLLE", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Beteiligungsrolle");
        assertThatThrownBy(() -> new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "ANTRAGSTELLERIN", null,
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFourCreatesStartsCompletesAndJournalsAufgabe() throws Exception {
        GeschaeftView business = newBusiness("Aufgabe");
        AufgabeView task = aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "Phase 4 Aufgabe", "Bitte prüfen.",
                "AUFGABE_FORMELLE_PRUEFUNG", LocalDate.of(2026, 8, 20), 3, "anna.mueller", null));
        assertThat(task.status()).isEqualTo("Offen");
        assertThat(task.assignedUsername()).isEqualTo("anna.mueller");
        assertThat(aufgabeQueryService.forGeschaeft(GeschaeftNumber.parse(business.number())))
                .extracting(AufgabeView::tid).contains(task.tid());

        AufgabeView updated = aufgabeService.update(new UpdateAufgabeCommand(
                task.tid(), "Phase 4 Aufgabe aktualisiert", "Neue Beschreibung.", LocalDate.of(2026, 8, 21), 4));
        assertThat(updated.title()).isEqualTo("Phase 4 Aufgabe aktualisiert");
        AufgabeView delegated = aufgabeService.delegate(new DelegateAufgabeCommand(task.tid(), null, "AGI-NOM"));
        assertThat(delegated.status()).isEqualTo("Delegiert");
        AufgabeView started = aufgabeService.start(task.tid());
        assertThat(started.status()).isEqualTo("In_Arbeit");
        AufgabeView completed = aufgabeService.complete(new CompleteAufgabeCommand(task.tid(), "Prüfung abgeschlossen."));
        assertThat(completed.status()).isEqualTo("Erledigt");
        assertThat(completed.completedAt()).isNotNull();
        assertThat(journalQueryService.findForObject(EreignisObjektTyp.Aufgabe, task.tid().toString(), 10))
                .extracting(entry -> entry.typ().name())
                .contains("Aufgabe_erstellt", "Status_geaendert", "Aufgabe_erledigt");

        assertThatThrownBy(() -> aufgabeService.update(
                new UpdateAufgabeCommand(task.tid(), "Nachträgliche Änderung", null, null, 1)))
                .isInstanceOf(DomainRuleViolationException.class).hasMessageContaining("abgeschlossen");
        assertThatThrownBy(() -> aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "Fehlerhafte Aufgabe", null,
                "NICHT_VORHANDEN", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Aufgabentyp");

        AufgabeView httpTask = aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "HTTP-Aufgabe", null,
                "RUECKFRAGE", null, null, "anna.mueller", null));
        mockMvc.perform(post("/aufgaben/{tid}/start", httpTask.tid())
                        .with(httpBasic("admin", "admin")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/geschaefte/" + business.number()));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFourMyWorkContainsOpenAndOverdueTasks() {
        GeschaeftView business = newBusiness("Meine Arbeit");
        AufgabeView task = aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "Überfällige Phase 4 Aufgabe", null,
                "RUECKFRAGE", LocalDate.of(2026, 8, 10), 2, "anna.mueller", null));
        MyWorkView work = myWorkQueryService.load("anna.mueller", LocalDate.of(2026, 8, 16));
        assertThat(work.activeBusinesses()).anyMatch(item -> item.number().equals(business.number()));
        assertThat(work.openTasks()).anyMatch(item -> item.tid().equals(task.tid()));
        assertThat(work.overdueTasks()).anyMatch(item -> item.tid().equals(task.tid()));
    }

    @Test
    void phaseFourControlViewProvidesOpenAndOverdueMetrics() {
        var control = geschaeftskontrolleQueryService.load(
                new GeschaeftskontrolleCriteria(LocalDate.of(2026, 8, 16), 50, 30));
        assertThat(control.offeneGeschaefte()).isNotEmpty();
        assertThat(control.offeneAufgaben()).isNotNull();
        assertThat(control.verteilungNachProzessstatus()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseSevenBusinessClosureRejectsOpenTasks() {
        GeschaeftView business = newBusiness("Abschluss offene Aufgabe");
        aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "Noch offene Aufgabe", null,
                "RUECKFRAGE", null, 1, "anna.mueller", null));
        assertThatThrownBy(() -> geschaeftService.close(GeschaeftNumber.parse(business.number())))
                .isInstanceOf(DomainRuleViolationException.class).hasMessageContaining("offene Aufgaben");
    }
}
