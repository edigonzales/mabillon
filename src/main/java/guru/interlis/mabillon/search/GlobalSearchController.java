package guru.interlis.mabillon.search;

import jakarta.servlet.http.HttpServletRequest;

import guru.interlis.mabillon.web.HtmxRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public final class GlobalSearchController {

    private final GlobalSearchService searchService;

    public GlobalSearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/suche")
    public String search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String geschaeftsnummer,
            @RequestParam(required = false) String dossiernummer,
            @RequestParam(required = false) String titel,
            @RequestParam(required = false) String beteiligterName,
            @RequestParam(required = false) String organisation,
            @RequestParam(required = false) String geschaeftsartCode,
            @RequestParam(required = false) String processStatusCode,
            @RequestParam(required = false) String unterlagentitel,
            @RequestParam(required = false) String fachsystemId,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request,
            Model model) {
        GlobalSearchCriteria criteria = new GlobalSearchCriteria(
                q, geschaeftsnummer, dossiernummer, titel, beteiligterName, organisation,
                geschaeftsartCode, processStatusCode, unterlagentitel, fachsystemId, page, 20);
        model.addAttribute("criteria", criteria);
        model.addAttribute("result", searchService.search(criteria));
        model.addAttribute("title", "Systemweite Suche");
        return HtmxRequest.isRequest(request) ? "suche/_results" : "suche/index";
    }
}
