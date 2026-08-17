package guru.interlis.mabillon.interlis;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ExportSelection(Path target, List<String> basketIds) {

    public ExportSelection {
        Objects.requireNonNull(target, "target");
        basketIds = basketIds == null ? List.of() : List.copyOf(basketIds);
    }

    public static ExportSelection all(Path target) {
        return new ExportSelection(target, List.of());
    }
}
