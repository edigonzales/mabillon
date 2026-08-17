package guru.interlis.mabillon.quality;

import java.util.List;

public record QualityReport(String objectType, String objectId, List<QualityFinding> findings) {

    public QualityReport {
        findings = List.copyOf(findings);
    }

    public boolean hasErrors() {
        return findings.stream().anyMatch(finding -> finding.severity() == QualitySeverity.ERROR);
    }

    public long errorCount() {
        return findings.stream().filter(finding -> finding.severity() == QualitySeverity.ERROR).count();
    }

    public long warningCount() {
        return findings.stream().filter(finding -> finding.severity() == QualitySeverity.WARNING).count();
    }
}
