package guru.interlis.mabillon.geschaeft;

import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import guru.interlis.mabillon.catalog.CatalogService;
import guru.interlis.mabillon.catalog.CatalogType;
import guru.interlis.mabillon.aufgabe.AufgabeQueryService;
import guru.interlis.mabillon.beteiligung.BeteiligungService;
import guru.interlis.mabillon.beteiligung.BeteiligterSearchCriteria;
import guru.interlis.mabillon.beteiligung.BeteiligterService;
import guru.interlis.mabillon.domain.NotFoundException;
import guru.interlis.mabillon.dossier.DossierQueryService;
import guru.interlis.mabillon.dossier.DossierSearchCriteria;
import guru.interlis.mabillon.fachsystem.AddFachsystemReferenzCommand;
import guru.interlis.mabillon.fachsystem.FachsystemReferenzService;
import guru.interlis.mabillon.journal.JournalQueryService;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.stammdaten.BenutzerService;
import guru.interlis.mabillon.stammdaten.OrganisationseinheitService;
import guru.interlis.mabillon.web.HtmxRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/geschaefte")
public final class GeschaeftController {

    private final GeschaeftQueryService queryService;
    private final GeschaeftService geschaeftService;
    private final CatalogService catalogService;
    private final DossierQueryService dossierQueryService;
    private final OrganisationseinheitService organisationseinheitService;
    private final BenutzerService benutzerService;
    private final AufgabeQueryService aufgabeQueryService;
    private final BeteiligungService beteiligungService;
    private final BeteiligterService beteiligterService;
    private final FachsystemReferenzService fachsystemReferenzService;
    private final JournalQueryService journalQueryService;

    public GeschaeftController(
            GeschaeftQueryService queryService,
            GeschaeftService geschaeftService,
            CatalogService catalogService,
            DossierQueryService dossierQueryService,
            OrganisationseinheitService organisationseinheitService,
            BenutzerService benutzerService,
            AufgabeQueryService aufgabeQueryService,
            BeteiligungService beteiligungService,
            BeteiligterService beteiligterService,
            FachsystemReferenzService fachsystemReferenzService,
            JournalQueryService journalQueryService) {
        this.queryService = queryService;
        this.geschaeftService = geschaeftService;
        this.catalogService = catalogService;
        this.dossierQueryService = dossierQueryService;
        this.organisationseinheitService = organisationseinheitService;
        this.benutzerService = benutzerService;
        this.aufgabeQueryService = aufgabeQueryService;
        this.beteiligungService = beteiligungService;
        this.beteiligterService = beteiligterService;
        this.fachsystemReferenzService = fachsystemReferenzService;
        this.journalQueryService = journalQueryService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String processStatus,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String responsible,
            @RequestParam(required = false) String organisation,
            @RequestParam(required = false) LocalDate dueFrom,
            @RequestParam(required = false) LocalDate dueTo,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request,
            Model model) {
        GeschaeftSearchCriteria criteria = new GeschaeftSearchCriteria(
                number, title, type, processStatus, lifecycle, responsible, organisation, dueFrom, dueTo);
        model.addAttribute("searchPage", queryService.search(criteria, page, 20));
        model.addAttribute("criteria", criteria);
        model.addAttribute("types", catalogService.list(CatalogType.GESCHAEFTSART, false));
        model.addAttribute("processStatuses", catalogService.list(CatalogType.PROZESSSTATUS, false));
        model.addAttribute("organisationseinheiten", organisationseinheitService.list(false));
        model.addAttribute("benutzer", benutzerService.list(false));
        model.addAttribute("lifecycles", new String[] {"Eroeffnet", "In_Bearbeitung", "Sistiert", "Abgeschlossen"});
        model.addAttribute("title", "Geschäfte");
        return HtmxRequest.isRequest(request) ? "geschaefte/_list" : "geschaefte/index";
    }

    @GetMapping("/neu")
    public String newForm(Model model) {
        model.addAttribute("dossiers", dossierQueryService.search(
                new DossierSearchCriteria(null, null, null, "Offen", null, null, null), 0, 200).items());
        model.addAttribute("types", catalogService.list(CatalogType.GESCHAEFTSART, false));
        model.addAttribute("organisationseinheiten", organisationseinheitService.list(false));
        model.addAttribute("benutzer", benutzerService.list(false));
        model.addAttribute("title", "Neues Geschäft");
        return "geschaefte/new";
    }

