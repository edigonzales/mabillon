package guru.interlis.mabillon.interlis;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class InterlisToolDefaults {

    public static final String ILI2PG_VERSION = "5.5.1";
    public static final String ILI2C_VERSION = "5.6.8";
    public static final String ILIVALIDATOR_VERSION = "1.15.0";
    public static final Path MODEL = Path.of("model/SO_AGI_GEVER_20260707.ili");
    public static final String MODEL_NAME = "SO_AGI_GEVER_20260707";
    public static final String MODEL_DIR = "model;http://models.interlis.ch/;http://models.geo.admin.ch/";

    private InterlisToolDefaults() {
    }

    public static Path model() {
        String override = System.getenv("MABILLON_MODEL");
        return override == null || override.isBlank() ? MODEL : Path.of(override);
    }

    public static String modelDir() {
        String override = System.getenv("MABILLON_MODEL_DIR");
        return override == null || override.isBlank() ? MODEL_DIR : override;
    }

    public static String absoluteModelDir() {
        return String.join(";", modelDirectories());
    }

    public static ArrayList<String> modelDirectories() {
        String[] configured = modelDir().split(";", -1);
        ArrayList<String> result = new ArrayList<>(configured.length);
        Path root = Path.of(System.getenv().getOrDefault("MABILLON_ROOT", "."))
                .toAbsolutePath().normalize();
        for (String entry : configured) {
            if (entry.isBlank() || entry.startsWith("http://") || entry.startsWith("https://")) {
                result.add(entry);
                continue;
            }
            Path path = Path.of(entry);
            result.add((path.isAbsolute() ? path : root.resolve(path)).normalize().toString());
        }
        return result;
    }
}
