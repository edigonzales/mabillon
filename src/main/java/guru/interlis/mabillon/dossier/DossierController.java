package guru.interlis.mabillon.dossier;

import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.catalog.CatalogService;
import guru.interlis.mabillon.catalog.CatalogType;
import guru.interlis.mabillon.domain.NotFoundException;
import guru.interlis.mabillon.registraturplan.RegistraturplanQueryService;
import guru.interlis.mabillon.fachsystem.AddFachsystemReferenzToDossierCommand;
import guru.interlis.mabillon.fachsystem.FachsystemReferenzService;
import guru.interlis.mabillon.journal.JournalQueryService;
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
@RequestMapping("/dossiers")
public final class DossierController {

    private final DossierQueryService queryService;
    private final DossierService dossierService;
    private final RegistraturplanQueryService registraturplanQueryService;
    private final OrganisationseinheitService organisationseinheitService;
    private final BenutzerService benutzerService;
    private final CatalogService catalogService;
    private final FachsystemReferenzService fachsystemReferenzService;
    private final JournalQueryService journalQueryService;

    public DossierController(
            DossierQueryService queryService,
            DossierService dossierService,
            RegistraturplanQueryService registraturplanQueryService,
            OrganisationseinheitService organisationseinheitService,
            BenutzerService benutzerService,
            CatalogService catalogService,
            FachsystemReferenzService fachsystemReferenzService,
            JournalQueryService journalQueryService) {
        this.queryService = queryService;
        this.dossierService = dossierService;
        this.registraturplanQueryService = registraturplanQueryService;
        this.organisationseinheitService = organisationseinheitService;
        this.benutzerService = benutzerService;
        this.catalogService = catalogService;
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
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String federation,
            @RequestParam(required = false) LocalDate openedFrom,
            @RequestParam(required = false) LocalDate openedTo,
            @RequestParam(required = false) LocalDate closedFrom,
            @RequestParam(required = false) LocalDate closedTo,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request,
            Model model) {
        DossierSearchCriteria criteria = new DossierSearchCriteria(
                number, title, position, status, federation, openedFrom, openedTo, closedFrom, closedTo);
        model.addAttribute("searchPage", queryService.search(criteria, page, 20));
        model.addAttribute("criteria", criteria);
        model.addAttribute("positions", registraturplanQueryService.activeLeafPositions());
        model.addAttribute("organisationseinheiten", organisationseinheitService.list(false));
        model.addAttribute("statuses", new String[] {"Offen", "Geschlossen"});
        model.addAttribute("title", "Dossiers");
        return HtmxRequest.isRequest(request) ? "dossiers/_list" : "dossiers/index";
    }

    @GetMapping("/neu")
    public String newForm(Model model) {
        model.addAttribute("positions", registraturplanQueryService.activeLeafPositions());
        model.addAttribute("organisationseinheiten", organisationseinheitService.list(false));
        model.addAttribute("benutzer", benutzerService.list(false));
        model.addAttribute("title", "Neues Dossier");
        return "dossiers/new";
    }

    @PostMapping
    public String open(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String position,
            @RequestParam String federation,
            @RequestParam String responsible,
            @RequestParam(required = false) LocalDate openingDate) {
        DossierView dossier = dossierService.open(new OpenDossierCommand(
                title, description, position, federation, responsible, openingDate));
        return "redirect:/dossiers/" + dossier.number();
    }

    @GetMapping("/{number}")
    public String detail(@PathVariable String number, HttpServletRequest request, Model model) {
        DossierView dossier = queryService.findByNumber(number)
                .orElseThrow(() -> new NotFoundException("Unbekanntes Dossier: " + number));
        model.addAttribute("dossier", dossier);
        model.addAttribute("unterlagentypen", catalogService.list(CatalogType.UNTERLAGENTYP, false));
        model.addAttribute("fachsystemReferenzen", fachsystemReferenzService.forDossier(DossierNumber.parse(number)));
        model.addAttribute("journalEntries", journalQueryService.findForDossier(DossierNumber.parse(number), 50));
        return HtmxRequest.isRequest(request) ? "dossiers/_detail" : "dossiers/detail";
    }

    @PostMapping("/{number}/abschluss")
    public String close(@PathVariable String number) {
        dossierService.close(DossierNumber.parse(number));
        return "redirect:/dossiers/" + number;
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
        fachsystemReferenzService.addToDossier(new AddFachsystemReferenzToDossierCommand(
                DossierNumber.parse(number), systemCode, objektTyp, objektId, mutationId, link, beschreibung));
        return "redirect:/dossiers/" + number;
    }

    @PostMapping("/{number}")
    public String update(
            @PathVariable String number,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String responsible,
            @RequestParam(required = false) String remarks) {
        dossierService.update(new UpdateDossierCommand(
                DossierNumber.parse(number), title, description, responsible, remarks));
        return "redirect:/dossiers/" + number;
    }
}
