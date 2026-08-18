package guru.interlis.mabillon.interlis;

import java.nio.file.Files;
import java.nio.file.Path;

import ch.ehi.basics.settings.Settings;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;
import ch.ehi.ili2pg.PgMain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class JavaApiIli2pgRunner implements Ili2pgRunner {

    private final String dbHost;
    private final String dbPort;
    private final String dbDatabase;
    private final String dbUser;
    private final String dbPassword;
    private final String dbSchema;

    public JavaApiIli2pgRunner(
            @Value("${mabillon.interlis.db-host:localhost}") String dbHost,
            @Value("${mabillon.interlis.db-port:5432}") String dbPort,
            @Value("${mabillon.interlis.db-database:mabillon}") String dbDatabase,
            @Value("${mabillon.interlis.db-user:mabillon}") String dbUser,
            @Value("${mabillon.interlis.db-password:mabillon}") String dbPassword,
            @Value("${mabillon.interlis.db-schema:mabillon}") String dbSchema) {
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbDatabase = dbDatabase;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.dbSchema = dbSchema;
    }

    @Override
    public Ili2pgResult schemaImport(SchemaImportRequest request) {
        requireFile(request.iliModel(), "INTERLIS-Modell");
        Config config = baseConfig(Config.FC_SCHEMAIMPORT);
        config.setXtffile(request.iliModel().toAbsolutePath().normalize().toString());
        config.setCreateFk(Config.CREATE_FK_YES);
        config.setCreateFkIdx(Config.CREATE_FKIDX_YES);
        config.setCreateUniqueConstraints(true);
        config.setCreateNumChecks(true);
        config.setCreateTextChecks(true);
        config.setCreateDateTimeChecks(true);
        config.setCreateMetaInfo(true);
        // Deliberately no createMandatoryChecks: ordinary MANDATORY columns are
        // mapped to NOT NULL. Phase 11.5 verifies Mabillon does not need the
        // additional mandatory CHECK constraints.
        if (request.createTidCol()) {
            config.setTidHandling(Config.TID_HANDLING_PROPERTY);
        }
        if (request.createBasketCol()) {
            config.setBasketHandling(Config.BASKET_HANDLING_READWRITE);
        }
        config.setSetupPgExt(true);
        return execute(config, false, "Schemaimport");
    }

    @Override
    public Ili2pgResult importXtf(ImportXtfRequest request) {
        requireFile(request.xtf(), "XTF-Datei");
        Config config = baseConfig(Config.FC_IMPORT);
        config.setXtffile(request.xtf().toAbsolutePath().normalize().toString());
        config.setImportTid(request.importTid());
        config.setImportBid(request.importBid());
        return execute(config, true, "Import");
    }

    @Override
    public Ili2pgResult exportXtf(ExportXtfRequest request) {
        Path target = request.target().toAbsolutePath().normalize();
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception failure) {
            return failure("Export-Ziel konnte nicht vorbereitet werden", failure);
        }
        Config config = baseConfig(Config.FC_EXPORT);
        config.setXtffile(target.toString());
        config.setExportTid(true);
        config.setModels(InterlisToolDefaults.MODEL_NAME);
        config.setTopics(request.scope().qualifiedTopic());
        if (!request.basketIds().isEmpty()) {
            config.setBaskets(String.join(",", request.basketIds()));
        }
        return execute(config, true, "Export");
    }

    @Override
    public Ili2pgResult validate(ValidateRequest request) {
        Config config = baseConfig(Config.FC_VALIDATE);
        config.setModels(InterlisToolDefaults.MODEL_NAME);
        config.setTopics(request.scope().qualifiedTopic());
        return execute(config, true, "Datenbankvalidierung");
    }

    private Config baseConfig(int function) {
        Config config = new Config();
        PgMain main = new PgMain();
        main.initConfig(config);
        config.setAppSettings(new Settings());
        config.setFunction(function);
        config.setDbhost(dbHost);
        config.setDbport(dbPort);
        config.setDbdatabase(dbDatabase);
        config.setDburl("jdbc:postgresql://%s:%s/%s".formatted(dbHost, dbPort, dbDatabase));
        config.setDbusr(dbUser);
        config.setDbpwd(dbPassword);
        config.setDbschema(dbSchema);
        config.setModeldir(InterlisToolDefaults.absoluteModelDir());
        return config;
    }

    private Ili2pgResult execute(Config config, boolean readDbSettings, String operation) {
        try {
            if (readDbSettings) {
                Ili2db.readSettingsFromDb(config);
            }
            Ili2db.run(config, null);
            return new Ili2pgResult(true, 0, operation + " erfolgreich.");
        } catch (Exception failure) {
            return failure(operation + " fehlgeschlagen", failure);
        }
    }

    private static Ili2pgResult failure(String prefix, Exception failure) {
        String message = failure.getMessage();
        return new Ili2pgResult(false, 1,
                prefix + ": " + (message == null || message.isBlank() ? failure.getClass().getSimpleName() : message));
    }

    private static void requireFile(Path path, String label) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException(label + " nicht gefunden: " + path);
        }
    }
}
