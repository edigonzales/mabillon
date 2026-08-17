package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.time.LocalDate;
import java.util.UUID;

import guru.interlis.mabillon.aufgabe.AufgabeQueryService;
import guru.interlis.mabillon.aufgabe.AufgabeService;
import guru.interlis.mabillon.aufgabe.AufgabeView;
import guru.interlis.mabillon.aufgabe.CompleteAufgabeCommand;
import guru.interlis.mabillon.aufgabe.CreateAufgabeCommand;
import guru.interlis.mabillon.aufgabe.DelegateAufgabeCommand;
import guru.interlis.mabillon.aufgabe.UpdateAufgabeCommand;
import guru.interlis.mabillon.beteiligung.AddBeteiligungCommand;
import guru.interlis.mabillon.beteiligung.BeteiligterService;
import guru.interlis.mabillon.beteiligung.BeteiligterView;
import guru.interlis.mabillon.beteiligung.BeteiligungService;
import guru.interlis.mabillon.beteiligung.BeteiligungView;
import guru.interlis.mabillon.catalog.CatalogCreateCommand;
import guru.interlis.mabillon.dashboard.MyWorkQueryService;
import guru.interlis.mabillon.dashboard.MyWorkView;
import guru.interlis.mabillon.domain.DomainRuleViolationException;
import guru.interlis.mabillon.dossier.DossierQueryService;
import guru.interlis.mabillon.dossier.DossierService;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.fachsystem.AddFachsystemReferenzCommand;
import guru.interlis.mabillon.fachsystem.AddFachsystemReferenzToDossierCommand;
import guru.interlis.mabillon.fachsystem.FachsystemReferenzService;
import guru.interlis.mabillon.fachsystem.FachsystemReferenzView;
import guru.interlis.mabillon.geschaeft.ChangeProcessStatusCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftQueryService;
import guru.interlis.mabillon.geschaeft.GeschaeftService;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.geschaeft.SetResultCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftskontrolleCriteria;
import guru.interlis.mabillon.geschaeft.GeschaeftskontrolleQueryService;
import guru.interlis.mabillon.storage.DocumentStorage;
import guru.interlis.mabillon.storage.DocumentUpload;
import guru.interlis.mabillon.unterlage.AssignUnterlageCommand;
import guru.interlis.mabillon.unterlage.OpenedDocument;
import guru.interlis.mabillon.unterlage.RegisterUnterlageCommand;
import guru.interlis.mabillon.unterlage.UnterlageContentService;
import guru.interlis.mabillon.unterlage.UnterlageQueryService;
import guru.interlis.mabillon.unterlage.UnterlageService;
import guru.interlis.mabillon.unterlage.UnterlageView;
import guru.interlis.mabillon.catalog.CatalogService;
import guru.interlis.mabillon.catalog.CatalogType;
import guru.interlis.mabillon.journal.JournalQueryService;
import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.search.GlobalSearchCriteria;
import guru.interlis.mabillon.search.GlobalSearchResult;
import guru.interlis.mabillon.search.GlobalSearchService;
import guru.interlis.mabillon.quality.DataQualityService;
import guru.interlis.mabillon.quality.QualityReport;
import guru.interlis.mabillon.archivierung.ArchivAblieferungService;
import guru.interlis.mabillon.archivierung.ArchivAblieferungView;
import guru.interlis.mabillon.archivierung.AussonderungQueryService;
import guru.interlis.mabillon.archivierung.CreateArchivAblieferungCommand;
import guru.interlis.mabillon.archivierung.SipService;
import guru.interlis.mabillon.archivierung.SippaketView;
import guru.interlis.mabillon.interlis.ExportSelection;
import guru.interlis.mabillon.interlis.InterlisExchangeService;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.numbering.NumberingService;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.registraturplan.RegistraturplanAdminService;
import guru.interlis.mabillon.registraturplan.RegistraturplanQueryService;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class Phase0CompatibilityTest {

    private static final Path STORAGE_ROOT = temporaryStorageRoot();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @LocalServerPort
    private int localPort;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CayenneRuntime cayenneRuntime;

    @Autowired
    private CayenneUnitOfWork unitOfWork;

    @Autowired
    private DossierQueryService dossierQueryService;

    @Autowired
    private GeschaeftQueryService geschaeftQueryService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private DossierService dossierService;

    @Autowired
    private GeschaeftService geschaeftService;

    @Autowired
    private JournalQueryService journalQueryService;

    @Autowired
    private NumberingService numberingService;

    @Autowired
    private RegistraturplanAdminService registraturplanAdminService;

    @Autowired
    private RegistraturplanQueryService registraturplanQueryService;

    @Autowired
    private BeteiligterService beteiligterService;

    @Autowired
    private BeteiligungService beteiligungService;

    @Autowired
    private AufgabeService aufgabeService;

    @Autowired
    private AufgabeQueryService aufgabeQueryService;

    @Autowired
    private MyWorkQueryService myWorkQueryService;

    @Autowired
    private GeschaeftskontrolleQueryService geschaeftskontrolleQueryService;

    @Autowired
    private DocumentStorage documentStorage;

    @Autowired
    private UnterlageService unterlageService;

    @Autowired
    private UnterlageQueryService unterlageQueryService;

    @Autowired
    private UnterlageContentService unterlageContentService;

    @Autowired
    private FachsystemReferenzService fachsystemReferenzService;

    @Autowired
    private GlobalSearchService globalSearchService;

    @Autowired
    private DataQualityService dataQualityService;

    @Autowired
    private InterlisExchangeService interlisExchangeService;

    @Autowired
    private ArchivAblieferungService archivAblieferungService;

    @Autowired
    private AussonderungQueryService aussonderungQueryService;

    @Autowired
    private SipService sipService;

    @DynamicPropertySource
    static void cayenneProperties(DynamicPropertyRegistry registry) {
        registry.add("mabillon.cayenne.url", POSTGRES::getJdbcUrl);
        registry.add("mabillon.cayenne.username", POSTGRES::getUsername);
        registry.add("mabillon.cayenne.password", POSTGRES::getPassword);
        registry.add("mabillon.storage.root", () -> STORAGE_ROOT.toString());
        registry.add("mabillon.interlis.db-host", () -> "localhost");
        registry.add("mabillon.interlis.db-port", () -> Integer.toString(POSTGRES.getMappedPort(5432)));
        registry.add("mabillon.interlis.db-database", POSTGRES::getDatabaseName);
        registry.add("mabillon.interlis.db-user", POSTGRES::getUsername);
        registry.add("mabillon.interlis.db-password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void importGoldenPath() throws IOException, InterruptedException {
        runScript("scripts/create-schema.sh");
        runScript("scripts/import-xtf.sh", "model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf");
        runScript("scripts/import-xtf.sh", "model/testdata/02_SO_AGI_GEVER_20260707_testdaten_stammdaten.xtf");
        runScript("scripts/import-xtf.sh", "model/testdata/03_SO_AGI_GEVER_20260707_testdaten_geschaeftsdaten.xtf");
    }

    @Test
    void springBootAndJteRenderTheLocalTemplate() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1>Mabillon</h1>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-D-2026-000007")));
    }

    @Test
    void cayenneRuntimeStartsWithTheNewBuilderApiAndHasARealDatasource() throws Exception {
        assertThat(cayenneRuntime).isNotNull();
        try (var connection = cayenneRuntime.getDataSource().getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
    }

    @Test
    void queryNomenklaturDossier() {
        DossierView dossier = dossierQueryService.findByNumber("AGI-D-2026-000007").orElseThrow();

        assertThat(dossier.title()).contains("Bodenrain");
        assertThat(dossier.geschaefte()).singleElement()
                .extracting(DossierView.GeschaeftSummary::number)
                .isEqualTo("AGI-G-2026-000421");
        assertThat(dossier.unterlagen()).hasSize(9);
    }

    @Test
    void queryNomenklaturGeschaeft() {
        GeschaeftView geschaeft = geschaeftQueryService.findByNumber("AGI-G-2026-000421").orElseThrow();

        assertThat(geschaeft.title()).contains("Musterwil");
        assertThat(geschaeft.dossierNumber()).isEqualTo("AGI-D-2026-000007");
        assertThat(geschaeft.unterlagen()).isNotEmpty();
    }

    @Test
    void normalHttpAndHtmxUseTheSameReadOnlyUseCase() throws Exception {
        mockMvc.perform(get("/dossiers/AGI-D-2026-000007"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"dossier-detail\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gemeinde Musterwil")));

        mockMvc.perform(get("/geschaefte/AGI-G-2026-000421").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"geschaeft-detail\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("app-header"))));
    }

    @Test
    void phaseThreeSearchListsAndCreationFormsRender() throws Exception {
        mockMvc.perform(get("/dossiers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dossiers")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-D-2026-000007")));
        mockMvc.perform(get("/geschaefte").param("title", "Musterwil"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-G-2026-000421")));
        mockMvc.perform(get("/dossiers/neu"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Neues Dossier")));
        mockMvc.perform(get("/geschaefte/neu"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Neues Geschäft")));
    }

    @Test
    void unitOfWorkReadUsesShortLivedObjectContext() {
        String number = unitOfWork.read(context -> {
            assertThat(context).isNotNull();
            return "AGI-D-2026-000007";
        });

        assertThat(number).isEqualTo("AGI-D-2026-000007");
        assertThat(localPort).isPositive();
    }

    @Test
    void sachbearbeiterCannotChangeAdminCatalogs() throws Exception {
        mockMvc.perform(post("/admin/kataloge/geschaeftsart")
                        .with(httpBasic("sachbearbeiter", "sachbearbeiter"))
                        .with(csrf())
                        .param("code", "NICHT_ERLAUBT")
                        .param("name", "Nicht erlaubt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPagesRenderWithAdminIdentity() throws Exception {
        mockMvc.perform(get("/admin").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Administration")));
        mockMvc.perform(get("/admin/kataloge").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geschäftsarten")));
        mockMvc.perform(get("/admin/stammdaten").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Anna Müller")));
        mockMvc.perform(get("/admin/registraturplan").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("4.3.2")));
    }

    @Test
    void adminCanCreateAndDeactivateCatalogValueWithoutDeletingItsHistory() throws Exception {
        mockMvc.perform(post("/admin/kataloge/geschaeftsart")
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .param("code", "PHASE2_TEST")
                        .param("name", "Phase 2 Testwert"))
                .andExpect(status().is3xxRedirection());

        assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "PHASE2_TEST").active()).isTrue();

        mockMvc.perform(post("/admin/kataloge/geschaeftsart/PHASE2_TEST/deactivate")
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "PHASE2_TEST").active()).isFalse();
        assertThat(catalogService.list(CatalogType.GESCHAEFTSART, true))
                .anyMatch(value -> value.code().equals("PHASE2_TEST"));
    }

    @Test
    void eachSeededBusinessTypeHasExactlyOneInitialProcessStatus() {
        assertThat(catalogService.initialProcessStatus("NOMENKLATURMUTATION").initial()).isTrue();
        assertThat(catalogService.processStatusesForGeschaeftsart("NOMENKLATURMUTATION"))
                .hasSize(9);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void usedCatalogValueCanBeDeactivatedAndRemainsReadable() {
        catalogService.deactivate(CatalogType.GESCHAEFTSART, "NOMENKLATURMUTATION");
        try {
            assertThat(catalogService.get(CatalogType.GESCHAEFTSART, "NOMENKLATURMUTATION").active()).isFalse();
            assertThat(geschaeftQueryService.findByNumber("AGI-G-2026-000421")).isPresent();
        } finally {
            catalogService.activate(CatalogType.GESCHAEFTSART, "NOMENKLATURMUTATION");
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void registraturplanCycleIsRejected() {
        assertThatThrownBy(() -> registraturplanAdminService.movePosition("4.3", "4.3.2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Zyklus");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void inactivePositionIsNotSelectableForNewDossiersButHistoricalDossierRemainsReadable() {
        registraturplanAdminService.deactivatePosition("4.3.2");
        try {
            assertThat(registraturplanQueryService.activeLeafPositions())
                    .noneMatch(position -> position.code().equals("4.3.2"));
            assertThat(dossierQueryService.findByNumber("AGI-D-2026-000007")).isPresent();
        } finally {
            registraturplanAdminService.updatePosition(new guru.interlis.mabillon.registraturplan.UpdatePositionCommand(
                    "4.3.2", "Einzelgeschäfte Flur- und Ortsnamen", null, "aktiv"));
        }
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
                "Phase 3 Dossier",
                "Dossier für den Kern-Use-Case.",
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()),
                "Phase 3 Geschäft",
                "Kern-Use-Case für Status und Journal.",
                "NOMENKLATURMUTATION",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 16),
                null,
                1));

        assertThat(business.lifecycleStatus()).isEqualTo("Eroeffnet");
        assertThat(business.processStatusCode()).isEqualTo("ANTRAG_EINGEGANGEN");
        assertThat(journalQueryService.findForGeschaeft(GeschaeftNumber.parse(business.number()), 10))
                .hasSize(1);

        GeschaeftView changed = geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                GeschaeftNumber.parse(business.number()), "FORMELLE_PRUEFUNG", "Formelle Prüfung begonnen."));

        assertThat(changed.lifecycleStatus()).isEqualTo("In_Bearbeitung");
        assertThat(changed.processStatusCode()).isEqualTo("FORMELLE_PRUEFUNG");
        assertThat(journalQueryService.findForGeschaeft(GeschaeftNumber.parse(business.number()), 10))
                .hasSize(2)
                .anyMatch(entry -> entry.typ().name().equals("Status_geaendert"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void processStatusEndpointSupportsNormalHttpAndHtmxFallback() throws Exception {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 3 HTTP Dossier",
                null,
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()),
                "Phase 3 HTTP Geschäft",
                null,
                "NOMENKLATURMUTATION",
                "AGI-NOM",
                "anna.mueller",
                null,
                LocalDate.of(2026, 8, 16),
                null,
                null));

        mockMvc.perform(post("/geschaefte/{number}/prozessstatus", business.number())
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .param("processStatusCode", "FORMELLE_PRUEFUNG")
                        .param("comment", "Normale HTTP-Anfrage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/geschaefte/" + business.number()));

        mockMvc.perform(post("/geschaefte/{number}/prozessstatus", business.number())
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("processStatusCode", "FORMELLE_PRUEFUNG")
                        .param("comment", "HTMX-Anfrage"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "id=\"geschaeft-status-panel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FORMELLE_PRUEFUNG")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void processAndResultStatusesOfAnotherBusinessTypeAreRejectedWithoutJournal() {
        catalogService.create(new CatalogCreateCommand(
                CatalogType.PROZESSSTATUS,
                "ONLY_AUSKUNFT_PHASE3",
                "Nur Auskunft Prozessstatus",
                null,
                "NOMENKLATURAUSKUNFT",
                999,
                false,
                false,
                false));
        catalogService.create(new CatalogCreateCommand(
                CatalogType.RESULTATSTATUS,
                "ONLY_AUSKUNFT_RESULT_PHASE3",
                "Nur Auskunft Resultat",
                null,
                "NOMENKLATURAUSKUNFT",
                999,
                false,
                true,
                false));

        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 3 Validierungsdossier",
                null,
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()),
                "Phase 3 Validierungsgeschäft",
                null,
                "NOMENKLATURMUTATION",
                "AGI-NOM",
                "anna.mueller",
                null,
                LocalDate.of(2026, 8, 16),
                null,
                null));
        GeschaeftNumber number = GeschaeftNumber.parse(business.number());
        int journalBefore = journalQueryService.findForGeschaeft(number, 10).size();

        assertThatThrownBy(() -> geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                number, "ONLY_AUSKUNFT_PHASE3", "falscher Typ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Geschäftsart");
        assertThatThrownBy(() -> geschaeftService.setResult(new SetResultCommand(
                number, "ONLY_AUSKUNFT_RESULT_PHASE3", "falscher Typ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Geschäftsart");

        assertThat(journalQueryService.findForGeschaeft(number, 10)).hasSize(journalBefore);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFourCreatesAndValidatesBeteiligung() {
        GeschaeftView business = newPhaseFourBusiness("Beteiligung");
        BeteiligterView party = beteiligterService.create(new guru.interlis.mabillon.beteiligung.CreateBeteiligterCommand(
                "Organisation", "Phase 4 Gemeinde", null, "Gemeindeverwaltung", null, null, null,
                "PHASE4-PARTY"));

        BeteiligungView value = beteiligungService.add(new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "ANTRAGSTELLERIN", null,
                LocalDate.of(2026, 8, 16), null, "Antrag eingereicht."));

        assertThat(value.beteiligterName()).isEqualTo("Phase 4 Gemeinde");
        assertThat(beteiligungService.listForGeschaeft(GeschaeftNumber.parse(business.number())))
                .anyMatch(item -> item.tid().equals(value.tid()));
        assertThat(journalQueryService.findForObject(EreignisObjektTyp.Beteiligung,
                value.tid().toString(), 10))
                .anyMatch(entry -> entry.typ().name().equals("Zugewiesen"));

        assertThatThrownBy(() -> beteiligungService.add(new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "NICHT_AKTIVE_ROLLE", null,
                null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Beteiligungsrolle");
        assertThatThrownBy(() -> new AddBeteiligungCommand(
                GeschaeftNumber.parse(business.number()), party.tid(), "ANTRAGSTELLERIN", null,
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 16), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFourCreatesStartsCompletesAndJournalsAufgabe() throws Exception {
        GeschaeftView business = newPhaseFourBusiness("Aufgabe");
        AufgabeView task = aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "Phase 4 Aufgabe", "Bitte prüfen.",
                "AUFGABE_FORMELLE_PRUEFUNG", LocalDate.of(2026, 8, 20), 3,
                "anna.mueller", null));

        assertThat(task.status()).isEqualTo("Offen");
        assertThat(task.assignedUsername()).isEqualTo("anna.mueller");
        assertThat(aufgabeQueryService.forGeschaeft(GeschaeftNumber.parse(business.number())))
                .extracting(AufgabeView::tid).contains(task.tid());

        AufgabeView updated = aufgabeService.update(new UpdateAufgabeCommand(
                task.tid(), "Phase 4 Aufgabe aktualisiert", "Neue Beschreibung.",
                LocalDate.of(2026, 8, 21), 4));
        assertThat(updated.title()).isEqualTo("Phase 4 Aufgabe aktualisiert");
        AufgabeView delegated = aufgabeService.delegate(new DelegateAufgabeCommand(
                task.tid(), null, "AGI-NOM"));
        assertThat(delegated.status()).isEqualTo("Delegiert");
        AufgabeView started = aufgabeService.start(task.tid());
        assertThat(started.status()).isEqualTo("In_Arbeit");
        AufgabeView completed = aufgabeService.complete(new CompleteAufgabeCommand(task.tid(), "Prüfung abgeschlossen."));
        assertThat(completed.status()).isEqualTo("Erledigt");
        assertThat(completed.completedAt()).isNotNull();
        assertThat(journalQueryService.findForObject(EreignisObjektTyp.Aufgabe, task.tid().toString(), 10))
                .extracting(entry -> entry.typ().name())
                .contains("Aufgabe_erstellt", "Status_geaendert", "Aufgabe_erledigt");

        assertThatThrownBy(() -> aufgabeService.update(new UpdateAufgabeCommand(
                task.tid(), "Nachträgliche Änderung", null, null, 1)))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("abgeschlossen");
        assertThatThrownBy(() -> aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "Fehlerhafte Aufgabe", null,
                "NICHT_VORHANDEN", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aufgabentyp");

        AufgabeView httpTask = aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "HTTP-Aufgabe", null,
                "RUECKFRAGE", null, null, "anna.mueller", null));
        mockMvc.perform(post("/aufgaben/{tid}/start", httpTask.tid())
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/geschaefte/" + business.number()));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFourMyWorkContainsOpenAndOverdueTasks() {
        GeschaeftView business = newPhaseFourBusiness("Meine Arbeit");
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
    void phaseFiveStoresRegistersAndDownloadsDocumentWithoutUsingFilenameAsPath() throws Exception {
        GeschaeftView business = newPhaseFourBusiness("Unterlage");
        byte[] content = "Mabillon Phase 5\n".getBytes(StandardCharsets.UTF_8);
        UnterlageView document = unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(business.dossierNumber()), GeschaeftNumber.parse(business.number()),
                "Phase 5 Textunterlage", "AKTENNOTIZ", LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 16), null, true, "TXT", "Ablagetest."),
                new DocumentUpload("../../ausserhalb.txt", "text/plain", new ByteArrayInputStream(content)));

        assertThat(document.filename()).isEqualTo("../../ausserhalb.txt");
        assertThat(document.storageUri()).doesNotContain("..").startsWith("mabillon:objects/");
        assertThat(documentStorage.exists(document.storageUri())).isTrue();
        try (OpenedDocument opened = unterlageContentService.open(document.tid())) {
            assertThat(opened.content().readAllBytes()).isEqualTo(content);
        }
        mockMvc.perform(get("/unterlagen/{tid}/download", document.tid()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().bytes(content));
        assertThat(unterlageQueryService.forGeschaeft(GeschaeftNumber.parse(business.number())))
                .extracting(UnterlageView::tid).contains(document.tid());
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseFiveEnforcesDossierConsistencyAndCleansStagingAfterRegistrationFailure() {
        GeschaeftView first = newPhaseFourBusiness("Unterlage eins");
        GeschaeftView second = newPhaseFourBusiness("Unterlage zwei");
        byte[] content = "Konsistenztest".getBytes(StandardCharsets.UTF_8);
        UnterlageView document = unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(first.dossierNumber()), null, "Dossierweite Unterlage", "AKTENNOTIZ",
                null, null, null, true, "TXT", null),
                new DocumentUpload("dossierweite.txt", "text/plain", new ByteArrayInputStream(content)));

        assertThatThrownBy(() -> unterlageService.assignToGeschaeft(new AssignUnterlageCommand(
                document.tid(), GeschaeftNumber.parse(second.number()))))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("demselben Dossier");
        assertThatThrownBy(() -> unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(first.dossierNumber()), null, "Fehlerhafte Unterlage", "NICHT_VORHANDEN",
                null, null, null, true, "TXT", null),
                new DocumentUpload("failure.txt", "text/plain", new ByteArrayInputStream(content))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unterlagentyp");
        try (var files = Files.walk(STORAGE_ROOT.resolve("staging"))) {
            assertThat(files.filter(Files::isRegularFile).count()).isZero();
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseSixManagesFachsystemReferencesForDossierAndGeschaeft() {
        GeschaeftView business = newPhaseFourBusiness("Fachsystem");
        FachsystemReferenzView businessReference = fachsystemReferenzService.addToGeschaeft(
                new AddFachsystemReferenzCommand(GeschaeftNumber.parse(business.number()), "NOMENKLATUR",
                        "Flurname", "FLN-PHASE6-000001", "MUT-PHASE6-000001",
                        "https://nomenklatur.example/FLN-PHASE6-000001", "Phase-6-Referenz"));
        FachsystemReferenzView dossierReference = fachsystemReferenzService.addToDossier(
                new AddFachsystemReferenzToDossierCommand(DossierNumber.parse(business.dossierNumber()),
                        "AV", "Grundstück", "GS-PHASE6-000001", null, null, "Dossierweite Referenz"));

        assertThat(fachsystemReferenzService.forGeschaeft(GeschaeftNumber.parse(business.number())))
                .extracting(FachsystemReferenzView::objektId).containsExactly("FLN-PHASE6-000001");
        assertThat(fachsystemReferenzService.forDossier(DossierNumber.parse(business.dossierNumber())))
                .extracting(FachsystemReferenzView::tid)
                .contains(businessReference.tid(), dossierReference.tid());

        fachsystemReferenzService.remove(businessReference.tid(), "Referenz korrigiert.");
        assertThat(fachsystemReferenzService.forGeschaeft(GeschaeftNumber.parse(business.number()))).isEmpty();
        assertThat(journalQueryService.findForObject(EreignisObjektTyp.FachsystemReferenz,
                businessReference.tid().toString(), 10)).isNotEmpty();
    }

    @Test
    void phaseSixGlobalSearchFindsGoldenPathAndFachsystemIdentifiersWithPagination() {
        GlobalSearchResult byName = globalSearchService.search(new GlobalSearchCriteria("Bodenrain", 0, 50));
        assertThat(byName.items()).anyMatch(item -> item.objectType().equals("Dossier"));
        assertThat(byName.items()).anyMatch(item -> item.objectType().equals("Geschaeft"));
        assertThat(byName.items()).anyMatch(item -> item.objectType().equals("Unterlage"));

        GlobalSearchResult byReference = globalSearchService.search(new GlobalSearchCriteria(
                null, null, null, null, null, null, null, null, null, "FLN-4500-000123", 0, 20));
        assertThat(byReference.items()).anyMatch(item -> item.objectType().equals("FachsystemReferenz"));

        GlobalSearchResult firstPage = globalSearchService.search(new GlobalSearchCriteria(null, 0, 2));
        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.totalElements()).isGreaterThan(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    void phaseSixSearchAndControlPagesRenderThroughHttp() throws Exception {
        mockMvc.perform(get("/suche").param("q", "Bodenrain"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Systemweite Suche")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGI-D-2026-000007")));
        mockMvc.perform(get("/geschaeftskontrolle"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geschäftskontrolle")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Offene Geschäfte")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseSevenGoldenPathClosesBusinessAndDossierWithQualityAndAudit() throws Exception {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 7 Golden-Path Dossier", "Datenqualitäts- und Abschlussprüfung.",
                "4.3.3", "AGI-NOM", "anna.mueller", LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Phase 7 Golden-Path Geschäft", null,
                "NOMENKLATURMUTATION", "AGI-NOM", "anna.mueller", null,
                LocalDate.of(2026, 8, 16), null, 1));

        assertThatThrownBy(() -> dossierService.close(DossierNumber.parse(dossier.number())))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("nicht abgeschlossen");

        geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                GeschaeftNumber.parse(business.number()), "ABGESCHLOSSEN", "Abschluss fachlich bereit."));
        geschaeftService.setResult(new SetResultCommand(
                GeschaeftNumber.parse(business.number()), "GENEHMIGT", "Golden-Path-Entscheid."));

        QualityReport businessQuality = dataQualityService.checkGeschaeft(
                GeschaeftNumber.parse(business.number()));
        mockMvc.perform(post("/geschaefte/{number}/abschluss", business.number())
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/geschaefte/" + business.number()));

        assertThat(businessQuality.hasErrors()).isFalse();
        assertThat(geschaeftQueryService.findByNumber(business.number()).orElseThrow().lifecycleStatus())
                .isEqualTo("Abgeschlossen");
        assertThat(journalQueryService.findForGeschaeft(GeschaeftNumber.parse(business.number()), 20))
                .extracting(entry -> entry.typ().name())
                .contains("Geschaeft_abgeschlossen");

        mockMvc.perform(get("/datenqualitaet/geschaefte/{number}", business.number())
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Keine Datenqualitätsbefunde")));
        mockMvc.perform(get("/geschaefte/{number}", business.number())
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geschaeft_abgeschlossen")));

        mockMvc.perform(post("/dossiers/{number}/abschluss", dossier.number())
                        .with(httpBasic("admin", "admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dossiers/" + dossier.number()));
        assertThat(dossierQueryService.findByNumber(dossier.number()).orElseThrow().status())
                .isEqualTo("Geschlossen");
        assertThat(journalQueryService.findForDossier(DossierNumber.parse(dossier.number()), 20))
                .extracting(entry -> entry.typ().name())
                .contains("Dossier_abgeschlossen");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseSevenBusinessClosureRejectsOpenTasks() {
        GeschaeftView business = newPhaseFourBusiness("Abschluss offene Aufgabe");
        aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(business.number()), "Noch offene Aufgabe", null,
                "RUECKFRAGE", null, 1, "anna.mueller", null));

        assertThatThrownBy(() -> geschaeftService.close(GeschaeftNumber.parse(business.number())))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("offene Aufgaben");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseEightExportsValidatedCatalogWithStableTidAndBasket() throws IOException {
        Path exportDirectory = Files.createTempDirectory("mabillon-phase8-export-");
        Path target = exportDirectory.resolve("kataloge.xtf");

        Path exported = interlisExchangeService.exportCatalog(ExportSelection.all(target));
        String content = Files.readString(exported, StandardCharsets.UTF_8);

        assertThat(exported).isEqualTo(target);
        assertThat(content).contains("ili:bid=\"c4dbb2a2-9b06-525d-b2d9-e69b8d9e7013\"");
        assertThat(content).contains("ili:tid=\"d5410f91-14ed-50c7-9596-f8c227db72c1\"");
        assertThat(content).contains("<SO_AGI_GEVER_20260707:Kataloge");
        assertThat(content.split("ili:tid=", -1).length).isGreaterThan(40);
    }

    @Test
    void phaseEightExchangePageRendersForAdministrators() throws Exception {
        mockMvc.perform(get("/admin/interlis").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INTERLIS-Datenaustausch")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kataloge")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Importieren")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseNineGeneratesValidatesCorrectsAndAcceptsStructuredSip() throws Exception {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 9 Archivdossier", "Archivierungs-Golden-Path.", "4.3.3", "AGI-NOM", "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Phase 9 Archivgeschäft", null, "NOMENKLATURMUTATION",
                "AGI-NOM", "anna.mueller", null, LocalDate.of(2026, 8, 16), null, 1));
        unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(dossier.number()), GeschaeftNumber.parse(business.number()),
                "Archivfähige Unterlage", "AKTENNOTIZ", LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 16),
                null, true, "TXT", null),
                new DocumentUpload("archiv-faehig.txt", "text/plain",
                        new ByteArrayInputStream("Archivinhalt Phase 9".getBytes(StandardCharsets.UTF_8))));
        geschaeftService.changeProcessStatus(new ChangeProcessStatusCommand(
                GeschaeftNumber.parse(business.number()), "ABGESCHLOSSEN", "Archivierung vorbereitet."));
        geschaeftService.setResult(new SetResultCommand(
                GeschaeftNumber.parse(business.number()), "GENEHMIGT", "Archivierungsentscheid."));
        geschaeftService.close(GeschaeftNumber.parse(business.number()));
        dossierService.close(DossierNumber.parse(dossier.number()));

        assertThat(aussonderungQueryService.eligible(0, 100).items())
                .anyMatch(item -> item.dossierNumber().equals(dossier.number()));
        ArchivAblieferungView delivery = archivAblieferungService.create(new CreateArchivAblieferungCommand(
                "AGI", "Phase 9 SIP", "Staatsarchiv", "Golden-Path-Prüfung."));
        delivery = archivAblieferungService.addDossier(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()),
                DossierNumber.parse(dossier.number()));
        delivery = archivAblieferungService.markReady(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        assertThat(delivery.status()).isEqualTo("Bereit");
        assertThat(dataQualityService.checkArchiveDelivery(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber())).hasErrors())
                .isFalse();

        SippaketView first = sipService.generate(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        Path firstPackage = Path.of(java.net.URI.create(first.storageUri()));
        assertThat(firstPackage.resolve("header/metadata.xml")).isRegularFile();
        assertThat(firstPackage.resolve("header/xsd/arelda.xsd")).isRegularFile();
        assertThat(firstPackage.resolve("content/dossier_" + dossier.number() + "/p000001.txt")).isRegularFile();

        Files.writeString(firstPackage.resolve("header/metadata.xml"), "<unlesbar/>", StandardCharsets.UTF_8);
        SippaketView invalid = sipService.validate(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()), 1);
        assertThat(invalid.validationStatus()).isEqualTo("Ungueltig");
        assertThat(archivAblieferungService.get(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber())).status())
                .isEqualTo("Korrektur_erforderlich");

        SippaketView second = sipService.generate(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        assertThat(second.attempt()).isEqualTo(2);
        SippaketView valid = sipService.validateLatest(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()));
        assertThat(valid.validationStatus()).isEqualTo("Gueltig");

        archivAblieferungService.recordTransferred(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()), "Übernahme vorbereitet.");
        ArchivAblieferungView accepted = archivAblieferungService.recordAccepted(
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber()),
                "BAR-2026-0009", "Übernahme bestätigt.");
        assertThat(accepted.status()).isEqualTo("Uebernommen");
        assertThat(dossierQueryService.findByNumber(dossier.number()).orElseThrow().status()).isEqualTo("Archiviert");
    }

    @Test
    void phaseNineArchivePageRendersForAdministrators() throws Exception {
        mockMvc.perform(get("/archivierung").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Archivierung")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Neue Archivablieferung")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void phaseNineRejectsOpenDossierFromArchiveDelivery() {
        DossierView open = dossierService.open(new OpenDossierCommand(
                "Noch offenes Archivdossier", null, "4.3.3", "AGI-NOM", "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        ArchivAblieferungView delivery = archivAblieferungService.create(new CreateArchivAblieferungCommand(
                "AGI", "Ungültige Archivablieferung", "Staatsarchiv", null));
        guru.interlis.mabillon.numbering.ArchivAblieferungNumber number =
                guru.interlis.mabillon.numbering.ArchivAblieferungNumber.parse(delivery.deliveryNumber());

        assertThatThrownBy(() -> archivAblieferungService.addDossier(number, DossierNumber.parse(open.number())))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("geschlossene");
        assertThat(archivAblieferungService.get(number).dossiers()).isEmpty();
    }

    @Test
    void phaseTenExposesSafeHealthEndpointAndSecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UP")));
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());
    }

    @Test
    void phaseTenProtectsMutationsWithCsrfAndRendersErrorPage() throws Exception {
        mockMvc.perform(post("/archivierung/ablieferungen")
                        .with(httpBasic("admin", "admin"))
                        .param("organisationCode", "AGI")
                        .param("title", "CSRF-Test")
                        .param("archivempfaenger", "Staatsarchiv"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HTTP 404")));
    }

    private GeschaeftView newPhaseFourBusiness(String label) {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 4 Dossier " + label,
                "Dossier für Phase 4.",
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 16)));
        return geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()),
                "Phase 4 Geschäft " + label,
                "Geschäft für Phase 4.",
                "NOMENKLATURMUTATION",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 31),
                2));
    }

    private static Path temporaryStorageRoot() {
        try {
            return Files.createTempDirectory("mabillon-phase5-storage-");
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void runScript(String script, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add(script);
        command.addAll(List.of(arguments));

        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true);
        builder.environment().put("PGHOST", POSTGRES.getHost());
        builder.environment().put("PGPORT", Integer.toString(POSTGRES.getMappedPort(5432)));
        builder.environment().put("PGDATABASE", POSTGRES.getDatabaseName());
        builder.environment().put("PGUSER", POSTGRES.getUsername());
        builder.environment().put("PGPASSWORD", POSTGRES.getPassword());
        builder.directory(Path.of(System.getProperty("user.dir")).toFile());

        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Fixture import failed (" + exitCode + "):\n" + output);
        }
    }
}
