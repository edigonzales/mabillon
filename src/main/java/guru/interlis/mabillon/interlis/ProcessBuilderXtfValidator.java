package guru.interlis.mabillon.interlis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public final class ProcessBuilderXtfValidator implements XtfValidator {

    @Override
    public ValidationResult validate(Path xtf) {
        if (xtf == null || !Files.isRegularFile(xtf)) {
            return ValidationResult.invalid(2, "XTF-Datei nicht gefunden: " + xtf);
        }
        List<String> command = new ArrayList<>(List.of(
                "java", "-jar", InterlisToolDefaults.ilivalidatorJar().toString(),
                "--models", InterlisToolDefaults.MODEL_NAME,
                "--modeldir", ProcessBuilderSupport.absoluteModelDir(),
                xtf.toAbsolutePath().normalize().toString()));
        ProcessBuilderSupport.ProcessResult result = ProcessBuilderSupport.run(command);
        return result.exitCode() == 0
                ? ValidationResult.valid(result.output())
                : ValidationResult.invalid(result.exitCode(), result.output());
    }
}
