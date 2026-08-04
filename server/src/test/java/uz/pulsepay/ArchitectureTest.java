package uz.pulsepay;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing stage boundaries and hexagonal architecture rules.
 * Risk #9 mitigation: ensures no Stage 1 package imports from Stage 2/3 packages.
 */
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        // importPath is used instead of importPackages to reliably locate classes
        // in the Gradle build directory during test execution.
        importedClasses = new ClassFileImporter()
                .importPath(Paths.get("build/classes/java/main"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Stage boundary: Stage 1 must not import Stage 2/3 packages
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void stage1_transfer_must_not_import_merchant() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.merchant..");
        rule.check(importedClasses);
    }

    @Test
    void stage1_transfer_must_not_import_business() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.business..");
        rule.check(importedClasses);
    }

    @Test
    void stage1_transfer_must_not_import_settlement() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.settlement..");
        rule.check(importedClasses);
    }

    @Test
    void stage1_identity_must_not_import_merchant() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.identity..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.merchant..");
        rule.check(importedClasses);
    }

    @Test
    void identity_must_not_import_card() {
        // identity → shared only; card deactivation cross-module via CardDeactivationPort in shared
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.identity..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.card..");
        rule.check(importedClasses);
    }

    @Test
    void stage1_ledger_must_not_import_merchant() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.ledger..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.merchant..");
        rule.check(importedClasses);
    }

    @Test
    void stage1_fee_must_not_import_merchant() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.fee..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.merchant..");
        rule.check(importedClasses);
    }

    @Test
    void stage1_limit_must_not_import_merchant() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.limit..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.merchant..");
        rule.check(importedClasses);
    }

    @Test
    void stage1_compliance_must_not_import_merchant() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.compliance..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.merchant..");
        rule.check(importedClasses);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ledger has no REST inbound adapter
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void ledger_has_no_rest_controller() {
        // ledger.adapter.in is intentionally empty; scan the whole ledger package so
        // the rule has classes to check (satisfying failOnEmptyShould).
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.ledger..")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController");
        rule.check(importedClasses);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // No module imports another module's adapter layer
    // (only domain.port interfaces are cross-module)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void transfer_must_not_import_identity_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.identity.adapter..");
        rule.check(importedClasses);
    }

    @Test
    void transfer_must_not_import_ledger_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.ledger.adapter..");
        rule.check(importedClasses);
    }

    @Test
    void transfer_must_not_import_fee_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.fee.adapter..");
        rule.check(importedClasses);
    }

    @Test
    void transfer_must_not_import_limit_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.limit.adapter..");
        rule.check(importedClasses);
    }

    @Test
    void transfer_must_not_import_compliance_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("uz.pulsepay.transfer..")
                .should().accessClassesThat().resideInAPackage("uz.pulsepay.compliance.adapter..");
        rule.check(importedClasses);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Domain model must not contain JPA or Jackson annotations
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void domain_models_must_not_use_jpa_annotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().beAnnotatedWith("jakarta.persistence.Entity");
        rule.check(importedClasses);
    }

    @Test
    void domain_models_must_not_use_table_annotation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().beAnnotatedWith("jakarta.persistence.Table");
        rule.check(importedClasses);
    }
}
