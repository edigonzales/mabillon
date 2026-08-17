package guru.interlis.mabillon.quality;

public record QualityFinding(
        String ruleCode,
        QualitySeverity severity,
        String objectType,
        String objectId,
        String message) {
}