    @PostMapping
    public String open(
            @RequestParam String dossierNumber,
            @RequestParam String title,
            @RequestParam(required = false) String shortDescription,
            @RequestParam String type,
            @RequestParam String federation,
            @RequestParam String responsible,
            @RequestParam(required = false) LocalDate receivedDate,
            @RequestParam(required = false) LocalDate openingDate,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) Integer priority) {
        GeschaeftView business = geschaeftService.open(new OpenGeschaeftCommand(
                guru.interlis.mabillon.numbering.DossierNumber.parse(dossierNumber), title, shortDescription,
                type, federation, responsible, receivedDate, openingDate, dueDate, priority));
        return "redirect:/geschaefte/" + business.number();
    }

    @GetMapping("/{number}")
    public String detail(@PathVariable String number, HttpServletRequest request, Model model) {
        GeschaeftView geschaeft = queryService.findByNumber(number)
                .orElseThrow(() -> new NotFoundException("Unbekanntes Geschäft: " + number));
        model.addAttribute("geschaeft", geschaeft);
        model.addAttribute("processStatuses", geschaeft.geschaeftsartCode() == null
                ? java.util.List.of()
                : catalogService.processStatusesForGeschaeftsart(geschaeft.geschaeftsartCode()));
        model.addAttribute("resultStatuses", geschaeft.geschaeftsartCode() == null
                ? java.util.List.of()
                : catalogService.resultStatusesForGeschaeftsart(geschaeft.geschaeftsartCode()));
        model.addAttribute("aufgaben", aufgabeQueryService.forGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("beteiligungen", beteiligungService.listForGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("beteiligte", beteiligterService.search(
                BeteiligterSearchCriteria.empty(), 0, 200).items());
        model.addAttribute("fachsystemReferenzen", fachsystemReferenzService.forGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("journalEntries", journalQueryService.findForGeschaeft(GeschaeftNumber.parse(number), 50));
        model.addAttribute("aufgabentypen", catalogService.list(CatalogType.AUFGABENTYP, false));
        model.addAttribute("beteiligungsrollen", catalogService.list(CatalogType.BETEILIGUNGSROLLE, false));
        return HtmxRequest.isRequest(request) ? "geschaefte/_detail" : "geschaefte/detail";
    }

    @PostMapping("/{number}/abschluss")
    public String close(@PathVariable String number) {
        geschaeftService.close(GeschaeftNumber.parse(number));
        return "redirect:/geschaefte/" + number;
    }

    @PostMapping("/{number}/fachsystem-referenzen")
    public String addFachsystemReferenz(
            @PathVariable String number,
            @RequestParam String systemCode,
            @RequestParam String objektTyp,
            @RequestParam String objektId,
            @RequestParam(required = false) String mutationId,
            @RequestParam(required = false) String link,
            @RequestParam(required = false) String beschreibung) {
        fachsystemReferenzService.addToGeschaeft(new AddFachsystemReferenzCommand(
                GeschaeftNumber.parse(number), systemCode, objektTyp, objektId, mutationId, link, beschreibung));
        return "redirect:/geschaefte/" + number;
    }

    @PostMapping("/{number}")
    public String update(
            @PathVariable String number,
            @RequestParam String title,
            @RequestParam(required = false) String shortDescription,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String responsible) {
        geschaeftService.update(new UpdateGeschaeftCommand(
                GeschaeftNumber.parse(number), title, shortDescription, responsible, dueDate, priority));
        return "redirect:/geschaefte/" + number;
    }

    @PostMapping("/{number}/prozessstatus")
    public String changeProcessStatus(
            @PathVariable String number,
            @RequestParam String processStatusCode,
            @RequestParam(required = false) String comment,
            HttpServletRequest request,
            Model model) {
        GeschaeftView geschaeft = geschaeftService.changeProcessStatus(
                new ChangeProcessStatusCommand(GeschaeftNumber.parse(number), processStatusCode, blankToNull(comment)));
        model.addAttribute("geschaeft", geschaeft);
        model.addAttribute("processStatuses", geschaeft.geschaeftsartCode() == null
                ? java.util.List.of()
                : catalogService.processStatusesForGeschaeftsart(geschaeft.geschaeftsartCode()));
        model.addAttribute("resultStatuses", geschaeft.geschaeftsartCode() == null
                ? java.util.List.of()
                : catalogService.resultStatusesForGeschaeftsart(geschaeft.geschaeftsartCode()));
        model.addAttribute("aufgaben", aufgabeQueryService.forGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("beteiligungen", beteiligungService.listForGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("aufgabentypen", catalogService.list(CatalogType.AUFGABENTYP, false));
        model.addAttribute("beteiligungsrollen", catalogService.list(CatalogType.BETEILIGUNGSROLLE, false));
        return HtmxRequest.isRequest(request)
                ? "geschaefte/_status-panel"
                : "redirect:/geschaefte/" + number;
    }

    @PostMapping("/{number}/resultat")
    public String setResult(
            @PathVariable String number,
            @RequestParam String resultStatusCode,
            @RequestParam(required = false) String comment,
            HttpServletRequest request,
            Model model) {
        GeschaeftView geschaeft = geschaeftService.setResult(new SetResultCommand(
                GeschaeftNumber.parse(number), resultStatusCode, blankToNull(comment)));
        model.addAttribute("geschaeft", geschaeft);
        model.addAttribute("processStatuses", geschaeft.geschaeftsartCode() == null
                ? java.util.List.of()
                : catalogService.processStatusesForGeschaeftsart(geschaeft.geschaeftsartCode()));
        model.addAttribute("resultStatuses", geschaeft.geschaeftsartCode() == null
                ? java.util.List.of()
                : catalogService.resultStatusesForGeschaeftsart(geschaeft.geschaeftsartCode()));
        model.addAttribute("aufgaben", aufgabeQueryService.forGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("beteiligungen", beteiligungService.listForGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("aufgabentypen", catalogService.list(CatalogType.AUFGABENTYP, false));
        model.addAttribute("beteiligungsrollen", catalogService.list(CatalogType.BETEILIGUNGSROLLE, false));
        return HtmxRequest.isRequest(request)
                ? "geschaefte/_status-panel"
                : "redirect:/geschaefte/" + number;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
