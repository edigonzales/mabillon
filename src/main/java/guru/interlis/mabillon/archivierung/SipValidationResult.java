package guru.interlis.mabillon.archivierung;

import java.nio.file.Path;
import java.util.List;

public record SipValidationResult(
        SipValidationStatus status,
        List<SipValidationMessage> messages,
        Path reportPath) {

    public SipValidationResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public boolean valid() {
        return status == SipValidationStatus.Gueltig
                || status == SipValidationStatus.Gueltig_mit_Warnungen;
    }
}
