package com.booking.app.architecture;

import static com.booking.app.architecture.ApplicationModules.APPLICATION_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.AUTH_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.BOOKING_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.COMMON_WEB_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.CONFIG_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.DOMAIN_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.EXCEPTION_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.INFRASTRUCTURE_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.NOTIFICATION_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.PAYMENT_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.RESOURCE_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.ROOT_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.WEB_PACKAGES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@AnalyzeClasses(packages = ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ConventionArchTest {

    @ArchTest
    static final ArchRule CONTROLLERS_ARE_ANNOTATED_AND_LIVE_IN_THE_WEB_PACKAGE = classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .beAnnotatedWith(RestController.class)
            .andShould()
            .resideInAPackage(WEB_PACKAGES);

    @ArchTest
    static final ArchRule REST_CONTROLLERS_ARE_NAMED_CONTROLLER =
            classes().that().areAnnotatedWith(RestController.class).should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule SERVICE_INTERFACES_ARE_THE_PUBLISHED_MODULE_API = classes()
            .that()
            .haveSimpleNameEndingWith("Service")
            .and()
            .areInterfaces()
            .should()
            .resideInAnyPackage(
                    BOOKING_API_PACKAGE,
                    RESOURCE_API_PACKAGE,
                    PAYMENT_API_PACKAGE,
                    NOTIFICATION_API_PACKAGE,
                    AUTH_API_PACKAGE)
            .because("the service interface is the only entry point a neighbouring module is allowed to call");

    @ArchTest
    static final ArchRule SERVICE_IMPLEMENTATIONS_LIVE_IN_APPLICATION_PACKAGES = classes()
            .that()
            .areAnnotatedWith(Service.class)
            .should()
            .resideInAPackage(APPLICATION_PACKAGES)
            .andShould()
            .haveSimpleNameEndingWith("Service")
            .because("service implementations belong in internal.application and are wired via Spring");

    @ArchTest
    static final ArchRule REPOSITORIES_ARE_SPRING_DATA_INTERFACES = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .beInterfaces()
            .andShould()
            .beAnnotatedWith(Repository.class)
            .andShould()
            .beAssignableTo(org.springframework.data.repository.Repository.class)
            .andShould()
            .resideInAPackage(INFRASTRUCTURE_PACKAGES);

    @ArchTest
    static final ArchRule ENTITIES_LIVE_IN_THE_DOMAIN_PACKAGE =
            classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage(DOMAIN_PACKAGES);

    @ArchTest
    static final ArchRule REQUEST_TYPES_ARE_WEB_LAYER_RECORDS = classes()
            .that()
            .haveSimpleNameEndingWith("Request")
            .should()
            .beRecords()
            .andShould()
            .resideInAPackage(WEB_PACKAGES);

    @ArchTest
    static final ArchRule RESPONSE_TYPES_ARE_PUBLISHED_RECORDS = classes()
            .that()
            .haveSimpleNameEndingWith("Response")
            .should()
            .beRecords()
            .andShould()
            .resideInAnyPackage(BOOKING_API_PACKAGE, RESOURCE_API_PACKAGE, PAYMENT_API_PACKAGE, AUTH_API_PACKAGE)
            .because("responses are part of the module API and must be immutable, entity-free carriers");

    @ArchTest
    static final ArchRule EXCEPTIONS_ARE_PART_OF_THE_MODULE_API = classes()
            .that()
            .areAssignableTo(RuntimeException.class)
            .should()
            .haveSimpleNameEndingWith("Exception")
            .andShould()
            .resideInAnyPackage(
                    BOOKING_API_PACKAGE,
                    RESOURCE_API_PACKAGE,
                    PAYMENT_API_PACKAGE,
                    NOTIFICATION_API_PACKAGE,
                    AUTH_API_PACKAGE)
            .because("callers of a module must be able to catch its failures without reaching into internals");

    @ArchTest
    static final ArchRule EXCEPTION_HANDLERS_ARE_ISOLATED = classes()
            .that()
            .areAnnotatedWith(RestControllerAdvice.class)
            .should()
            .haveSimpleNameEndingWith("ExceptionHandler")
            .andShould()
            .resideInAnyPackage(EXCEPTION_PACKAGES, COMMON_WEB_PACKAGE);

    @ArchTest
    static final ArchRule STATIC_HELPERS_ARE_FINAL_AND_NOT_INSTANTIABLE = classes()
            .that()
            .haveSimpleNameEndingWith("Mapper")
            .or()
            .haveSimpleNameEndingWith("Specifications")
            .should()
            .haveModifier(JavaModifier.FINAL)
            .andShould()
            .haveOnlyPrivateConstructors()
            .andShould()
            .resideInAPackage(INFRASTRUCTURE_PACKAGES);

    @ArchTest
    static final ArchRule SPRING_CONFIGURATION_LIVES_IN_THE_CONFIG_PACKAGE =
            classes().that().areAnnotatedWith(Configuration.class).should().resideInAPackage(CONFIG_PACKAGE);
}
