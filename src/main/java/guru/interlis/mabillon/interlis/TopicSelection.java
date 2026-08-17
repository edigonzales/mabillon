package guru.interlis.mabillon.interlis;

import java.nio.file.Path;
import java.util.Objects;

public record TopicSelection(ImportScope scope, Path xtf) {

    public TopicSelection {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(xtf, "xtf");
    }
}
