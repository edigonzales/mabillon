package guru.interlis.mabillon.interlis;

public interface Ili2pgRunner {
    Ili2pgResult schemaImport(SchemaImportRequest request);

    Ili2pgResult importXtf(ImportXtfRequest request);

    Ili2pgResult exportXtf(ExportXtfRequest request);

    Ili2pgResult validate(ValidateRequest request);
}
