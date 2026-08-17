package guru.interlis.mabillon.search;

import java.util.List;

public record GlobalSearchResult(List<GlobalSearchHit> items, int page, int size, long totalElements) {

    public GlobalSearchResult {
        items = List.copyOf(items);
        if (page < 0 || size < 1 || totalElements < 0) {
            throw new IllegalArgumentException("Ungültige Suchresultate.");
        }
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }
}
