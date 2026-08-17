package guru.interlis.mabillon.search;

import java.util.UUID;

public record GlobalSearchHit(
        String objectType,
        UUID tid,
        String identifier,
        String title,
        String context,
        String href) {
}
