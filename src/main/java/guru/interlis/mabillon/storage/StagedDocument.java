package guru.interlis.mabillon.storage;

public record StagedDocument(
        String token,
        String originalFilename,
        String mimeType,
        long size,
        String sha256) {
}
