package guru.interlis.mabillon.interlis;

import java.nio.file.Path;

public interface InterlisModelValidator {
    ValidationResult validate(Path iliModel);
}
