package guru.interlis.mabillon.persistence;

import org.apache.cayenne.runtime.CayenneRuntime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CayenneConfiguration {

    @Bean(destroyMethod = "shutdown")
    CayenneRuntime cayenneRuntime(
            @Value("${mabillon.cayenne.url:jdbc:postgresql://localhost:55432/mabillon}") String url,
            @Value("${mabillon.cayenne.username:mabillon}") String username,
            @Value("${mabillon.cayenne.password:}") String password,
            @Value("${mabillon.cayenne.min-connections:2}") int minConnections,
            @Value("${mabillon.cayenne.max-connections:12}") int maxConnections,
            @Value("${mabillon.cayenne.max-queue-wait-ms:5000}") long maxQueueWaitMs) {
        if (minConnections < 1 || maxConnections < minConnections || maxQueueWaitMs < 0) {
            throw new IllegalArgumentException("Ungültige Cayenne-Poolkonfiguration.");
        }
        return CayenneRuntime.builder()
                .addConfig("cayenne/cayenne-project.xml")
                .jdbcDriver("org.postgresql.Driver")
                .url(url)
                .user(username)
                .password(password)
                .minConnections(minConnections)
                .maxConnections(maxConnections)
                .maxQueueWaitTime(maxQueueWaitMs)
                .build();
    }
}
