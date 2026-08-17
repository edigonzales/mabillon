package guru.interlis.mabillon.geschaeft;

import java.time.Clock;
import java.time.LocalDate;

import guru.interlis.mabillon.web.HtmxRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public final class GeschaeftskontrolleController {

    private final GeschaeftskontrolleQueryService queryService;
    private final Clock clock;

    public GeschaeftskontrolleController(GeschaeftskontrolleQueryService queryService, Clock clock) {
        this.queryService = queryService;
        this.clock = clock;
    }

    @GetMapping("/geschaeftskontrolle")
    public String index(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "30") int inactiveSinceDays,
            HttpServletRequest request,
            Model model) {
        GeschaeftskontrolleCriteria criteria = new GeschaeftskontrolleCriteria(
                LocalDate.now(clock), limit, inactiveSinceDays);
        model.addAttribute("criteria", criteria);
        model.addAttribute("control", queryService.load(criteria));
        model.addAttribute("title", "Geschäftskontrolle");
        return HtmxRequest.isRequest(request) ? "geschaeftskontrolle/_content" : "geschaeftskontrolle/index";
    }
}
