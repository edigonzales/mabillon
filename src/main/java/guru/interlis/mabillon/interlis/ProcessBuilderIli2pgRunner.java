package guru.interlis.mabillon.interlis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ProcessBuilderIli2pgRunner implements Ili2pgRunner {

    private final String dbHost;
    private final String dbPort;
    private final String dbDatabase;
    private final String dbUser;
    private final String dbPassword;
    private final String dbSchema;

    public ProcessBuilderIli2pgRunner(
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
        List<String> command = base("--schemaimport");
        command.addAll(List.of("--dbschema", dbSchema,
                "--createFk", "--createFkIdx", "--createUnique",
                "--createMandatoryChecks", "--createNumChecks", "--createTextChecks",
                "--createDateTimeChecks", "--createMetaInfo"));
        if (request.createTidCol()) {
            command.add("--createTidCol");
        }
        if (request.createBasketCol()) {
            command.add("--createBasketCol");
        }
        command.addAll(List.of("--modeldir", ProcessBuilderSupport.absoluteModelDir()));
        command.add(request.iliModel().toAbsolutePath().normalize().toString());
        return execute(command);
    }

    @Override
    public Ili2pgResult importXtf(ImportXtfRequest request) {
        requireFile(request.xtf(), "XTF-Datei");
        List<String> command = base("--import", "--dbschema", dbSchema);
        if (request.importTid()) {
            command.add("--importTid");
        }
        if (request.importBid()) {
            command.add("--importBid");
        }
        command.addAll(List.of("--modeldir", ProcessBuilderSupport.absoluteModelDir(),
                request.xtf().toAbsolutePath().normalize().toString()));
        return execute(command);
    }

    @Override
    public Ili2pgResult exportXtf(ExportXtfRequest request) {
        Path target = request.target().toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (Exception failure) {
                return new Ili2pgResult(false, -1, "Export-Ziel konnte nicht vorbereitet werden: " + failure.getMessage());
            }
        }
        List<String> command = base("--export", "--dbschema", dbSchema,
                "--exportTid", "--modeldir", ProcessBuilderSupport.absoluteModelDir(),
                "--models", InterlisToolDefaults.MODEL_NAME,
                "--topics", request.scope().qualifiedTopic());
        if (!request.basketIds().isEmpty()) {
            command.addAll(List.of("--baskets", String.join(",", request.basketIds())));
        }
        command.add(target.toString());
        return execute(command);
    }

    @Override
    public Ili2pgResult validate(ValidateRequest request) {
        List<String> command = base("--validate", "--dbschema", dbSchema,
                "--modeldir", ProcessBuilderSupport.absoluteModelDir(),
                "--models", InterlisToolDefaults.MODEL_NAME,
                "--topics", request.scope().qualifiedTopic());
        return execute(command);
    }

    private List<String> base(String... operation) {
        List<String> command = new ArrayList<>(List.of("java", "-jar", InterlisToolDefaults.ili2pgJar().toString()));
        command.addAll(List.of(operation));
        command.addAll(List.of("--dbhost", dbHost, "--dbport", dbPort, "--dbdatabase", dbDatabase,
                "--dbusr", dbUser, "--dbpwd", dbPassword));
        return command;
    }

    private Ili2pgResult execute(List<String> command) {
        ProcessBuilderSupport.ProcessResult result = ProcessBuilderSupport.run(command);
        return new Ili2pgResult(result.exitCode() == 0, result.exitCode(), result.output());
    }

    private static void requireFile(Path path, String label) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException(label + " nicht gefunden: " + path);
        }
    }
}
