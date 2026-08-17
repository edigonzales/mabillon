package guru.interlis.mabillon.query;

import java.util.List;

public record SearchPage<T>(List<T> items, int page, int size, long totalElements) {

    public SearchPage {
        items = List.copyOf(items);
        if (page < 0 || size < 1 || totalElements < 0) {
            throw new IllegalArgumentException("Ungültige Seitendaten.");
        }
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }
}
