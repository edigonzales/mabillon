package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import guru.interlis.mabillon.interlis.ExchangeResult;
import guru.interlis.mabillon.interlis.ExportSelection;
import guru.interlis.mabillon.interlis.ExportXtfRequest;
import guru.interlis.mabillon.interlis.Ili2pgResult;
import guru.interlis.mabillon.interlis.Ili2pgRunner;
import guru.interlis.mabillon.interlis.ImportScope;
import guru.interlis.mabillon.interlis.ImportXtfRequest;
import guru.interlis.mabillon.interlis.InterlisExchangeException;
import guru.interlis.mabillon.interlis.InterlisExchangeService;
import guru.interlis.mabillon.interlis.InterlisModelValidator;
import guru.interlis.mabillon.interlis.ProcessBuilderInterlisModelValidator;
import guru.interlis.mabillon.interlis.ProcessBuilderXtfValidator;
import guru.interlis.mabillon.interlis.SchemaImportRequest;
import guru.interlis.mabillon.interlis.ValidateRequest;
import guru.interlis.mabillon.interlis.ValidationResult;
import guru.interlis.mabillon.interlis.XtfValidator;
import guru.interlis.mabillon.security.ActorId;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.MabillonRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InterlisExchangeServiceTest {

    private static final CurrentActor ADMIN = new CurrentActor() {
        @Override
        public ActorId id() {
            return new ActorId("admin");
        }

        @Override
        public String username() {
            return "admin";
        }

        @Override
        public String displayName() {
            return "Administrator";
        }

        @Override
        public Set<MabillonRole> roles() {
            return EnumSet.of(MabillonRole.ADMIN);
        }
    };

    @TempDir
    Path tempDir;

    @Test
    void invalidInputIsRejectedBeforeIli2pg() throws IOException {
        Path input = Files.writeString(tempDir.resolve("invalid.xtf"), "not xml");
        RecordingRunner runner = new RecordingRunner();
        InterlisExchangeService service = new InterlisExchangeService(
                ignored -> ValidationResult.invalid(1, "invalid"), runner, new AuthorizationService(ADMIN));

        ExchangeResult result = service.importCatalog(input);

        assertThat(result.successful()).isFalse();
        assertThat(result.validation().valid()).isFalse();
        assertThat(runner.importRequest).isNull();
    }

    @Test
    void publicImportAlwaysKeepsTidAndBid() throws IOException {
        Path input = Files.copy(Path.of("model/testdata/03_SO_AGI_GEVER_20260707_testdaten_geschaeftsdaten.xtf"),
                tempDir.resolve("valid.xtf"));
        RecordingRunner runner = new RecordingRunner();
        InterlisExchangeService service = new InterlisExchangeService(
                ignored -> ValidationResult.valid("ok"), runner, new AuthorizationService(ADMIN));

        ExchangeResult result = service.importBusinessData(input);

        assertThat(result.successful()).isTrue();
        assertThat(runner.importRequest).extracting(ImportXtfRequest::importTid, ImportXtfRequest::importBid)
                .containsExactly(true, true);
        assertThat(runner.importRequest.scope()).isEqualTo(ImportScope.BUSINESS_DATA);
        assertThat(runner.validateRequest.scope()).isEqualTo(ImportScope.BUSINESS_DATA);
    }

    @Test
    void requestedTopicMustBePresentBeforeImport() throws IOException {
        Path input = Files.copy(Path.of("model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf"),
                tempDir.resolve("catalog-only.xtf"));
        RecordingRunner runner = new RecordingRunner();
        InterlisExchangeService service = new InterlisExchangeService(
                ignored -> ValidationResult.valid("ok"), runner, new AuthorizationService(ADMIN));

        ExchangeResult result = service.importBusinessData(input);

        assertThat(result.successful()).isFalse();
        assertThat(result.validation().diagnostics()).contains("Geschäftsdaten");
        assertThat(runner.importRequest).isNull();
    }

    @Test
    void invalidExportIsRemovedAndNotReturned() throws IOException {
        RecordingRunner runner = new RecordingRunner();
        XtfValidator validator = path -> {
            try {
                Files.writeString(path, "generated");
            } catch (IOException failure) {
                throw new RuntimeException(failure);
            }
            return ValidationResult.invalid(1, "invalid export");
        };
        InterlisExchangeService service = new InterlisExchangeService(
                validator, runner, new AuthorizationService(ADMIN));
        Path target = tempDir.resolve("failed.xtf");

        assertThatThrownBy(() -> service.exportCatalog(ExportSelection.all(target)))
                .isInstanceOf(InterlisExchangeException.class)
                .hasMessageContaining("nicht valide");
        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    void localToolAdaptersValidateModelAndPositiveXtf() {
        InterlisModelValidator modelValidator = new ProcessBuilderInterlisModelValidator();
        XtfValidator xtfValidator = new ProcessBuilderXtfValidator();

        assertThat(modelValidator.validate(Path.of("model/SO_AGI_GEVER_20260707.ili")).valid()).isTrue();
        assertThat(xtfValidator.validate(Path.of(
                "model/testdata/01_SO_AGI_GEVER_20260707_testdaten_kataloge.xtf")).valid()).isTrue();
    }

    private static final class RecordingRunner implements Ili2pgRunner {
        private ImportXtfRequest importRequest;
        private ValidateRequest validateRequest;

        @Override
        public Ili2pgResult schemaImport(SchemaImportRequest request) {
            return new Ili2pgResult(true, 0, "ok");
        }

        @Override
        public Ili2pgResult importXtf(ImportXtfRequest request) {
            importRequest = request;
            return new Ili2pgResult(true, 0, "imported");
        }

        @Override
        public Ili2pgResult exportXtf(ExportXtfRequest request) {
            try {
                Files.writeString(request.target(), "generated");
            } catch (IOException failure) {
                return new Ili2pgResult(false, 1, failure.getMessage());
            }
            return new Ili2pgResult(true, 0, "exported");
        }

        @Override
        public Ili2pgResult validate(ValidateRequest request) {
            validateRequest = request;
            return new Ili2pgResult(true, 0, "validated");
        }
    }
}
