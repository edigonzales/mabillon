package guru.interlis.mabillon.journal;

import java.util.Comparator;
import java.util.List;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Ereignis;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class JournalQueryService {

    private final CayenneUnitOfWork unitOfWork;

    public JournalQueryService(CayenneUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public List<JournalEntryView> findForObject(EreignisObjektTyp type, String objectId, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit muss positiv sein.");
        }
        return unitOfWork.read(context -> ObjectSelect.query(Ereignis.class).select(context).stream()
                .filter(event -> type.name().equals(event.getObjekttyp()) && objectId.equals(event.getObjektid()))
                .sorted(Comparator.comparing(Ereignis::getZeitpunkt).reversed())
                .limit(limit)
                .map(this::toView)
                .toList());
    }

    public List<JournalEntryView> findForGeschaeft(GeschaeftNumber number, int limit) {
        return findForObject(EreignisObjektTyp.Geschaeft, number.value(), limit);
    }

    public List<JournalEntryView> findForDossier(DossierNumber number, int limit) {
        return findForObject(EreignisObjektTyp.Dossier, number.value(), limit);
    }

    private JournalEntryView toView(Ereignis event) {
        return new JournalEntryView(
                EreignisObjektTyp.valueOf(event.getObjekttyp()),
                event.getObjektid(),
                EreignisTyp.valueOf(event.getTyp()),
                event.getZeitpunkt(),
                event.getBenutzer() == null ? null : event.getBenutzer().getUsername(),
                event.getBemerkung());
    }
}
