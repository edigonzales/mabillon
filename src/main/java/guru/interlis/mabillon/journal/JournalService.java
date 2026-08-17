package guru.interlis.mabillon.journal;

import java.time.ZoneId;
import java.util.UUID;

import guru.interlis.mabillon.persistence.cayenne.Benutzer;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Ereignis;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class JournalService {

    public void record(ObjectContext context, JournalCommand command) {
        Ereignis event = context.newObject(Ereignis.class);
        event.setObjekttyp(command.objektTyp().name());
        event.setObjektid(command.objektId());
        event.setTyp(command.typ().name());
        event.setZeitpunkt(command.timestamp().atZone(ZoneId.systemDefault()).toLocalDateTime());
        event.setBemerkung(command.bemerkung());
        event.setTIliTid(UUID.randomUUID());
        event.setTBasket(journalBasket(context));
        Benutzer actor = ObjectSelect.query(Benutzer.class)
                .where(Benutzer.USERNAME.eq(command.actorId().value()))
                .selectFirst(context);
        if (actor == null) {
            throw new IllegalStateException("Journalakteur ist kein fachlicher Benutzer: " + command.actorId().value());
        }
        event.setBenutzer(actor);
    }

    private long journalBasket(ObjectContext context) {
        Ereignis existingEvent = ObjectSelect.query(Ereignis.class).selectFirst(context);
        if (existingEvent != null) {
            return existingEvent.getTBasket();
        }
        Geschaeft existingBusiness = ObjectSelect.query(Geschaeft.class).selectFirst(context);
        if (existingBusiness != null) {
            return existingBusiness.getTBasket();
        }
        Dossier existingDossier = ObjectSelect.query(Dossier.class).selectFirst(context);
        if (existingDossier != null) {
            return existingDossier.getTBasket();
        }
        throw new IllegalStateException("Kein Geschäftsdaten-Basket für das Journal vorhanden.");
    }
}
