package dev.springawsportfolio.portfolio.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("dev.springawsportfolio.portfolio");
    }

    @Test
    void domainMustNotDependOnWebOrInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..web..", "..infrastructure..")
                .allowEmptyShould(true);

        rule.check(productionClasses);
    }

    @Test
    void applicationMustNotDependOnWeb() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..web..")
                .allowEmptyShould(true);

        rule.check(productionClasses);
    }

    @Test
    void awsSdkMustRemainOutsideDomainApplicationAndWeb() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "..domain..",
                        "..application..",
                        "..web.."
                )
                .should().dependOnClassesThat()
                .resideInAPackage("software.amazon.awssdk..")
                .allowEmptyShould(true);

        rule.check(productionClasses);
    }
}
