package guru.interlis.mabillon.unterlage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.storage.DocumentUpload;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public final class UnterlageController {

    private final UnterlageService unterlageService;
    private final UnterlageQueryService queryService;
    private final UnterlageContentService contentService;

    public UnterlageController(
            UnterlageService unterlageService,
            UnterlageQueryService queryService,
            UnterlageContentService contentService) {
        this.unterlageService = unterlageService;
        this.queryService = queryService;
        this.contentService = contentService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @PostMapping("/dossiers/{dossierNumber}/unterlagen")
    public String register(
            @PathVariable String dossierNumber,
            @RequestParam String title,
            @RequestParam String typCode,
            @RequestParam(required = false) String geschaeftNumber,
            @RequestParam(required = false) LocalDate documentDate,
            @RequestParam(required = false) LocalDate incomingDate,
            @RequestParam(required = false) LocalDate outgoingDate,
            @RequestParam(defaultValue = "true") boolean aktenrelevant,
            @RequestParam(required = false) String dateiformat,
            @RequestParam(required = false) String bemerkungen,
            @RequestParam("file") MultipartFile file) throws IOException {
        DocumentUpload upload = file == null || file.isEmpty() ? null
                : new DocumentUpload(file.getOriginalFilename(),
                        file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType(),
                        file.getInputStream());
        UnterlageView value = unterlageService.register(new RegisterUnterlageCommand(
                DossierNumber.parse(dossierNumber), blankToNull(geschaeftNumber) == null
                        ? null : GeschaeftNumber.parse(geschaeftNumber), title, typCode,
                documentDate, incomingDate, outgoingDate, aktenrelevant, dateiformat, bemerkungen), upload);
        return "redirect:/unterlagen/" + value.tid();
    }

    @GetMapping("/unterlagen/{tid}")
    public String detail(@PathVariable UUID tid, Model model) {
        UnterlageView value = queryService.get(tid);
        model.addAttribute("title", value.title());
        model.addAttribute("active", "dossiers");
        model.addAttribute("unterlage", value);
        return "unterlagen/detail";
    }

    @PostMapping("/unterlagen/{tid}")
    public String updateMetadata(
            @PathVariable UUID tid,
            @RequestParam String title,
            @RequestParam String typCode,
            @RequestParam(required = false) LocalDate documentDate,
            @RequestParam(required = false) LocalDate incomingDate,
            @RequestParam(required = false) LocalDate outgoingDate,
            @RequestParam(required = false) String dateiformat,
            @RequestParam(required = false) String bemerkungen) {
        unterlageService.updateMetadata(new UpdateUnterlageCommand(
                tid, title, typCode, documentDate, incomingDate, outgoingDate,
                blankToNull(dateiformat), blankToNull(bemerkungen)));
        return redirect(tid);
    }

    @PostMapping("/unterlagen/{tid}/geschaeft")
    public String assign(@PathVariable UUID tid, @RequestParam String geschaeftNumber) {
        unterlageService.assignToGeschaeft(new AssignUnterlageCommand(tid, GeschaeftNumber.parse(geschaeftNumber)));
        return redirect(tid);
    }

    @PostMapping("/unterlagen/{tid}/geschaeft/entfernen")
    public String unassign(@PathVariable UUID tid) {
        unterlageService.unassignFromGeschaeft(tid);
        return redirect(tid);
    }

    @PostMapping("/unterlagen/{tid}/finalisieren")
    public String finalizeUnterlage(@PathVariable UUID tid) {
        unterlageService.finalizeUnterlage(tid);
        return redirect(tid);
    }

    @PostMapping("/unterlagen/{tid}/aktenrelevant-registrieren")
    public String registerAktenrelevant(@PathVariable UUID tid) {
        unterlageService.registerAktenrelevant(tid);
        return redirect(tid);
    }

    @PostMapping("/unterlagen/{tid}/stornieren")
    public String cancel(@PathVariable UUID tid, @RequestParam(required = false) String reason) {
        unterlageService.cancel(tid, blankToNull(reason));
        return redirect(tid);
    }

    @GetMapping("/unterlagen/{tid}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID tid) {
        OpenedDocument value = contentService.open(tid);
        MediaType mediaType = mediaType(value.mimeType());
        String filename = value.filename() == null || value.filename().isBlank()
                ? "unterlage-" + tid : value.filename();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(value.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(value.content()));
    }

    private static String redirect(UUID tid) {
        return "redirect:/unterlagen/" + tid;
    }

    private static MediaType mediaType(String value) {
        if (value == null || value.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(value);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
