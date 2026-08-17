package guru.interlis.mabillon.observability;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.cayenne.runtime.CayenneRuntime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("mabillon")
public final class MabillonHealthIndicator implements HealthIndicator {

    private final CayenneRuntime cayenneRuntime;
    private final Path storageRoot;

    public MabillonHealthIndicator(
            CayenneRuntime cayenneRuntime,
            @Value("${mabillon.storage.root:build/document-storage}") String storageRoot) {
        this.cayenneRuntime = cayenneRuntime;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public Health health() {
        try (var connection = cayenneRuntime.getDataSource().getConnection()) {
            boolean database = connection.isValid(2);
            boolean storage = Files.isDirectory(storageRoot)
                    || Files.isWritable(storageRoot.getParent() == null ? storageRoot : storageRoot.getParent());
            if (database && storage) {
                return Health.up()
                        .withDetail("database", "reachable")
                        .withDetail("storage", storageRoot.toString())
                        .build();
            }
            return Health.down()
                    .withDetail("database", database ? "reachable" : "unavailable")
                    .withDetail("storage", storage ? "available" : "unavailable")
                    .build();
        } catch (Exception failure) {
            return Health.down().withDetail("reason", "dependency unavailable").build();
        }
    }
}
