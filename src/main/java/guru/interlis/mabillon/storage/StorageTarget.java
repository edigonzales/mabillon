package guru.interlis.mabillon.storage;

public record StorageTarget(String scope) {

    public StorageTarget {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("Ablagebereich ist erforderlich.");
        }
    }
}
