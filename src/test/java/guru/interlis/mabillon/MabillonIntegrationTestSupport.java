package guru.interlis.mabillon;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import guru.interlis.mabillon.archivierung.ArchivAblieferungService;
import guru.interlis.mabillon.archivierung.AussonderungQueryService;
import guru.interlis.mabillon.archivierung.SipService;
import guru.interlis.mabillon.aufgabe.AufgabeQueryService;
import guru.interlis.mabillon.aufgabe.AufgabeService;
import guru.interlis.mabillon.beteiligung.BeteiligterService;
import guru.interlis.mabillon.beteiligung.BeteiligungService;
import guru.interlis.mabillon.catalog.CatalogService;
import guru.interlis.mabillon.dashboard.MyWorkQueryService;
import guru.interlis.mabillon.dossier.DossierQueryService;
import guru.interlis.mabillon.dossier.DossierService;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.fachsystem.FachsystemReferenzService;
import guru.interlis.mabillon.geschaeft.GeschaeftQueryService;
import guru.interlis.mabillon.geschaeft.GeschaeftService;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.GeschaeftskontrolleQueryService;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.interlis.InterlisExchangeService;
import guru.interlis.mabillon.journal.JournalQueryService;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.NumberingService;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.quality.DataQualityService;
import guru.interlis.mabillon.registraturplan.RegistraturplanAdminService;
import guru.interlis.mabillon.registraturplan.RegistraturplanQueryService;
import guru.interlis.mabillon.search.GlobalSearchService;
import guru.interlis.mabillon.storage.DocumentStorage;
import guru.interlis.mabillon.unterlage.UnterlageContentService;
import guru.interlis.mabillon.unterlage.UnterlageQueryService;
import guru.interlis.mabillon.unterlage.UnterlageService;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

abstract class MabillonIntegrationTestSupport {

    static final Path STORAGE_ROOT = temporaryStorageRoot();
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");
    private static final AtomicBoolean FIXTURE_IMPORTED = new AtomicBoolean();

    static {
        POSTGRES.start();
    }

    @LocalServerPort
    protected int localPort;

    @Autowired protected MockMvc mockMvc;
    @Autowired protected CayenneRuntime cayenneRuntime;
    @Autowired protected CayenneUnitOfWork unitOfWork;
    @Autowired protected DossierQueryService dossierQueryService;
    @Autowired protected GeschaeftQueryService geschaeftQueryService;
    @Autowired protected CatalogService catalogService;
    @Autowired protected DossierService dossierService;
    @Autowired protected GeschaeftService geschaeftService;
    @Autowired protected JournalQueryService journalQueryService;
    @Autowired protected NumberingService numberingService;
    @Autowired protected RegistraturplanAdminService registraturplanAdminService;
    @Autowired protected RegistraturplanQueryService registraturplanQueryService;
    @Autowired protected BeteiligterService beteiligterService;
    @Autowired protected BeteiligungService beteiligungService;
    @Autowired protected AufgabeService aufgabeService;
    @Autowired protected AufgabeQueryService aufgabeQueryService;
    @Autowired protected MyWorkQueryService myWorkQueryService;
    @Autowired protected GeschaeftskontrolleQueryService geschaeftskontrolleQueryService;
    @Autowired protected DocumentStorage documentStorage;
    @Autowired protected UnterlageService unterlageService;
    @Autowired protected UnterlageQueryService unterlageQueryService;
    @Autowired protected UnterlageContentService unterlageContentService;
    @Autowired protected FachsystemReferenzService fachsystemReferenzService;
    @Autowired protected GlobalSearchService globalSearchService;
    @Autowired protected DataQualityService dataQualityService;
    @Autowired protected InterlisExchangeService interlisExchangeService;
    @Autowired protected ArchivAblieferungService archivAblieferungService;
    @Autowired protected AussonderungQueryService aussonderungQueryService;
    @Autowired protected SipService sipService;

    @DynamicPropertySource
    static void cayenneProperties(DynamicPropertyRegistry registry) {
        registry.add("mabillon.cayenne.url", POSTGRES::getJdbcUrl);
        registry.add("mabillon.cayenne.username", POSTGRES::getUsername);
        registry.add("mabillon.cayenne.password", POSTGRES::getPassword);
        registry.add("mabillon.storage.root", () -> STORAGE_ROOT.toString());
        registry.add("mabillon.interlis.db-host", POSTGRES::getHost);
        registry.add("mabillon.interlis.db-port", () -> Integer.toString(POSTGRES.getMappedPort(5432)));
        registry.add("mabillon.interlis.db-database", POSTGRES::getDatabaseName);
        registry.add("mabillon.interlis.db-user", POSTGRES::getUsername);
        registry.add("mabillon.interlis.db-password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void importGoldenPathOnce() throws IOException, InterruptedException {
        if (FIXTURE_IMPORTED.compareAndSet(false, true)) {
            runScript("scripts/create-schema.sh");
            runScript("scripts/import-xtf.sh", "model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf");
            runScript("scripts/import-xtf.sh", "model/testdata/02_SO_AGI_GEVER_20260707_testdaten_stammdaten.xtf");
            runScript("scripts/import-xtf.sh", "model/testdata/03_SO_AGI_GEVER_20260707_testdaten_geschaeftsdaten.xtf");
        }
    }

    protected GeschaeftView newBusiness(String label) {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Integration Dossier " + label, "Dossier für Integrationstest.", "4.3.3", "AGI-NOM",
                "anna.mueller", LocalDate.of(2026, 8, 16)));
        return geschaeftService.open(new OpenGeschaeftCommand(
                DossierNumber.parse(dossier.number()), "Integration Geschäft " + label,
                "Geschäft für Integrationstest.", "NOMENKLATURMUTATION", "AGI-NOM", "anna.mueller",
                LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 31), 2));
    }

    private static Path temporaryStorageRoot() {
        try {
            return Files.createTempDirectory("mabillon-integration-storage-");
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void runScript(String script, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add(script);
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
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
