package guru.interlis.mabillon.interlis;

import java.nio.file.Path;

public record ImportXtfRequest(Path xtf, ImportScope scope, boolean importTid, boolean importBid) {
}
