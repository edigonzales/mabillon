package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import guru.interlis.mabillon.aufgabe.AufgabeController;
import guru.interlis.mabillon.beteiligung.BeteiligterController;
import guru.interlis.mabillon.beteiligung.BeteiligungController;
import guru.interlis.mabillon.catalog.CatalogAdminController;
import guru.interlis.mabillon.registraturplan.RegistraturplanController;
import guru.interlis.mabillon.stammdaten.MasterdataController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class Phase11UiReachabilityTest {

    @Test
    void participantWorkspaceExposesListCreateDetailAndEditRoutes() {
        assertThat(getRoutes(BeteiligterController.class)).contains(
                "/beteiligte",
                "/beteiligte/neu",
                "/beteiligte/{tid}",
                "/beteiligte/{tid}/bearbeiten");
        assertThat(postRoutes(BeteiligterController.class)).contains(
                "/beteiligte",
                "/beteiligte/{tid}");
    }

    @Test
    void taskWorkspaceExposesPersonalListDetailUpdateAndDelegationRoutes() {
        assertThat(getRoutes(AufgabeController.class)).contains(
                "/aufgaben",
                "/aufgaben/{tid}");
        assertThat(postRoutes(AufgabeController.class)).contains(
                "/aufgaben/{tid}",
                "/aufgaben/{tid}/delegate");
    }

    @Test
    void participationWorkspaceExposesUpdateAndEndRoutes() {
        assertThat(postRoutes(BeteiligungController.class)).contains(
                "/beteiligungen/{tid}",
                "/beteiligungen/{tid}/end");
    }

    @Test
    void catalogAdministrationExposesEditAndUpdateRoutes() {
        assertThat(getRoutes(CatalogAdminController.class)).contains(
                "/admin/kataloge/{type}/{code}/bearbeiten");
        assertThat(postRoutes(CatalogAdminController.class)).contains(
                "/admin/kataloge/{type}/{code}");
    }

    @Test
    void masterdataAdministrationExposesOrganisationAndUserUpdates() {
        assertThat(postRoutes(MasterdataController.class)).contains(
                "/admin/stammdaten/organisationseinheiten/{kuerzel}",
                "/admin/stammdaten/benutzer/{username}");
    }

    @Test
    void registraturplanAdministrationExposesPlanAndPositionMaintenance() {
        assertThat(postRoutes(RegistraturplanController.class)).contains(
                "/admin/registraturplan/plaene",
                "/admin/registraturplan/plaene/{code}/activate",
                "/admin/registraturplan/plaene/{code}/replace",
                "/admin/registraturplan/positionen",
                "/admin/registraturplan/positionen/{code}",
                "/admin/registraturplan/positionen/{code}/verschieben",
                "/admin/registraturplan/positionen/{code}/deactivate");
    }

    private static Set<String> getRoutes(Class<?> controller) {
        String prefix = prefix(controller);
        Set<String> routes = new LinkedHashSet<>();
        Arrays.stream(controller.getDeclaredMethods()).forEach(method -> {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            if (mapping != null) {
                addRoutes(routes, prefix, mapping.value());
            }
        });
        return routes;
    }

    private static Set<String> postRoutes(Class<?> controller) {
        String prefix = prefix(controller);
        Set<String> routes = new LinkedHashSet<>();
        Arrays.stream(controller.getDeclaredMethods()).forEach(method -> {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            if (mapping != null) {
                addRoutes(routes, prefix, mapping.value());
            }
        });
        return routes;
    }

    private static String prefix(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        if (mapping == null || mapping.value().length == 0) {
            return "";
        }
        return mapping.value()[0];
    }

    private static void addRoutes(Set<String> routes, String prefix, String[] suffixes) {
        if (suffixes.length == 0) {
            routes.add(prefix);
            return;
        }
        for (String suffix : suffixes) {
            routes.add(prefix + suffix);
        }
    }
}
