package guru.interlis.mabillon.unterlage;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class UnterlageQueryService {

    private final CayenneUnitOfWork unitOfWork;

    public UnterlageQueryService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public UnterlageView get(UUID tid) {
        return unitOfWork.read(context -> {
            Unterlage value = find(context, tid);
            if (value == null) {
                throw new IllegalArgumentException("Unbekannte Unterlage: " + tid);
            }
            return toView(value);
        });
    }

    public List<UnterlageView> forDossier(DossierNumber number) {
        return unitOfWork.read(context -> {
            Dossier dossier = ObjectSelect.query(Dossier.class)
                    .where(Dossier.DOSSIERNUMMER.eq(number.value())).selectFirst(context);
            if (dossier == null) {
                throw new IllegalArgumentException("Unbekanntes Dossier: " + number.value());
            }
            return dossier.getUnterlages().stream()
                    .sorted(Comparator.comparing(Unterlage::getTitel))
                    .map(UnterlageQueryService::toView)
                    .toList();
        });
    }

    public List<UnterlageView> forGeschaeft(GeschaeftNumber number) {
        return unitOfWork.read(context -> {
            Geschaeft business = ObjectSelect.query(Geschaeft.class)
                    .where(Geschaeft.GESCHAEFTSNUMMER.eq(number.value())).selectFirst(context);
            if (business == null) {
                throw new IllegalArgumentException("Unbekanntes Geschäft: " + number.value());
            }
            return business.getUnterlages().stream()
                    .sorted(Comparator.comparing(Unterlage::getTitel))
                    .map(UnterlageQueryService::toView)
                    .toList();
        });
    }

    private Unterlage find(org.apache.cayenne.ObjectContext context, UUID tid) {
        return ObjectSelect.query(Unterlage.class).where(Unterlage.T_ILI_TID.eq(tid)).selectFirst(context);
    }

    static UnterlageView toView(Unterlage value) {
        return new UnterlageView(
                value.getTIliTid(), value.getTitel(),
                value.getUnterlagentyp() == null ? null : value.getUnterlagentyp().getAcode(),
                value.getUnterlagentyp() == null ? null : value.getUnterlagentyp().getAname(),
                value.getAstatus(), value.getDossier() == null ? null : value.getDossier().getDossiernummer(),
                value.getGeschaeft() == null ? null : value.getGeschaeft().getGeschaeftsnummer(),
                value.getUnterlagendatum(), value.getEingangsdatum(), value.getAusgangsdatum(),
                value.getRegistriertam(), value.isAktenrelevant(), value.getDateiname(), value.getMimetype(),
                value.getDateigroesse(), value.getStorageuri(), value.getHashsha256(), value.getDateiformat(),
                value.getBemerkungen());
    }
}
