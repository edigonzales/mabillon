package guru.interlis.mabillon.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public final class AdminController {

    @GetMapping
    String index(Model model) {
        model.addAttribute("title", "Administration");
        model.addAttribute("active", "admin");
        return "admin/index";
    }
}
