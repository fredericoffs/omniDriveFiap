package br.com.fiap.omnidrive;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "br.com.fiap.omnidrive", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String DOMAIN = "br.com.fiap.omnidrive.domain..";
    private static final String APPLICATION = "br.com.fiap.omnidrive.application..";
    private static final String INFRASTRUCTURE = "br.com.fiap.omnidrive.infrastructure..";

    @ArchTest
    static final ArchRule camadasRespeitamARegraDeDependencia = layeredArchitecture()
            .consideringAllDependencies()
            .withOptionalLayers(true)
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domainDependeSomenteDeJava = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDependeSomenteDeJavaEDomain = classes()
            .that().resideInAPackage(APPLICATION)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN, APPLICATION)
            .allowEmptyShould(true);
}
