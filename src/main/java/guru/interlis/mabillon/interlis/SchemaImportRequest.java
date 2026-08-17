package guru.interlis.mabillon.interlis;

import java.nio.file.Path;

public record SchemaImportRequest(Path iliModel, boolean createTidCol, boolean createBasketCol) {
}
