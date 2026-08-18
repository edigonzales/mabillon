package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import guru.interlis.mabillon.beteiligung.BeteiligterSearchCriteria;
import guru.interlis.mabillon.beteiligung.BeteiligterService;
import guru.interlis.mabillon.beteiligung.CreateBeteiligterCommand;
import guru.interlis.mabillon.dossier.DossierQueryService;
import guru.interlis.mabillon.dossier.DossierSearchCriteria;
import guru.interlis.mabillon.dossier.DossierService;
import guru.interlis.mabillon.dossier.DossierView;
import guru.interlis.mabillon.dossier.OpenDossierCommand;
import guru.interlis.mabillon.geschaeft.GeschaeftQueryService;
import guru.interlis.mabillon.geschaeft.GeschaeftSearchCriteria;
import guru.interlis.mabillon.geschaeft.GeschaeftService;
import guru.interlis.mabillon.geschaeft.GeschaeftView;
import guru.interlis.mabillon.geschaeft.OpenGeschaeftCommand;
import guru.interlis.mabillon.numbering.DossierNumber;
import guru.interlis.mabillon.search.GlobalSearchCriteria;
import guru.interlis.mabillon.search.GlobalSearchHit;
import guru.interlis.mabillon.search.GlobalSearchService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class Phase11DatabaseSearchPaginationIntegrationTest {

    private static final String PREFIX = "Phase 11.13";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mabillon")
            .withUsername("mabillon")
            .withPassword("mabillon");

    @Autowired
    private DossierService dossierService;

    @Autowired
    private DossierQueryService dossierQueryService;

    @Autowired
    private GeschaeftService geschaeftService;

    @Autowired
    private GeschaeftQueryService geschaeftQueryService;

    @Autowired
    private BeteiligterService beteiligterService;

    @Autowired
    private GlobalSearchService globalSearchService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("mabillon.cayenne.url", POSTGRES::getJdbcUrl);
        registry.add("mabillon.cayenne.username", POSTGRES::getUsername);
        registry.add("mabillon.cayenne.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void importFixtures() {
        InterlisTestFixture.importGoldenPath(POSTGRES);
    }

    @Test
    @WithMockUser(username = "admin", roles = "MABILLON_ADMIN")
    void databaseQueriesReturnStableCountsAndPagesAcrossObjectTypes() {
        List<DossierView> dossiers = java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(index -> dossierService.open(new OpenDossierCommand(
                        PREFIX + " Dossier " + index,
                        "DB-side paging fixture",
                        "4.3.2",
                        "AGI-NOM",
                        "anna.mueller",
                        LocalDate.of(2026, 8, 17))))
                .toList();

        DossierNumber businessDossier = DossierNumber.parse(dossiers.getFirst().number());
        java.util.stream.IntStream.rangeClosed(1, 3)
                .forEach(index -> geschaeftService.open(new OpenGeschaeftCommand(
                        businessDossier,
                        PREFIX + " Geschäft " + index,
                        "DB-side paging fixture",
                        "NOMENKLATURMUTATION",
                        "AGI-NOM",
                        "anna.mueller",
                        LocalDate.of(2026, 8, 17),
                        LocalDate.of(2026, 8, 17),
                        LocalDate.of(2026, 9, 30),
                        2)));

        java.util.stream.IntStream.rangeClosed(1, 3)
                .forEach(index -> beteiligterService.create(new CreateBeteiligterCommand(
                        "Organisation",
                        PREFIX + " Partei " + index,
                        null,
                        PREFIX + " Organisation " + index,
                        "phase11-13-" + index + "@example.test",
                        null,
                        null,
                        "phase11-13-" + index)));

        var dossierPage0 = dossierQueryService.search(
                new DossierSearchCriteria(null, PREFIX + " Dossier", null, null, null,
                        null, null, null, null),
                0, 2);
        var dossierPage1 = dossierQueryService.search(
                new DossierSearchCriteria(null, PREFIX + " Dossier", null, null, null,
                        null, null, null, null),
                1, 2);
        assertThat(dossierPage0.totalElements()).isEqualTo(3);
        assertThat(dossierPage0.items()).hasSize(2);
        assertThat(dossierPage0.hasNext()).isTrue();
        assertThat(dossierPage1.items()).hasSize(1);
        assertNoOverlap(
                dossierPage0.items().stream().map(DossierView::number).toList(),
                dossierPage1.items().stream().map(DossierView::number).toList());

        var businessPage0 = geschaeftQueryService.search(
                new GeschaeftSearchCriteria(null, PREFIX + " Geschäft", null, null, null,
                        null, null, null, null),
                0, 2);
        var businessPage1 = geschaeftQueryService.search(
                new GeschaeftSearchCriteria(null, PREFIX + " Geschäft", null, null, null,
                        null, null, null, null),
                1, 2);
        assertThat(businessPage0.totalElements()).isEqualTo(3);
        assertThat(businessPage0.items()).hasSize(2);
        assertThat(businessPage1.items()).hasSize(1);
        assertNoOverlap(
                businessPage0.items().stream().map(GeschaeftView::number).toList(),
                businessPage1.items().stream().map(GeschaeftView::number).toList());

        var partyPage0 = beteiligterService.search(
                new BeteiligterSearchCriteria(PREFIX + " Partei", null, null), 0, 2);
        var partyPage1 = beteiligterService.search(
                new BeteiligterSearchCriteria(PREFIX + " Partei", null, null), 1, 2);
        assertThat(partyPage0.totalElements()).isEqualTo(3);
        assertThat(partyPage0.items()).hasSize(2);
        assertThat(partyPage1.items()).hasSize(1);
        assertNoOverlap(
                partyPage0.items().stream().map(value -> value.tid().toString()).toList(),
                partyPage1.items().stream().map(value -> value.tid().toString()).toList());

        Set<String> globalKeys = new HashSet<>();
        long total = -1;
        for (int page = 0; page < 5; page++) {
            var result = globalSearchService.search(new GlobalSearchCriteria(PREFIX, page, 2));
            if (total < 0) {
                total = result.totalElements();
            }
            assertThat(result.totalElements()).isEqualTo(total);
            result.items().stream().map(Phase11DatabaseSearchPaginationIntegrationTest::key).forEach(key ->
                    assertThat(globalKeys.add(key)).as("global result appears only once: %s", key).isTrue());
        }
        assertThat(total).isEqualTo(9);
        assertThat(globalKeys).hasSize(9);
    }

    private static void assertNoOverlap(List<String> first, List<String> second) {
        assertThat(first).doesNotContainAnyElementsOf(second);
    }

    private static String key(GlobalSearchHit hit) {
        return hit.objectType() + ":" + hit.identifier();
    }
}
