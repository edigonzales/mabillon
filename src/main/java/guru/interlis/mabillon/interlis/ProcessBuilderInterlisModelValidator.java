package guru.interlis.mabillon.interlis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public final class ProcessBuilderInterlisModelValidator implements InterlisModelValidator {

    @Override
    public ValidationResult validate(Path iliModel) {
        if (iliModel == null || !Files.isRegularFile(iliModel)) {
            return ValidationResult.invalid(2, "INTERLIS-Modell nicht gefunden: " + iliModel);
        }
        List<String> command = new ArrayList<>(List.of(
                "java", "-jar", InterlisToolDefaults.ili2cJar().toString(),
                "--modeldir", ProcessBuilderSupport.absoluteModelDir(),
                "--quiet", iliModel.toAbsolutePath().normalize().toString()));
        ProcessBuilderSupport.ProcessResult result = ProcessBuilderSupport.run(command);
        return result.exitCode() == 0
                ? ValidationResult.valid(result.output())
                : ValidationResult.invalid(result.exitCode(), result.output());
    }
}
