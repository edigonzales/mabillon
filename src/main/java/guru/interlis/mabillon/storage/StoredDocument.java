package guru.interlis.mabillon.storage;

public record StoredDocument(
        String storageUri,
        String originalFilename,
        String mimeType,
        long size,
        String sha256) {
}
