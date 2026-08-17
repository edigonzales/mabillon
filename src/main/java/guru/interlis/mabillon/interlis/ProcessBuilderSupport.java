package guru.interlis.mabillon.interlis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

final class ProcessBuilderSupport {

    private ProcessBuilderSupport() {
    }

    static ProcessResult run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output);
        } catch (IOException failure) {
            return new ProcessResult(-1, failure.getMessage() == null ? failure.getClass().getSimpleName()
                    : failure.getMessage());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "Prozess wurde unterbrochen.");
        }
    }

    static String absoluteModelDir() {
        String configured = InterlisToolDefaults.modelDir();
        String[] parts = configured.split(";", -1);
        if (parts.length == 0 || parts[0].isBlank() || parts[0].startsWith("http")) {
            return configured;
        }
        Path local = Path.of(parts[0]);
        if (!local.isAbsolute()) {
            local = Path.of(System.getenv().getOrDefault("MABILLON_ROOT", "."))
                    .toAbsolutePath().normalize().resolve(local).normalize();
        }
        parts[0] = local.toString();
        return String.join(";", parts);
    }

    record ProcessResult(int exitCode, String output) {
    }
}
