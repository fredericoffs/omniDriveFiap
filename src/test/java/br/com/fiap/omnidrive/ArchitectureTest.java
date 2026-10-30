package br.com.fiap.omnidrive;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Verifica a regra de dependência da Clean Architecture: {@code domain} só usa Java, {@code application} só usa Java e {@code domain}, e nenhuma
 * camada acessa {@code infrastructure}.
 */
@AnalyzeClasses(packages = "br.com.fiap.omnidrive", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String DOMAIN = "br.com.fiap.omnidrive.domain..";
  private static final String APPLICATION = "br.com.fiap.omnidrive.application..";
  private static final String INFRASTRUCTURE = "br.com.fiap.omnidrive.infrastructure..";

  @ArchTest
  static final ArchRule domainDependsOnlyOnJava = classes()
      .that().resideInAPackage(DOMAIN)
      .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN)
      .allowEmptyShould(true);

  @ArchTest
  static final ArchRule applicationDependsOnlyOnJavaAndDomain = classes()
      .that().resideInAPackage(APPLICATION)
      .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN, APPLICATION)
      .allowEmptyShould(true);

  @ArchTest
  static final ArchRule layersFollowTheDependencyRule = layeredArchitecture()
      .consideringAllDependencies()
      .withOptionalLayers(true)
      .layer("Domain").definedBy(DOMAIN)
      .layer("Application").definedBy(APPLICATION)
      .layer("Infrastructure").definedBy(INFRASTRUCTURE)
      .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
      .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
      .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");
}
