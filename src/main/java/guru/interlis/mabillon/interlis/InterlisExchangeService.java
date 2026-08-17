package guru.interlis.mabillon.interlis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.springframework.stereotype.Service;

@Service
public final class InterlisExchangeService {

    private final XtfValidator xtfValidator;
    private final Ili2pgRunner ili2pgRunner;
    private final AuthorizationService authorizationService;

    public InterlisExchangeService(
            XtfValidator xtfValidator,
            Ili2pgRunner ili2pgRunner,
            AuthorizationService authorizationService) {
        this.xtfValidator = xtfValidator;
        this.ili2pgRunner = ili2pgRunner;
        this.authorizationService = authorizationService;
    }

    public ExchangeResult importCatalog(Path xtf) {
        return importTopic(ImportScope.CATALOG, xtf);
    }

    public ExchangeResult importMasterData(Path xtf) {
        return importTopic(ImportScope.MASTER_DATA, xtf);
    }

    public ExchangeResult importBusinessData(Path xtf) {
        return importTopic(ImportScope.BUSINESS_DATA, xtf);
    }

    public Path exportCatalog(ExportSelection selection) {
        return exportTopic(ImportScope.CATALOG, selection);
    }

    public Path exportMasterData(ExportSelection selection) {
        return exportTopic(ImportScope.MASTER_DATA, selection);
    }

    public Path exportBusinessData(ExportSelection selection) {
        return exportTopic(ImportScope.BUSINESS_DATA, selection);
    }

    public ValidationReport validateTopic(TopicSelection selection) {
        authorizationService.require(permissionFor(selection.scope()));
        return ValidationReport.from(xtfValidator.validate(selection.xtf().toAbsolutePath().normalize()));
    }

    private ExchangeResult importTopic(ImportScope scope, Path xtf) {
        authorizationService.require(permissionFor(scope));
        Path input = requireInput(xtf);
        ValidationReport validation = ValidationReport.from(xtfValidator.validate(input));
        if (!validation.valid()) {
            return ExchangeResult.rejected(scope, input, validation);
        }
        XtfTopicInspector.Inspection inspection = XtfTopicInspector.inspect(input);
        if (inspection.error() != null || !inspection.contains(scope)) {
            String reason = inspection.error() == null
                    ? "Das XTF enthält das erwartete Topic " + scope.label() + " nicht."
                    : inspection.error();
            return ExchangeResult.rejected(scope, input,
                    new ValidationReport(false, 2, reason));
        }

        Ili2pgResult importResult = ili2pgRunner.importXtf(new ImportXtfRequest(input, scope, true, true));
        if (!importResult.successful()) {
            return ExchangeResult.imported(scope, input, validation, importResult);
        }

        Ili2pgResult postImportValidation = ili2pgRunner.validate(new ValidateRequest(scope));
        if (postImportValidation.successful()) {
            return ExchangeResult.imported(scope, input, validation, importResult);
        }

        String diagnostics = joinDiagnostics(importResult.diagnostics(),
                "Post-Import-Validierung fehlgeschlagen: " + postImportValidation.diagnostics());
        Ili2pgResult failedResult = new Ili2pgResult(false, postImportValidation.exitCode(), diagnostics);
        return ExchangeResult.imported(scope, input, validation, failedResult);
    }

    private Path exportTopic(ImportScope scope, ExportSelection selection) {
        authorizationService.require(permissionFor(scope));
        if (selection == null || selection.target() == null) {
            throw new IllegalArgumentException("Export-Ziel ist erforderlich.");
        }
        Path target = selection.target().toAbsolutePath().normalize();
        Ili2pgResult exportResult = ili2pgRunner.exportXtf(
                new ExportXtfRequest(target, scope, selection.basketIds()));
        if (!exportResult.successful() || !Files.isRegularFile(target)) {
            deleteFailedExport(target);
            throw new InterlisExchangeException("XTF-Export fehlgeschlagen: " + exportResult.diagnostics());
        }

        ValidationReport validation = ValidationReport.from(xtfValidator.validate(target));
        if (!validation.valid()) {
            deleteFailedExport(target);
            throw new InterlisExchangeException("Erzeugtes XTF ist nicht valide: " + validation.diagnostics());
        }
        return target;
    }

    private Permission permissionFor(ImportScope scope) {
        return switch (scope) {
            case CATALOG -> Permission.MANAGE_CATALOGS;
            case MASTER_DATA -> Permission.MANAGE_MASTERDATA;
            case BUSINESS_DATA -> Permission.EDIT_GESCHAEFT;
        };
    }

    private static Path requireInput(Path xtf) {
        if (xtf == null || !Files.isRegularFile(xtf)) {
            throw new IllegalArgumentException("XTF-Datei nicht gefunden: " + xtf);
        }
        return xtf.toAbsolutePath().normalize();
    }

    private static String joinDiagnostics(String first, String second) {
        List<String> parts = new ArrayList<>();
        if (first != null && !first.isBlank()) {
            parts.add(first);
        }
        if (second != null && !second.isBlank()) {
            parts.add(second);
        }
        return String.join(System.lineSeparator(), parts);
    }

    private static void deleteFailedExport(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException failure) {
            throw new InterlisExchangeException("Fehlerhafter Export konnte nicht entfernt werden: " + target, failure);
        }
    }
}
