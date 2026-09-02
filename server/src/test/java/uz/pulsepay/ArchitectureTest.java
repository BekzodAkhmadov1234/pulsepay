package uz.pulsepay;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing the N-Tier Layered Architecture rules.
 *
 * Layer ordering (top to bottom):
 *   controller → service → repository → domain
 *
 * Key invariants:
 *  - Controllers must not access repositories directly (must go through service)
 *  - Services must not access controllers
 *  - Domain layer must not import service, controller, or repository
 *  - Ledger has no REST endpoint (LedgerService is called in-process only)
 */
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .importPath(Paths.get("build/classes/java/main"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Layer dependency rules
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void controllers_must_not_access_repositories_directly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.controller..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.repository..");
        rule.check(importedClasses);
    }

    @Test
    void services_must_not_access_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.service..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.controller..");
        rule.check(importedClasses);
    }

    @Test
    void domain_must_not_import_service_layer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.domain..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.service..");
        rule.check(importedClasses);
    }

    @Test
    void domain_must_not_import_repository_layer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.domain..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.repository..");
        rule.check(importedClasses);
    }

    @Test
    void domain_must_not_import_controller_layer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.domain..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.controller..");
        rule.check(importedClasses);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ledger has no REST endpoint — in-process only
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void ledger_service_has_no_rest_controller() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.service..")
                .and().haveSimpleName("LedgerService")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    @Test
    void no_controller_is_named_ledger_controller() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.controller..")
                .should().haveSimpleName("LedgerController");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utils layer must not access business layers
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void utils_security_must_not_access_service_layer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.utils.security..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.service..");
        rule.check(importedClasses);
    }
}
