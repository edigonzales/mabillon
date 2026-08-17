package guru.interlis.mabillon.interlis;

import java.nio.file.Files;
import java.nio.file.Path;

import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.Main;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import org.springframework.stereotype.Component;

@Component
public final class JavaApiInterlisModelValidator implements InterlisModelValidator {

    @Override
    public ValidationResult validate(Path iliModel) {
        if (iliModel == null || !Files.isRegularFile(iliModel)) {
            return ValidationResult.invalid(2, "INTERLIS-Modell nicht gefunden: " + iliModel);
        }

        Configuration configuration = new Configuration();
        configuration.setAutoCompleteModelList(true);
        configuration.addFileEntry(new FileEntry(
                iliModel.toAbsolutePath().normalize().toString(), FileEntryKind.ILIMODELFILE));

        Settings settings = new Settings();
        settings.setValue(Ili2cSettings.ILIDIRS, InterlisToolDefaults.absoluteModelDir());
        Main.setDefaultIli2cPathMap(settings);

        try {
            if (Main.runCompiler(configuration, settings) == null) {
                return ValidationResult.invalid(1, "ili2c Java API: Kompilierung fehlgeschlagen.");
            }
            return ValidationResult.valid("ili2c Java API: Modell ist valide.");
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return ValidationResult.invalid(1,
                    "ili2c Java API: "
                            + (message == null || message.isBlank() ? failure.getClass().getSimpleName() : message));
        }
    }
}
