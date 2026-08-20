package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlaywrightGoldenPathE2ETest {

    private static final Path PLAYWRIGHT_ROOT = Path.of("build/playwright").toAbsolutePath().normalize();
    private static final Path STORAGE_ROOT = PLAYWRIGHT_ROOT.resolve("storage");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @LocalServerPort
    private int port;

    @TempDir
    Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("mabillon.cayenne.url", POSTGRES::getJdbcUrl);
        registry.add("mabillon.cayenne.username", POSTGRES::getUsername);
        registry.add("mabillon.cayenne.password", POSTGRES::getPassword);
        registry.add("mabillon.storage.root", () -> STORAGE_ROOT.toString());
    }

    @BeforeAll
    static void prepareGoldenPath() throws IOException {
        deleteRecursively(PLAYWRIGHT_ROOT);
        Files.createDirectories(PLAYWRIGHT_ROOT);
        InterlisTestFixture.importGoldenPath(POSTGRES);
    }

    @Test
    void completeNomenklaturGoldenPathRunsInRealChromium() throws Exception {
        Path antrag = minimalPdf(tempDir.resolve("antrag-gemeinde-musterwil.pdf"), "Antrag Gemeinde Musterwil");
        Path beschluss = minimalPdf(tempDir.resolve("kommissionsbeschluss-bodenrain.pdf"), "Kommissionsbeschluss Bodenrain");
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                "admin:admin".getBytes(StandardCharsets.UTF_8));

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

            BrowserContext reviewContext = browser.newContext(new Browser.NewContextOptions()
                    .setBaseURL("http://127.0.0.1:" + port)
                    .setViewportSize(1280, 720)
                    .setExtraHTTPHeaders(Map.of("Authorization", authorization)));
            Page reviewPage = reviewContext.newPage();
            reviewPage.setDefaultTimeout(15_000);
            reviewPage.setDefaultNavigationTimeout(20_000);
            try {
                verifyIli2GrailsDesignSystem(reviewPage);
            } catch (Throwable failure) {
                reviewPage.screenshot(new Page.ScreenshotOptions()
                        .setPath(PLAYWRIGHT_ROOT.resolve("ui-contract-failure.png"))
                        .setFullPage(true));
                throw failure;
            } finally {
                reviewContext.close();
            }

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setBaseURL("http://127.0.0.1:" + port)
                    .setViewportSize(1280, 720)
                    .setExtraHTTPHeaders(Map.of("Authorization", authorization)));
            Page page = context.newPage();
            page.setDefaultTimeout(15_000);
            page.setDefaultNavigationTimeout(20_000);

            try {
                page.navigate("/");
                page.evaluate("() => document.fonts.ready");
                assertThat(page.title()).contains("Mabillon");
                assertThat(page.locator("body").innerText())
                        .contains("Meine offenen Aufgaben", "Aktive Geschäfte");

                String dossierNumber = openDossier(page);
                String businessNumber = openBusiness(page, dossierNumber);

                assignMunicipality(page, businessNumber);
                registerDocument(page, dossierNumber, businessNumber,
                        "Antrag Gemeinde Musterwil", "ANTRAG", antrag);

                page.navigate("/geschaefte/" + businessNumber);
                setProcessStatus(page, businessNumber, "FACHLICHE_PRUEFUNG", "Fachliche Prüfung gestartet.");
                createAndCompleteTask(page, businessNumber);

                registerDocument(page, dossierNumber, businessNumber,
                        "Kommissionsbeschluss Bodenrain", "KOMMISSIONSBESCHLUSS", beschluss);

                page.navigate("/geschaefte/" + businessNumber);
                setResult(page, businessNumber, "GENEHMIGT", "Umbenennung zu Bodenrain genehmigt.");
                setProcessStatus(page, businessNumber, "ABGESCHLOSSEN", "Verfahren abgeschlossen.");

                page.locator("form[action='/geschaefte/" + businessNumber + "/abschluss'] button[type='submit']").click();
                assertThat(page.locator("#geschaeft-detail").innerText())
                        .contains("Abgeschlossen", "Geschaeft_abgeschlossen", "anna.mueller");

                page.navigate("/dossiers/" + dossierNumber);
                page.locator("form[action='/dossiers/" + dossierNumber + "/abschluss'] button[type='submit']").click();
                assertThat(page.locator("#dossier-detail").innerText())
                        .contains("Geschlossen", "Dossier_abgeschlossen", "anna.mueller");

                page.navigate("/geschaefte/" + businessNumber);
                String businessJournal = page.locator("#geschaeft-detail").innerText();
                assertThat(businessJournal)
                        .contains("Erstellt", "Status_geaendert", "Entscheid_erfasst", "Geschaeft_abgeschlossen")
                        .contains("anna.mueller")
                        .contains("Gemeinde Musterwil")
                        .contains("Antragstellerin")
                        .contains("Antrag Gemeinde Musterwil")
                        .contains("Kommissionsbeschluss Bodenrain");
            } catch (Throwable failure) {
                Path screenshot = PLAYWRIGHT_ROOT.resolve("golden-path-failure.png");
                page.screenshot(new Page.ScreenshotOptions().setPath(screenshot).setFullPage(true));
                throw failure;
            } finally {
                context.close();
                browser.close();
            }
        }
    }

    private static void verifyIli2GrailsDesignSystem(Page page) throws IOException {
        Path uiRoot = PLAYWRIGHT_ROOT.resolve("ui");
        Files.createDirectories(uiRoot);

        page.navigate("/");
        page.evaluate("() => document.fonts.ready");
        assertThat(page.title()).contains("Mabillon");
        assertThat(page.locator("body").innerText())
                .contains("Meine offenen Aufgaben", "Aktive Geschäfte");
        assertThat(page.locator(".mabillon-summary-grid[aria-label='Meine Arbeit']").count()).isZero();
        assertThat(page.locator(".mabillon-dashboard-grid .mabillon-tile").count()).isEqualTo(3);
        assertThat((Boolean) page.evaluate("() => document.fonts.check('16px \\\"Fira Sans\\\"')")).isTrue();
        assertThat(css(page.locator("body"), "font-family")).contains("Fira Sans");
        assertThat(css(page.locator("body"), "background-color")).isEqualTo("rgb(255, 255, 255)");
        assertThat(css(page.locator(".mabillon-topbar"), "height")).isEqualTo("64px");
        assertThat(css(page.locator(".mabillon-sidebar"), "width")).isEqualTo("272px");
        assertThat(css(page.locator(".mabillon-navigation a.mabillon-navigation-link--active"), "background-color"))
                .isEqualTo("rgb(236, 245, 252)");
        assertThat(css(page.locator(".mabillon-global-search .mabillon-button"), "background-color"))
                .isEqualTo("rgb(66, 153, 225)");
        assertThat(css(page.locator(".mabillon-global-search .mabillon-button"), "border-radius")).isEqualTo("3px");
        assertThat(css(page.locator(".mabillon-search-control input"), "border-left-width")).isEqualTo("0px");
        capture(page, uiRoot.resolve("overview.png"));

        Locator globalSearch = page.locator(".mabillon-global-search");
        globalSearch.locator("input[name='q']").fill("AGI-G-2026-000421");
        globalSearch.locator("button[type='submit']").click();
        assertThat(URI.create(page.url()).getQuery()).isEqualTo("q=AGI-G-2026-000421");
        assertThat(page.locator(".mabillon-main-grid").innerText()).contains("AGI-G-2026-000421");

        page.navigate("/dossiers");
        assertThat(css(page.locator(".mabillon-table-wrap"), "border-top-color"))
                .isEqualTo("rgb(211, 221, 229)");
        assertThat(css(page.locator(".mabillon-table-wrap th").first(), "background-color"))
                .isEqualTo("rgb(237, 242, 245)");
        Locator firstResult = page.locator(".mabillon-table-wrap tbody tr").first();
        firstResult.hover();
        assertThat(css(firstResult, "background-color")).isEqualTo("rgb(232, 241, 247)");
        Locator filterGrid = page.locator(".mabillon-filter-grid");
        assertThat(css(filterGrid.locator("label").first(), "font-size")).isEqualTo("16px");
        assertThat(css(filterGrid.locator("label").first(), "font-weight")).isEqualTo("600");
        assertThat(css(filterGrid.locator("input").first(), "font-weight")).isEqualTo("400");
        Locator firstSelect = filterGrid.locator("select").first();
        assertThat(css(firstSelect, "font-weight")).isEqualTo("400");
        assertThat(css(firstSelect, "appearance")).isEqualTo("none");
        assertThat(css(firstSelect, "padding-right")).isEqualTo("36px");
        assertThat(css(firstSelect, "background-size")).isEqualTo("16px 12px");
        assertThat(css(firstSelect, "background-image")).isNotEqualTo("none");
        capture(page, uiRoot.resolve("dossiers.png"));

        page.navigate("/geschaefte/AGI-G-2026-000421");
        capture(page, uiRoot.resolve("geschaeft-detail.png"));
        page.navigate("/geschaefte/neu");
        capture(page, uiRoot.resolve("geschaeft-formular.png"));
        page.navigate("/admin");
        capture(page, uiRoot.resolve("administration.png"));

        page.setViewportSize(390, 844);
        page.navigate("/dossiers/neu");
        page.waitForFunction("() => document.documentElement.classList.contains('mabillon-js-enabled')");
        assertThat((Boolean) page.evaluate(
                "() => document.documentElement.scrollWidth === document.documentElement.clientWidth"))
                .isTrue();
        Locator formLabels = page.locator(".mabillon-form-grid > label");
        assertThat(formLabels.nth(1).boundingBox().y).isGreaterThan(formLabels.first().boundingBox().y);

        Locator navigationToggle = page.locator("[data-mabillon-navigation-toggle]");
        assertThat(navigationToggle.isVisible()).isTrue();
        navigationToggle.focus();
        assertThat(css(navigationToggle, "outline-color")).isEqualTo("rgb(66, 153, 225)");
        navigationToggle.click();
        Locator navigation = page.locator("[data-mabillon-navigation-panel]");
        assertThat(navigation.getAttribute("data-mabillon-navigation-open")).isEqualTo("true");
        page.keyboard().press("Escape");
        assertThat(navigation.getAttribute("data-mabillon-navigation-open")).isEqualTo("false");
        assertThat((Boolean) page.evaluate(
                "() => document.activeElement === document.querySelector('[data-mabillon-navigation-toggle]')"))
                .isTrue();

        page.navigate("/geschaefte/neu");
        capture(page, uiRoot.resolve("geschaeft-formular-mobile.png"));
        capture(page, uiRoot.resolve("dossier-formular-mobile.png"));
        page.setViewportSize(1280, 720);
    }

    private static String css(Locator locator, String property) {
        return String.valueOf(locator.evaluate(
                "(element, propertyName) => getComputedStyle(element).getPropertyValue(propertyName)", property)).trim();
    }

    private static void capture(Page page, Path target) {
        page.screenshot(new Page.ScreenshotOptions().setPath(target).setFullPage(true));
    }

    private static String openDossier(Page page) {
        page.navigate("/dossiers/neu");
        Locator form = page.locator("form[action='/dossiers']");
        form.locator("input[name='title']").fill("Umbenennung Im alten Boden zu Bodenrain – Playwright");
        form.locator("textarea[name='description']").fill("Golden-Path-Dossier für die Nomenklaturmutation Gemeinde Musterwil.");
        form.locator("select[name='position']").selectOption("4.3.2");
        form.locator("select[name='federation']").selectOption("AGI-NOM");
        form.locator("select[name='responsible']").selectOption("anna.mueller");
        form.locator("input[name='openingDate']").fill("2026-08-17");
        form.locator("button[type='submit']").click();

        String number = lastPathSegment(page);
        assertThat(number).startsWith("AGI-NOM-D-");
        assertThat(page.locator("#dossier-detail").innerText()).contains(number, "Offen");
        return number;
    }

    private static String openBusiness(Page page, String dossierNumber) {
        page.navigate("/geschaefte/neu");
        Locator form = page.locator("form[action='/geschaefte']");
        form.locator("select[name='dossierNumber']").selectOption(dossierNumber);
        form.locator("input[name='title']").fill("Antrag Gemeinde Musterwil: Umbenennung Flurname Im alten Boden");
        form.locator("textarea[name='shortDescription']").fill("Umbenennung des Flurnamens Im alten Boden in Bodenrain.");
        form.locator("select[name='type']").selectOption("NOMENKLATURMUTATION");
        form.locator("select[name='federation']").selectOption("AGI-NOM");
        form.locator("select[name='responsible']").selectOption("anna.mueller");
        form.locator("input[name='receivedDate']").fill("2026-08-17");
        form.locator("input[name='openingDate']").fill("2026-08-17");
        form.locator("input[name='dueDate']").fill("2026-09-30");
        form.locator("input[name='priority']").fill("2");
        form.locator("button[type='submit']").click();

        String number = lastPathSegment(page);
        assertThat(number).startsWith("AGI-NOM-G-");
        assertThat(page.locator("#geschaeft-detail").innerText()).contains(number, "Antrag eingegangen");
        return number;
    }

    private static void assignMunicipality(Page page, String businessNumber) {
        page.navigate("/geschaefte/" + businessNumber);
        Locator form = page.locator("form[action='/beteiligungen']");
        Locator partySelect = form.locator("select[name='beteiligterTid']");
        Locator municipality = partySelect.locator("option")
                .filter(new Locator.FilterOptions().setHasText("Gemeinde Musterwil"))
                .first();
        String tid = municipality.getAttribute("value");
        assertThat(tid).isNotBlank();
        partySelect.selectOption(tid);
        form.locator("select[name='rollenCode']").selectOption("ANTRAGSTELLERIN");
        form.locator("button[type='submit']").click();

        assertThat(page.locator("#geschaeft-detail").innerText())
                .contains("Gemeinde Musterwil", "Antragstellerin");
    }

    private static void registerDocument(
            Page page,
            String dossierNumber,
            String businessNumber,
            String title,
            String typeCode,
            Path file) {
        page.navigate("/dossiers/" + dossierNumber);
        Locator form = page.locator("form[action='/dossiers/" + dossierNumber + "/unterlagen']");
        form.locator("input[name='title']").fill(title);
        form.locator("select[name='typCode']").selectOption(typeCode);
        form.locator("input[type='file']").setInputFiles(file);
        form.locator("input[name='documentDate']").fill("2026-08-17");
        form.locator("input[name='dateiformat']").fill("PDF");
        form.locator("button[type='submit']").click();

        String tid = lastPathSegment(page);
        assertThat(tid).isNotBlank();
        Locator assign = page.locator("form[action='/unterlagen/" + tid + "/geschaeft']");
        assign.locator("input[name='geschaeftNumber']").fill(businessNumber);
        assign.locator("button[type='submit']").click();
        assertThat(page.locator("body").innerText()).contains(title, businessNumber, "Registriert");
    }

    private static void setProcessStatus(Page page, String businessNumber, String status, String comment) {
        page.navigate("/geschaefte/" + businessNumber);
        Locator form = page.locator("form[action='/geschaefte/" + businessNumber + "/prozessstatus']");
        form.locator("select[name='processStatusCode']").selectOption(status);
        form.locator("input[name='comment']").fill(comment);
        form.locator("button[type='submit']").click();
        assertThat(page.locator("#geschaeft-status-panel").innerText()).contains(statusName(status));
    }

    private static void setResult(Page page, String businessNumber, String result, String comment) {
        page.navigate("/geschaefte/" + businessNumber);
        Locator form = page.locator("form[action='/geschaefte/" + businessNumber + "/resultat']");
        form.locator("select[name='resultStatusCode']").selectOption(result);
        form.locator("input[name='comment']").fill(comment);
        form.locator("button[type='submit']").click();
        assertThat(page.locator("#geschaeft-status-panel").innerText()).contains("Genehmigt");
    }

    private static void createAndCompleteTask(Page page, String businessNumber) {
        page.navigate("/geschaefte/" + businessNumber);
        String taskTitle = "Fachliche Prüfung Bodenrain";
        Locator form = page.locator("form[action='/aufgaben']");
        form.locator("input[name='title']").fill(taskTitle);
        form.locator("select[name='typCode']").selectOption("AUFGABE_FACHLICHE_PRUEFUNG");
        form.locator("input[name='dueDate']").fill("2026-08-31");
        form.locator("input[name='priority']").fill("2");
        form.locator("input[name='assignedUsername']").fill("anna.mueller");
        form.locator("textarea[name='description']").fill("Namensform und Verwechslungsgefahr prüfen.");
        form.locator("button[type='submit']").click();

        Locator row = page.locator("#geschaeft-aufgaben li")
                .filter(new Locator.FilterOptions().setHasText(taskTitle))
                .first();
        assertThat(row.innerText()).contains(taskTitle);
        row.locator("form[action$='/complete'] button[type='submit']").click();
        assertThat(page.locator("#geschaeft-aufgaben").innerText()).contains(taskTitle, "Erledigt");
    }

    private static String statusName(String code) {
        return switch (code) {
            case "FACHLICHE_PRUEFUNG" -> "Fachliche Prüfung";
            case "ABGESCHLOSSEN" -> "Abgeschlossen";
            default -> code;
        };
    }

    private static String lastPathSegment(Page page) {
        String path = URI.create(page.url()).getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static Path minimalPdf(Path target, String title) throws IOException {
        String content = "%PDF-1.4\n% Mabillon Playwright\n1 0 obj\n<< /Type /Catalog /Title ("
                + title.replace("(", "[").replace(")", "]")
                + ") >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n";
        Files.writeString(target, content, StandardCharsets.ISO_8859_1);
        return target;
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
