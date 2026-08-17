package guru.interlis.mabillon.interlis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/interlis")
public final class InterlisExchangeController {

    private final InterlisExchangeService exchangeService;
    private final Path exportRoot;

    public InterlisExchangeController(
            InterlisExchangeService exchangeService,
            @Value("${mabillon.interlis.export-root:${java.io.tmpdir}/mabillon-exports}") String exportRoot) {
        this.exchangeService = exchangeService;
        this.exportRoot = Path.of(exportRoot).toAbsolutePath().normalize();
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    String index(Model model) {
        return render(model, null);
    }

    @PostMapping("/import/{scope}")
    String importTopic(
            @PathVariable String scope,
            @RequestParam("file") MultipartFile file,
            Model model) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Eine XTF-Datei ist erforderlich.");
        }
        ImportScope importScope = ImportScope.valueOf(scope.toUpperCase());
        Path temporary = Files.createTempFile("mabillon-import-", ".xtf");
        try {
            file.transferTo(temporary);
            ExchangeResult result = switch (importScope) {
                case CATALOG -> exchangeService.importCatalog(temporary);
                case MASTER_DATA -> exchangeService.importMasterData(temporary);
                case BUSINESS_DATA -> exchangeService.importBusinessData(temporary);
            };
            model.addAttribute("result", result);
            return render(model, result);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @GetMapping("/export/{scope}")
    ResponseEntity<Resource> exportTopic(@PathVariable String scope) throws IOException {
        ImportScope importScope = ImportScope.valueOf(scope.toUpperCase());
        Files.createDirectories(exportRoot);
        Path target = exportRoot.resolve("mabillon-" + importScope.name().toLowerCase() + "-"
                + UUID.randomUUID() + ".xtf").normalize();
        Path exported = switch (importScope) {
            case CATALOG -> exchangeService.exportCatalog(ExportSelection.all(target));
            case MASTER_DATA -> exchangeService.exportMasterData(ExportSelection.all(target));
            case BUSINESS_DATA -> exchangeService.exportBusinessData(ExportSelection.all(target));
        };
        FileSystemResource resource = new FileSystemResource(exported);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(exported.getFileName().toString())
                .build());
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private String render(Model model, ExchangeResult result) {
        model.addAttribute("title", "INTERLIS-Datenaustausch");
        model.addAttribute("active", "admin");
        model.addAttribute("scopes", Arrays.asList(ImportScope.values()));
        model.addAttribute("result", result);
        return "interlis/index";
    }
}
