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
