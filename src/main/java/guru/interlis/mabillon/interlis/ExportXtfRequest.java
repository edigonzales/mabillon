package guru.interlis.mabillon.interlis;

import java.nio.file.Path;
import java.util.List;

public record ExportXtfRequest(Path target, ImportScope scope, List<String> basketIds) {

    public ExportXtfRequest {
        basketIds = basketIds == null ? List.of() : List.copyOf(basketIds);
    }
}
