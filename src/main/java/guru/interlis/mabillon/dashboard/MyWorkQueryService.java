package guru.interlis.mabillon.dashboard;

import java.time.LocalDate;

import guru.interlis.mabillon.aufgabe.AufgabeQueryService;
import guru.interlis.mabillon.geschaeft.GeschaeftQueryService;
import guru.interlis.mabillon.geschaeft.GeschaeftSearchCriteria;
import org.springframework.stereotype.Service;

@Service
public final class MyWorkQueryService {

    private final GeschaeftQueryService geschaeftQueryService;
    private final AufgabeQueryService aufgabeQueryService;

    public MyWorkQueryService(
            GeschaeftQueryService geschaeftQueryService,
            AufgabeQueryService aufgabeQueryService) {
        this.geschaeftQueryService = geschaeftQueryService;
        this.aufgabeQueryService = aufgabeQueryService;
    }

    public MyWorkView load(String username, LocalDate today) {
        if (username == null || username.isBlank() || today == null) {
            throw new IllegalArgumentException("Benutzer und Datum sind erforderlich.");
        }
        return new MyWorkView(
                geschaeftQueryService.activeForUser(username, 8),
                geschaeftQueryService.search(new GeschaeftSearchCriteria(
                        null, null, null, null, null, username, null, today, today.plusDays(14)), 0, 8).items(),
                geschaeftQueryService.recentlyChangedForUser(username, 8),
                aufgabeQueryService.myOpenTasks(username, 8),
                aufgabeQueryService.overdueForUser(username, today, 8));
    }
}
