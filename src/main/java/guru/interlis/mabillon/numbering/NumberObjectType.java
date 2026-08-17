package guru.interlis.mabillon.numbering;

public enum NumberObjectType {
    GESCHAEFT("G"),
    DOSSIER("D"),
    ARCHIVABLIEFERUNG("A");

    private final String code;

    NumberObjectType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
