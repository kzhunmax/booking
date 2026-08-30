package com.booking.app.architecture;

import static com.booking.app.architecture.ApplicationModules.BOOKING_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.COMMON_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.COMMON_WEB_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.CONFIG_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.DOMAIN_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.EXCEPTION_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.INFRASTRUCTURE_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.PAYMENT_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.RESOURCE_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.ROOT_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.WEB_PACKAGES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

@AnalyzeClasses(packages = ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchTest {

    private static final String WEB = "Web";
    private static final String ERROR_HANDLING = "ErrorHandling";
    private static final String API = "Api";
    private static final String PERSISTENCE = "Persistence";
    private static final String DOMAIN = "Domain";
    private static final String SHARED = "Shared";
    private static final String CONFIGURATION = "Configuration";

    @ArchTest
    static final ArchRule LAYERS_ARE_RESPECTED = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(ROOT_PACKAGE + "..")
            .layer(WEB)
            .definedBy(WEB_PACKAGES)
            .layer(ERROR_HANDLING)
            .definedBy(EXCEPTION_PACKAGES, COMMON_WEB_PACKAGE)
            .layer(API)
            .definedBy(BOOKING_API_PACKAGE, RESOURCE_API_PACKAGE, PAYMENT_API_PACKAGE)
            .layer(PERSISTENCE)
            .definedBy(INFRASTRUCTURE_PACKAGES)
            .layer(DOMAIN)
            .definedBy(DOMAIN_PACKAGES)
            .layer(SHARED)
            .definedBy(COMMON_PACKAGE)
            .layer(CONFIGURATION)
            .definedBy(CONFIG_PACKAGE)
            .whereLayer(WEB)
            .mayNotBeAccessedByAnyLayer()
            .whereLayer(ERROR_HANDLING)
            .mayNotBeAccessedByAnyLayer()
            .whereLayer(CONFIGURATION)
            .mayNotBeAccessedByAnyLayer()
            .whereLayer(API)
            .mayOnlyBeAccessedByLayers(WEB, ERROR_HANDLING, PERSISTENCE, DOMAIN)
            .whereLayer(PERSISTENCE)
            .mayOnlyBeAccessedByLayers(API)
            .whereLayer(DOMAIN)
            .mayOnlyBeAccessedByLayers(API, PERSISTENCE);

    @ArchTest
    static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that()
            .resideInAPackage(DOMAIN_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(WEB_PACKAGES, INFRASTRUCTURE_PACKAGES, EXCEPTION_PACKAGES);

    @ArchTest
    static final ArchRule DOMAIN_IS_FREE_OF_WEB_AND_DATA_ACCESS_CONCERNS = noClasses()
            .that()
            .resideInAPackage(DOMAIN_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "org.springframework.stereotype..",
                    "org.springframework.transaction..",
                    "org.springframework.data.repository..",
                    "org.springframework.data.jpa.repository..")
            .as("domain classes should contain business rules only, not web or data-access plumbing");

    @ArchTest
    static final ArchRule INFRASTRUCTURE_DOES_NOT_DEPEND_ON_THE_WEB_LAYER = noClasses()
            .that()
            .resideInAPackage(INFRASTRUCTURE_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(WEB_PACKAGES);

    @ArchTest
    static final ArchRule CONTROLLERS_GO_THROUGH_THE_MODULE_API = noClasses()
            .that()
            .resideInAPackage(WEB_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(DOMAIN_PACKAGES, INFRASTRUCTURE_PACKAGES)
            .because("controllers must not bypass the module service and touch entities or repositories");

    @ArchTest
    static final ArchRule ENTITIES_NEVER_REACH_THE_WEB_LAYER = noClasses()
            .that()
            .resideInAPackage(WEB_PACKAGES)
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Entity.class)
            .because("JPA entities must not be serialized into HTTP responses");

    @ArchTest
    static final ArchRule REPOSITORIES_ARE_ONLY_USED_BY_THE_MODULE_SERVICE = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .onlyHaveDependentClassesThat()
            .resideInAnyPackage(
                    BOOKING_API_PACKAGE, RESOURCE_API_PACKAGE, PAYMENT_API_PACKAGE, INFRASTRUCTURE_PACKAGES);
}
