package guru.interlis.mabillon.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemDocumentStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void describePlansFinalObjectWithoutMovingStagedFile() throws Exception {
        FileSystemDocumentStorage storage = new FileSystemDocumentStorage(temporaryDirectory.toString(), 1024);
        byte[] content = "Mabillon storage consistency".getBytes(StandardCharsets.UTF_8);
        StagedDocument staged = storage.stage(new DocumentUpload(
                "document.txt", "text/plain", new ByteArrayInputStream(content)));

        StoredDocument planned = storage.describe(staged, new StorageTarget("AGI-D-2026-000001"));

        assertThat(planned.originalFilename()).isEqualTo("document.txt");
        assertThat(planned.size()).isEqualTo(content.length);
        assertThat(storage.exists(planned.storageUri())).isFalse();
        assertThat(temporaryDirectory.resolve("staging").resolve(staged.token())).isRegularFile();

        StoredDocument committed = storage.commit(staged, new StorageTarget("AGI-D-2026-000001"));

        assertThat(committed).isEqualTo(planned);
        assertThat(storage.exists(planned.storageUri())).isTrue();
        assertThat(temporaryDirectory.resolve("staging").resolve(staged.token())).doesNotExist();

        storage.discard(staged);
        assertThat(storage.exists(planned.storageUri())).isFalse();
    }

    @Test
    void rejectsOversizedUploadAndRemovesPartialStagingFile() throws Exception {
        FileSystemDocumentStorage storage = new FileSystemDocumentStorage(temporaryDirectory.toString(), 4);

        assertThatThrownBy(() -> storage.stage(new DocumentUpload(
                "too-large.txt", "text/plain", new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8)))))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("maximale Grösse");
        Path staging = temporaryDirectory.resolve("staging");
        assertThat(Files.exists(staging)).isTrue();
        try (var files = Files.list(staging)) {
            assertThat(files).noneMatch(Files::isRegularFile);
        }
    }
}
