package guru.interlis.mabillon.storage;

import java.io.InputStream;

public record DocumentUpload(String originalFilename, String mimeType, InputStream content) {

    public DocumentUpload {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Originaldateiname ist erforderlich.");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME-Type ist erforderlich.");
        }
        if (content == null) {
            throw new IllegalArgumentException("Dateiinhalt ist erforderlich.");
        }
    }
}
