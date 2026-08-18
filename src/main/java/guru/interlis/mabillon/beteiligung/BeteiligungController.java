package guru.interlis.mabillon.beteiligung;

import java.time.LocalDate;
import java.util.UUID;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/beteiligungen")
public final class BeteiligungController {

    private final BeteiligungService beteiligungService;

    public BeteiligungController(BeteiligungService beteiligungService) {
        this.beteiligungService = beteiligungService;
    }

    @PostMapping
    public String add(
            @RequestParam String geschaeftNumber,
            @RequestParam UUID beteiligterTid,
            @RequestParam String rollenCode,
            @RequestParam(required = false) String rollenbezeichnung,
            @RequestParam(required = false) LocalDate gueltigVon,
            @RequestParam(required = false) LocalDate gueltigBis,
            @RequestParam(required = false) String bemerkung) {
        BeteiligungView created = beteiligungService.add(new AddBeteiligungCommand(
                GeschaeftNumber.parse(geschaeftNumber), beteiligterTid, rollenCode, rollenbezeichnung,
                gueltigVon, gueltigBis, bemerkung));
        return "redirect:/geschaefte/" + created.geschaeftsnummer();
    }

    @PostMapping("/{tid}")
    String update(
            @PathVariable UUID tid,
            @RequestParam(required = false) String rollenbezeichnung,
            @RequestParam(required = false) LocalDate gueltigVon,
            @RequestParam(required = false) LocalDate gueltigBis,
            @RequestParam(required = false) String bemerkung) {
        BeteiligungView updated = beteiligungService.update(new UpdateBeteiligungCommand(
                tid, rollenbezeichnung, gueltigVon, gueltigBis, bemerkung));
        return "redirect:/geschaefte/" + updated.geschaeftsnummer();
    }

    @PostMapping("/{tid}/end")
    String end(
            @PathVariable UUID tid,
            @RequestParam LocalDate endDate,
            @RequestParam String geschaeftNumber) {
        beteiligungService.end(new EndBeteiligungCommand(tid, endDate));
        return "redirect:/geschaefte/" + GeschaeftNumber.parse(geschaeftNumber).value();
    }
}
