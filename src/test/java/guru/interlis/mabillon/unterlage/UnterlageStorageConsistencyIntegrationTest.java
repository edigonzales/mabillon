package guru.interlis.mabillon.unterlage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.dossier.DossierService;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Ereignis;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.storage.DocumentStorage;
import guru.interlis.mabillon.storage.DocumentUpload;
import guru.interlis.mabillon.storage.FileSystemDocumentStorage;
import guru.interlis.mabillon.storage.StagedDocument;
import guru.interlis.mabillon.storage.StorageTarget;
import guru.interlis.mabillon.storage.StoredDocument;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@Import(UnterlageStorageConsistencyIntegrationTest.StorageTestConfiguration.class)
class UnterlageStorageConsistencyIntegrationTest {

    private static final Path STORAGE_ROOT = temporaryStorageRoot();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @Autowired
    private UnterlageService unterlageService;

    @Autowired
    private DossierService dossierService;

    @Autowired
    private CayenneUnitOfWork unitOfWork;

    @Autowired
    private ControllableDocumentStorage storage;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("mabillon.cayenne.url", POSTGRES::getJdbcUrl);
        registry.add("mabillon.cayenne.username", POSTGRES::getUsername);
        registry.add("mabillon.cayenne.password", POSTGRES::getPassword);
        registry.add("mabillon.storage.root", () -> STORAGE_ROOT.toString());
    }

    @BeforeAll
    static void importFixtures() throws IOException, InterruptedException {
        runScript("scripts/create-schema.sh");
        runScript("scripts/import-xtf.sh", "model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf");
        runScript("scripts/import-xtf.sh", "model/testdata/02_SO_AGI_GEVER_20260707_testdaten_stammdaten.xtf");
        runScript("scripts/import-xtf.sh", "model/testdata/03_SO_AGI_GEVER_20260707_testdaten_geschaeftsdaten.xtf");
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void failedFinalMoveRemovesCommittedMetadataAndJournal() throws Exception {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                "Phase 11 Storage Compensation Dossier",
                "Offenes Testdossier fuer den simulierten finalen Storage-Fehler.",
                "4.3.3",
                "AGI-NOM",
                "anna.mueller",
                LocalDate.of(2026, 8, 17)));
        String title = "Phase 11 Storage Compensation";
        long journalBefore = registeredJournalCount();
        storage.failNextCommit();

        assertThatThrownBy(() -> unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(dossier.number()), null, title, "AKTENNOTIZ",
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 17), null,
                true, "TXT", "Simulierter Storage-Fehler."),
                new DocumentUpload("storage-failure.txt", "text/plain",
                        new ByteArrayInputStream("storage failure".getBytes(StandardCharsets.UTF_8)))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("endgültig abgelegt");

        List<Unterlage> remaining = unitOfWork.read(context -> ObjectSelect.query(Unterlage.class)
                .where(Unterlage.TITEL.eq(title)).select(context));
        assertThat(remaining).isEmpty();
        assertThat(registeredJournalCount()).isEqualTo(journalBefore);

        Path staging = STORAGE_ROOT.resolve("staging");
        if (Files.exists(staging)) {
            try (var files = Files.list(staging)) {
                assertThat(files).noneMatch(Files::isRegularFile);
            }
        }
    }

    private long registeredJournalCount() {
        return unitOfWork.read(context -> ObjectSelect.query(Ereignis.class)
                .where(Ereignis.TYP.eq(EreignisTyp.Unterlage_registriert.name()))
                .select(context).size());
    }

    @TestConfiguration
    static class StorageTestConfiguration {

        @Bean
        @Primary
        ControllableDocumentStorage controllableDocumentStorage(
                @Value("${mabillon.storage.root}") String root,
                @Value("${mabillon.storage.max-file-size-bytes:52428800}") long maxFileSizeBytes) {
            return new ControllableDocumentStorage(new FileSystemDocumentStorage(root, maxFileSizeBytes));
        }
    }

    static final class ControllableDocumentStorage implements DocumentStorage {

        private final DocumentStorage delegate;
        private boolean failNextCommit;

        ControllableDocumentStorage(DocumentStorage delegate) {
            this.delegate = delegate;
        }

        void failNextCommit() {
            failNextCommit = true;
        }

        @Override
        public StagedDocument stage(DocumentUpload upload) throws IOException {
            return delegate.stage(upload);
        }

        @Override
        public StoredDocument describe(StagedDocument staged, StorageTarget target) throws IOException {
            return delegate.describe(staged, target);
        }

        @Override
        public StoredDocument commit(StagedDocument staged, StorageTarget target) throws IOException {
            if (failNextCommit) {
                failNextCommit = false;
                throw new IOException("Simulierter finaler Storage-Fehler.");
            }
            return delegate.commit(staged, target);
        }

        @Override
        public InputStream open(String storageUri) throws IOException {
            return delegate.open(storageUri);
        }

        @Override
        public boolean exists(String storageUri) {
            return delegate.exists(storageUri);
        }

        @Override
        public void discard(StagedDocument staged) throws IOException {
            delegate.discard(staged);
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

    private static Path temporaryStorageRoot() {
        try {
            return Files.createTempDirectory("mabillon-storage-consistency-");
        } catch (IOException failure) {
            throw new IllegalStateException("Temporäres Storage-Verzeichnis konnte nicht erstellt werden.", failure);
        }
    }
}
