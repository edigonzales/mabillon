package guru.interlis.mabillon.archivierung;

import java.nio.file.Path;

public interface SipValidator {
    SipValidationResult validate(Path sipPath);
}
