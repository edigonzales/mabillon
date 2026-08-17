package guru.interlis.mabillon.archivierung;

public record SipValidationMessage(Severity severity, String message) {

    public enum Severity {
        ERROR,
        WARNING
    }
}
