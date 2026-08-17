package guru.interlis.mabillon.archivierung;

public record SippaketView(
        int attempt,
        String status,
        String validationStatus,
        long size,
        String sha256,
        String storageUri,
        String validationReportUri) {
}
