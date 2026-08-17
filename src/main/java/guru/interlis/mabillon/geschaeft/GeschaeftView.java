package guru.interlis.mabillon.geschaeft;

import java.util.List;
import java.util.UUID;

public record GeschaeftView(
        String number,
        String title,
        String shortDescription,
        String lifecycleStatus,
        String dossierNumber,
        List<UnterlageSummary> unterlagen,
        String geschaeftsartCode,
        String processStatusCode,
        String processStatusName,
        String resultStatusCode) {

    public GeschaeftView(
            String number,
            String title,
            String shortDescription,
            String lifecycleStatus,
            String dossierNumber,
            List<UnterlageSummary> unterlagen) {
        this(number, title, shortDescription, lifecycleStatus, dossierNumber, unterlagen,
                null, null, null, null);
    }

    public GeschaeftView {
        unterlagen = List.copyOf(unterlagen);
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
