package guru.interlis.mabillon.archivierung;

import java.nio.file.Path;

public record GeneratedSip(Path path, long size, String sha256) {
}
