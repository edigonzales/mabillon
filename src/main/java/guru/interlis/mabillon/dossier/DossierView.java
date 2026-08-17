package guru.interlis.mabillon.dossier;

import java.util.List;
import java.util.UUID;

public record DossierView(
        String number,
        String title,
        String description,
        String status,
        List<GeschaeftSummary> geschaefte,
        List<UnterlageSummary> unterlagen) {

    public DossierView {
        geschaefte = List.copyOf(geschaefte);
        unterlagen = List.copyOf(unterlagen);
    }

    public record GeschaeftSummary(String number, String title, String status) {
    }

    public record UnterlageSummary(UUID tid, String title, String filename, String status, boolean downloadable) {

        public UnterlageSummary(String title, String filename, String status) {
            this(null, title, filename, status, false);
        }

        public UnterlageSummary(UUID tid, String title, String filename, String status) {
            this(tid, title, filename, status, false);
        }
    }
}
