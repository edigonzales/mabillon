package guru.interlis.mabillon.interlis;

import java.nio.file.Path;

public final class InterlisToolDefaults {

    public static final Path ILI2PG_JAR = Path.of("/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar");
    public static final Path ILI2C_JAR = Path.of("/Users/stefan/apps/ili2c-5.6.8/ili2c.jar");
    public static final Path ILIVALIDATOR_JAR = Path.of("/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar");

    public static final String ILI2PG_VERSION = "5.5.2";
    public static final String ILI2C_VERSION = "5.6.8";
    public static final String ILIVALIDATOR_VERSION = "1.15.0";
    public static final Path MODEL = Path.of("model/SO_AGI_GEVER_20260707.ili");
    public static final String MODEL_NAME = "SO_AGI_GEVER_20260707";
    public static final String MODEL_DIR = "model;http://models.interlis.ch/;http://models.geo.admin.ch/";

    private InterlisToolDefaults() {
    }

    public static Path ili2pgJar() {
        return override("ILI2PG_JAR", ILI2PG_JAR);
    }

    public static Path ili2cJar() {
        return override("ILI2C_JAR", ILI2C_JAR);
    }

    public static Path ilivalidatorJar() {
        return override("ILIVALIDATOR_JAR", ILIVALIDATOR_JAR);
    }

    public static Path model() {
        String override = System.getenv("MABILLON_MODEL");
        return override == null || override.isBlank() ? MODEL : Path.of(override);
    }

    public static String modelDir() {
        String override = System.getenv("MABILLON_MODEL_DIR");
        return override == null || override.isBlank() ? MODEL_DIR : override;
    }

    private static Path override(String variable, Path fallback) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? fallback : Path.of(value);
    }
}
