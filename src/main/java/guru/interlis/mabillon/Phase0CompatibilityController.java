package guru.interlis.mabillon;

import java.time.Clock;
import java.time.LocalDate;

import guru.interlis.mabillon.dashboard.MyWorkQueryService;
import guru.interlis.mabillon.security.AuthorizationException;
import guru.interlis.mabillon.security.CurrentActor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Phase0CompatibilityController {

    private final MyWorkQueryService myWorkQueryService;
    private final CurrentActor currentActor;
    private final Clock clock;

    public Phase0CompatibilityController(
            MyWorkQueryService myWorkQueryService,
            CurrentActor currentActor,
            Clock clock) {
        this.myWorkQueryService = myWorkQueryService;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("title", "Mabillon");
        try {
            model.addAttribute("work", myWorkQueryService.load(
                    currentActor.username(), LocalDate.now(clock)));
        } catch (AuthorizationException ignored) {
            model.addAttribute("work", null);
        }
        return "hello";
    }
}
