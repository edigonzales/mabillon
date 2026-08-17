package guru.interlis.mabillon.archivierung;

public record CreateArchivAblieferungCommand(
        String organisationCode,
        String title,
        String archivempfaenger,
        String bemerkung) {

    public CreateArchivAblieferungCommand {
        if (organisationCode == null || organisationCode.isBlank()
                || title == null || title.isBlank()
                || archivempfaenger == null || archivempfaenger.isBlank()) {
            throw new IllegalArgumentException("Organisation, Titel und Archivempfänger sind erforderlich.");
        }
    }
}
