package guru.interlis.mabillon.interlis;

public enum ImportScope {
    CATALOG("Kataloge"),
    MASTER_DATA("Stammdaten"),
    BUSINESS_DATA("Geschaeftsdaten");

    private final String topic;

    ImportScope(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }

    public String qualifiedTopic() {
        return "SO_AGI_GEVER_20260707." + topic;
    }

    public String label() {
        return switch (this) {
            case CATALOG -> "Kataloge";
            case MASTER_DATA -> "Stammdaten";
            case BUSINESS_DATA -> "Geschäftsdaten";
        };
    }
}
