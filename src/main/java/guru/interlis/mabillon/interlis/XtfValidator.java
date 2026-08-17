package guru.interlis.mabillon.interlis;

import java.nio.file.Path;

public interface XtfValidator {
    ValidationResult validate(Path xtf);
}
