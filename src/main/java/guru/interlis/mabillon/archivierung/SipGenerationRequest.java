package guru.interlis.mabillon.archivierung;

import java.nio.file.Path;
import java.util.Objects;

import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;

public record SipGenerationRequest(
        ArchivAblieferungNumber deliveryNumber,
        SipProfile profile,
        Path targetDirectory) {

    public SipGenerationRequest {
        Objects.requireNonNull(deliveryNumber, "deliveryNumber");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(targetDirectory, "targetDirectory");
    }
}
