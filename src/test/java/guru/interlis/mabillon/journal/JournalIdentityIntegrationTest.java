package guru.interlis.mabillon.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.dossier.DossierService;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.numbering.DossierNumber;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class JournalIdentityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @Autowired
    private DossierService dossierService;

    @Autowired
    private JournalQueryService journalQueryService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("mabillon.cayenne.url", POSTGRES::getJdbcUrl);
        registry.add("mabillon.cayenne.username", POSTGRES::getUsername);
        registry.add("mabillon.cayenne.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void importFixtures() throws IOException, InterruptedException {
        runScript("scripts/create-schema.sh");
        runScript("scripts/import-xtf.sh", "model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf");
        runScript("scripts/import-xtf.sh", "model/testdata/02_SO_AGI_GEVER_20260707_testdaten_stammdaten.xtf");
        runScript("scripts/import-xtf.sh", "model/testdata/03_SO_AGI_GEVER_20260707_testdaten_geschaeftsdaten.xtf");
    }

    @Test
    @WithMockUser(username = "sachbearbeiter", roles = "MABILLON_SACHBEARBEITER")
    void journalUsesMappedDomainUser() {
        DossierView dossier = openDossier("Audit-Mapping");

        assertThat(journalQueryService.findForDossier(DossierNumber.parse(dossier.number()), 10))
                .singleElement()
                .extracting(JournalEntryView::username)
                .isEqualTo("a.keller");
    }

    @Test
    @WithMockUser(username = "unknown.audit.user", roles = "MABILLON_SACHBEARBEITER")
    void unknownDomainUserCannotBeSilentlyReplaced() {
        assertThatThrownBy(() -> openDossier("Unbekannter Audit-Akteur"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Journalakteur ist kein fachlicher Benutzer: unknown.audit.user");
    }

    private DossierView openDossier(String title) {
        return dossierService.open(new OpenDossierCommand(
                title,
                "Phase-11-Identitätsprüfung",
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 17)));
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
