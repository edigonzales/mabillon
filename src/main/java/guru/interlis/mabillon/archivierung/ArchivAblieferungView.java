package guru.interlis.mabillon.archivierung;

import java.util.List;

public record ArchivAblieferungView(
        String deliveryNumber,
        String title,
        String status,
        String archiveRecipient,
        List<DossierArchiveView> dossiers,
        List<SippaketView> sipPackages) {

    public ArchivAblieferungView {
        dossiers = List.copyOf(dossiers);
        sipPackages = List.copyOf(sipPackages);
    }

    public record DossierArchiveView(
            String dossierNumber,
            String title,
            String dossierStatus,
            String archiveStatus) {
    }
}
