package guru.interlis.mabillon.archivierung;

import java.util.Comparator;
import java.util.List;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.quality.DataQualityService;
import guru.interlis.mabillon.query.SearchPage;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class AussonderungQueryService {

    private final CayenneUnitOfWork unitOfWork;
    private final DataQualityService dataQualityService;
    private final AuthorizationService authorizationService;

    public AussonderungQueryService(
            CayenneUnitOfWork unitOfWork,
            DataQualityService dataQualityService,
            AuthorizationService authorizationService) {
        this.unitOfWork = unitOfWork;
        this.dataQualityService = dataQualityService;
        this.authorizationService = authorizationService;
    }

    public SearchPage<AussonderungView> eligible(int page, int size) {
        authorizationService.require(Permission.MANAGE_ARCHIVE_DELIVERY);
        return unitOfWork.read(context -> {
            List<AussonderungView> values = ObjectSelect.query(Dossier.class).select(context).stream()
                    .filter(dossier -> "Geschlossen".equalsIgnoreCase(dossier.getAstatus()))
                    .filter(dossier -> dossier.getArchivierungs().stream().noneMatch(archive ->
                            "Uebernommen".equalsIgnoreCase(archive.getAstatus())
                                    || "Vernichtet".equalsIgnoreCase(archive.getAstatus())))
                    .map(this::toViewIfQualityValid)
                    .flatMap(java.util.Optional::stream)
                    .sorted(Comparator.comparing(AussonderungView::dossierNumber))
                    .toList();
            if (page < 0 || size < 1) {
                throw new IllegalArgumentException("Ungültige Seitendaten.");
            }
            int from = Math.min(page * size, values.size());
            int to = Math.min(from + size, values.size());
            return new SearchPage<>(values.subList(from, to), page, size, values.size());
        });
    }

    private java.util.Optional<AussonderungView> toViewIfQualityValid(Dossier dossier) {
        var report = dataQualityService.checkDossier(DossierNumber.parse(dossier.getDossiernummer()));
        if (report.hasErrors()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new AussonderungView(
                dossier.getDossiernummer(), dossier.getTitel(), dossier.getAstatus(),
                dossier.getGeschlossenam() == null ? "" : dossier.getGeschlossenam().toString(),
                dossier.getOrdnungssystemposition() == null ? "" : dossier.getOrdnungssystemposition().getAcode(),
                dossier.getUnterlages().stream().filter(document -> !"Storniert".equalsIgnoreCase(document.getAstatus())).count(),
                report.warningCount()));
    }
}
