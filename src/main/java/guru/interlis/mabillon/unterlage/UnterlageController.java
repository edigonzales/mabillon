package guru.interlis.mabillon.unterlage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.storage.DocumentUpload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public final class UnterlageController {

    private final UnterlageService unterlageService;
    private final UnterlageContentService contentService;

    public UnterlageController(UnterlageService unterlageService, UnterlageContentService contentService) {
        this.unterlageService = unterlageService;
        this.contentService = contentService;
    }

    @PostMapping("/dossiers/{dossierNumber}/unterlagen")
    public String register(
            @PathVariable String dossierNumber,
            @RequestParam String title,
            @RequestParam String typCode,
            @RequestParam(required = false) String geschaeftNumber,
            @RequestParam(required = false) java.time.LocalDate documentDate,
            @RequestParam(required = false) java.time.LocalDate incomingDate,
            @RequestParam(required = false) java.time.LocalDate outgoingDate,
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
        return "redirect:/dossiers/" + value.dossierNumber();
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
