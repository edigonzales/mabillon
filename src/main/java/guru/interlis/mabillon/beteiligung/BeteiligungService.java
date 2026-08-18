package guru.interlis.mabillon.beteiligung;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import guru.interlis.mabillon.journal.EreignisObjektTyp;
import guru.interlis.mabillon.journal.EreignisTyp;
import guru.interlis.mabillon.journal.JournalCommand;
import guru.interlis.mabillon.journal.JournalService;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Beteiligter;
import guru.interlis.mabillon.persistence.cayenne.Beteiligung;
import guru.interlis.mabillon.persistence.cayenne.Beteiligungsrolle;
import guru.interlis.mabillon.persistence.cayenne.Geschaeft;
import guru.interlis.mabillon.security.AuthorizationService;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.security.Permission;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class BeteiligungService {

    private final CayenneUnitOfWork unitOfWork;
    private final JournalService journalService;
    private final AuthorizationService authorizationService;
    private final CurrentActor currentActor;
    private final Clock clock;

    public BeteiligungService(
            CayenneUnitOfWork unitOfWork,
            JournalService journalService,
            AuthorizationService authorizationService,
            CurrentActor currentActor,
            Clock clock) {
        this.unitOfWork = unitOfWork;
        this.journalService = journalService;
        this.authorizationService = authorizationService;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    public BeteiligungView add(AddBeteiligungCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        requireValidDates(command.gueltigVon(), command.gueltigBis());
        return unitOfWork.write(context -> {
            Geschaeft business = findBusiness(context, command.geschaeftNumber());
            Beteiligter party = ObjectSelect.query(Beteiligter.class)
                    .where(Beteiligter.T_ILI_TID.eq(command.beteiligterTid())).selectFirst(context);
            Beteiligungsrolle role = ObjectSelect.query(Beteiligungsrolle.class)
                    .where(Beteiligungsrolle.ACODE.eq(command.rollenCode())).selectFirst(context);
            requireOpenBusiness(business);
            requireParty(party);
            if (role == null || !"aktiv".equalsIgnoreCase(role.getAstatus())) {
                throw new IllegalArgumentException("Aktive Beteiligungsrolle fehlt: " + command.rollenCode());
            }
            Beteiligung value = context.newObject(Beteiligung.class);
            value.setGeschaeft(business);
            value.setBeteiligter(party);
            value.setBeteiligungsrolle(role);
            value.setRollenbezeichnung(command.rollenbezeichnung());
            value.setGueltigvon(command.gueltigVon());
            value.setGueltigbis(command.gueltigBis());
            value.setBemerkung(command.bemerkung());
            value.setTBasket(business.getTBasket());
            value.setTIliTid(UUID.randomUUID());
            record(value, EreignisTyp.Zugewiesen, "Beteiligter zugeordnet.", context);
            return toView(value);
        });
    }

    public BeteiligungView update(UpdateBeteiligungCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        requireValidDates(command.gueltigVon(), command.gueltigBis());
        return unitOfWork.write(context -> {
            Beteiligung value = find(context, command.tid());
            if (value == null) {
                throw new IllegalArgumentException("Unbekannte Beteiligung: " + command.tid());
            }
            requireOpenBusiness(value.getGeschaeft());
            value.setRollenbezeichnung(command.rollenbezeichnung());
            value.setGueltigvon(command.gueltigVon());
            value.setGueltigbis(command.gueltigBis());
            value.setBemerkung(command.bemerkung());
            record(value, EreignisTyp.Geaendert, "Beteiligung geändert.", context);
            return toView(value);
        });
    }

    public void end(EndBeteiligungCommand command) {
        authorizationService.require(Permission.EDIT_GESCHAEFT);
        unitOfWork.write(context -> {
            Beteiligung value = find(context, command.tid());
            if (value == null) {
                throw new IllegalArgumentException("Unbekannte Beteiligung: " + command.tid());
            }
            requireOpenBusiness(value.getGeschaeft());
            requireValidDates(value.getGueltigvon(), command.endDate());
            value.setGueltigbis(command.endDate());
            record(value, EreignisTyp.Geaendert, "Beteiligung beendet.", context);
        });
    }

    public List<BeteiligungView> listForGeschaeft(GeschaeftNumber number) {
        return unitOfWork.read(context -> {
            Geschaeft business = findBusiness(context, number);
            if (business == null) {
                throw new IllegalArgumentException("Unbekanntes Geschäft: " + number.value());
            }
            return business.getBeteiligungs().stream().map(this::toView).toList();
        });
    }

    private Geschaeft findBusiness(ObjectContext context, GeschaeftNumber number) {
        return ObjectSelect.query(Geschaeft.class)
                .where(Geschaeft.GESCHAEFTSNUMMER.eq(number.value())).selectFirst(context);
    }

    private Beteiligung find(ObjectContext context, UUID tid) {
        return ObjectSelect.query(Beteiligung.class).where(Beteiligung.T_ILI_TID.eq(tid)).selectFirst(context);
    }

    private void requireOpenBusiness(Geschaeft business) {
        if (business == null) {
            throw new IllegalArgumentException("Unbekanntes Geschäft.");
        }
        if ("Abgeschlossen".equalsIgnoreCase(business.getLifecyclestatus())
                || "Archiviert".equalsIgnoreCase(business.getLifecyclestatus())
                || "Vernichtet".equalsIgnoreCase(business.getLifecyclestatus())) {
            throw new IllegalStateException("Geschäft ist nicht mehr bearbeitbar.");
        }
    }

    private void requireParty(Beteiligter party) {
        if (party == null) {
            throw new IllegalArgumentException("Unbekannter Beteiligter.");
        }
    }

    private static void requireValidDates(LocalDate validFrom, LocalDate validUntil) {
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException("Gültig-bis darf nicht vor Gültig-von liegen.");
        }
    }

    private void record(Beteiligung value, EreignisTyp type, String remark, ObjectContext context) {
        journalService.record(context, new JournalCommand(
                EreignisObjektTyp.Beteiligung, value.getTIliTid().toString(), type, remark,
                currentActor.id(), Instant.now(clock)));
    }

    private BeteiligungView toView(Beteiligung value) {
        Beteiligter party = value.getBeteiligter();
        Beteiligungsrolle role = value.getBeteiligungsrolle();
        return new BeteiligungView(value.getTIliTid(), value.getGeschaeft().getGeschaeftsnummer(),
                party.getTIliTid(), party.getAname(), role.getAcode(), role.getAname(),
                value.getRollenbezeichnung(), value.getGueltigvon(), value.getGueltigbis(), value.getBemerkung());
    }
}
