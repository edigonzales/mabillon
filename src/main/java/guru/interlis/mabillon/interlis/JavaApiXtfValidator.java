package guru.interlis.mabillon.interlis;

import java.nio.file.Files;
import java.nio.file.Path;

import ch.ehi.basics.settings.Settings;
import org.interlis2.validator.Validator;
import org.springframework.stereotype.Component;

@Component
public final class JavaApiXtfValidator implements XtfValidator {

    @Override
    public ValidationResult validate(Path xtf) {
        if (xtf == null || !Files.isRegularFile(xtf)) {
            return ValidationResult.invalid(2, "XTF-Datei nicht gefunden: " + xtf);
        }
        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_MODELNAMES, InterlisToolDefaults.MODEL_NAME);
        settings.setValue(Validator.SETTING_ILIDIRS, InterlisToolDefaults.absoluteModelDir());
        try {
            boolean valid = Validator.runValidation(
                    new String[] {xtf.toAbsolutePath().normalize().toString()}, settings);
            return valid
                    ? ValidationResult.valid("ilivalidator Java API: valide.")
                    : ValidationResult.invalid(1, "ilivalidator Java API: Validierung fehlgeschlagen.");
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return ValidationResult.invalid(1,
                    "ilivalidator Java API: "
                            + (message == null || message.isBlank() ? failure.getClass().getSimpleName() : message));
        }
    }
}
