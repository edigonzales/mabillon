package guru.interlis.mabillon.beteiligung;

import java.time.LocalDate;
import java.util.UUID;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import org.springframework.stereotype.Controller;
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
}
