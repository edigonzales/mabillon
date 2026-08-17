package guru.interlis.mabillon.archivierung;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArchivePathConfiguration {

    private final Path sipRoot;
    private final Path xsdRoot;

    public ArchivePathConfiguration(
            @Value("${mabillon.archive.sip-root:${java.io.tmpdir}/mabillon-sips}") String sipRoot,
            @Value("${mabillon.archive.ech-0160-xsd-root:docs/archive/profiles/ech-0160-1.3.0/xsd}") String xsdRoot) {
        this.sipRoot = Path.of(sipRoot).toAbsolutePath().normalize();
        this.xsdRoot = Path.of(xsdRoot).toAbsolutePath().normalize();
    }

    public Path sipRoot() {
        return sipRoot;
    }

    public Path xsdRoot() {
        return xsdRoot;
    }
}
