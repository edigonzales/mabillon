package guru.interlis.mabillon.catalog;

import java.util.List;

public record CatalogGroupView(CatalogType type, List<CatalogEntryView> entries) {

    public CatalogGroupView {
        entries = List.copyOf(entries);
    }
}
