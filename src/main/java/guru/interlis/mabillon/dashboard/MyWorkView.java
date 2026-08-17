package guru.interlis.mabillon.dashboard;

import java.util.List;

import guru.interlis.mabillon.aufgabe.AufgabeView;
import guru.interlis.mabillon.geschaeft.GeschaeftView;

public record MyWorkView(
        List<GeschaeftView> activeBusinesses,
        List<GeschaeftView> dueSoonBusinesses,
        List<GeschaeftView> recentlyChangedBusinesses,
        List<AufgabeView> openTasks,
        List<AufgabeView> overdueTasks) {

    public MyWorkView {
        activeBusinesses = List.copyOf(activeBusinesses);
        dueSoonBusinesses = List.copyOf(dueSoonBusinesses);
        recentlyChangedBusinesses = List.copyOf(recentlyChangedBusinesses);
        openTasks = List.copyOf(openTasks);
        overdueTasks = List.copyOf(overdueTasks);
    }
}
