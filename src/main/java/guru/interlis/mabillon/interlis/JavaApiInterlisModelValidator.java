package guru.interlis.mabillon.interlis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cFailure;
import org.springframework.stereotype.Component;

@Component
public final class JavaApiInterlisModelValidator implements InterlisModelValidator {

    @Override
    public ValidationResult validate(Path iliModel) {
        if (iliModel == null || !Files.isRegularFile(iliModel)) {
            return ValidationResult.invalid(2, "INTERLIS-Modell nicht gefunden: " + iliModel);
        }
        ArrayList<String> files = new ArrayList<>();
        files.add(iliModel.toAbsolutePath().normalize().toString());
        try {
            Ili2c.compileIliFiles(files, InterlisToolDefaults.modelDirectories());
            return ValidationResult.valid("ili2c Java API: Modell ist valide.");
        } catch (Ili2cFailure failure) {
            String message = failure.getMessage();
            return ValidationResult.invalid(1,
                    "ili2c Java API: "
                            + (message == null || message.isBlank() ? "Kompilierung fehlgeschlagen." : message));
        }
    }
}
