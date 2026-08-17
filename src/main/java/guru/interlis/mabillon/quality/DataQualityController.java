package guru.interlis.mabillon.quality;

import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.numbering.GeschaeftNumber;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public final class DataQualityController {

    private final DataQualityService qualityService;

    public DataQualityController(DataQualityService qualityService) {
        this.qualityService = qualityService;
    }

    @GetMapping("/datenqualitaet/dossiers/{number}")
    public String dossier(@PathVariable String number, Model model) {
        model.addAttribute("report", qualityService.checkDossier(DossierNumber.parse(number)));
        model.addAttribute("title", "Datenqualität Dossier");
        return "quality/report";
    }

    @GetMapping("/datenqualitaet/geschaefte/{number}")
    public String business(@PathVariable String number, Model model) {
        model.addAttribute("report", qualityService.checkGeschaeft(GeschaeftNumber.parse(number)));
        model.addAttribute("title", "Datenqualität Geschäft");
        return "quality/report";
    }
}
