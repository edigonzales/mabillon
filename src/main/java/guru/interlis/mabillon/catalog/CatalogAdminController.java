package guru.interlis.mabillon.catalog;

import java.util.Arrays;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import guru.interlis.mabillon.web.HtmxRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/kataloge")
public final class CatalogAdminController {

    private final CatalogService catalogService;

    public CatalogAdminController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("title", "Kataloge");
        model.addAttribute("active", "catalogs");
        model.addAttribute("groups", Arrays.stream(CatalogType.values())
                .map(type -> new CatalogGroupView(type, catalogService.list(type, true)))
                .toList());
        return "admin/catalogs";
    }

    @PostMapping("/{type}")
    String create(
            @PathVariable String type,
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String geschaeftsartCode,
            @RequestParam(required = false) Integer sortierung,
            @RequestParam(defaultValue = "false") boolean initial,
            @RequestParam(defaultValue = "false") boolean terminal,
            @RequestParam(defaultValue = "false") boolean resultatErforderlich,
            HttpServletRequest request,
            Model model) {
        CatalogType catalogType = parseType(type);
        catalogService.create(new CatalogCreateCommand(catalogType, code, name, description,
                geschaeftsartCode, sortierung, initial, terminal, resultatErforderlich));
        if (HtmxRequest.isRequest(request)) {
            return index(model);
        }
        return "redirect:/admin/kataloge";
    }

    @PostMapping("/{type}/{code}/deactivate")
    String deactivate(
            @PathVariable String type,
            @PathVariable String code,
            HttpServletRequest request,
            Model model) {
        catalogService.deactivate(parseType(type), code);
        if (HtmxRequest.isRequest(request)) {
            return index(model);
        }
        return "redirect:/admin/kataloge";
    }

    @PostMapping("/{type}/{code}/activate")
    String activate(
            @PathVariable String type,
            @PathVariable String code,
            HttpServletRequest request,
            Model model) {
        catalogService.activate(parseType(type), code);
        if (HtmxRequest.isRequest(request)) {
            return index(model);
        }
        return "redirect:/admin/kataloge";
    }

    private CatalogType parseType(String value) {
        return CatalogType.valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
