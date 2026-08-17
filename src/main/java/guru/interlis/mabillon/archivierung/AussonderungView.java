package guru.interlis.mabillon.archivierung;

public record AussonderungView(
        String dossierNumber,
        String title,
        String status,
        String closedAt,
        String registraturplanPosition,
        long documentCount,
        long warningCount) {
}
