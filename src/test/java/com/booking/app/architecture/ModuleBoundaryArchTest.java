package com.booking.app.architecture;

import static com.booking.app.architecture.ApplicationModules.AUTH_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.BOOKING_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.COMMON_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.COMMON_WEB_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.CONFIG_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.INTERNAL_PACKAGES;
import static com.booking.app.architecture.ApplicationModules.NOTIFICATION_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.PAYMENT_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.RESOURCE_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.ROOT_PACKAGE;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

@AnalyzeClasses(packages = ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryArchTest {

    @ArchTest
    static final ArchRule MODULE_INTERNALS_ARE_MODULE_PRIVATE = classes()
            .that(resideInAPackage(INTERNAL_PACKAGES))
            .should(ApplicationModules.onlyBeUsedWithinTheirOwnModule())
            .as("classes below an 'internal' package should only be used within their own module")
            .because("modules may only talk to each other through their published API package");

    @ArchTest
    static final ArchRule BOOKING_INTERNALS_STAY_INSIDE_THE_BOOKING_MODULE = noClasses()
            .that()
            .resideOutsideOfPackage(BOOKING_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BOOKING_API_PACKAGE + ".internal..")
            .as("no class outside the booking module should depend on com.booking.app.booking.internal..");

    @ArchTest
    static final ArchRule RESOURCE_INTERNALS_STAY_INSIDE_THE_RESOURCE_MODULE = noClasses()
            .that()
            .resideOutsideOfPackage(RESOURCE_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(RESOURCE_API_PACKAGE + ".internal..")
            .as("no class outside the resource module should depend on com.booking.app.resource.internal..");

    @ArchTest
    static final ArchRule PAYMENT_INTERNALS_STAY_INSIDE_THE_PAYMENT_MODULE = noClasses()
            .that()
            .resideOutsideOfPackage(PAYMENT_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PAYMENT_API_PACKAGE + ".internal..")
            .as("no class outside the payment module should depend on com.booking.app.payment.internal..");

    @ArchTest
    static final ArchRule NOTIFICATION_INTERNALS_STAY_INSIDE_THE_NOTIFICATION_MODULE = noClasses()
            .that()
            .resideOutsideOfPackage(NOTIFICATION_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(NOTIFICATION_API_PACKAGE + ".internal..")
            .as("no class outside the notification module should depend on com.booking.app.notification.internal..");

    @ArchTest
    static final ArchRule AUTH_INTERNALS_STAY_INSIDE_THE_AUTH_MODULE = noClasses()
            .that()
            .resideOutsideOfPackage(AUTH_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(AUTH_API_PACKAGE + ".internal..")
            .as("no class outside the auth module should depend on com.booking.app.auth.internal..");

    @ArchTest
    static final ArchRule MODULES_ARE_FREE_OF_CYCLES =
            slices().matching(ROOT_PACKAGE + ".(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule RESOURCE_MODULE_IS_INDEPENDENT_OF_THE_BOOKING_MODULE = noClasses()
            .that()
            .resideInAPackage(RESOURCE_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BOOKING_API_PACKAGE + "..")
            .because("resources are the upstream concept: bookings reference resources, never the other way around");

    @ArchTest
    static final ArchRule RESOURCE_MODULE_IS_INDEPENDENT_OF_THE_PAYMENT_MODULE = noClasses()
            .that()
            .resideInAPackage(RESOURCE_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PAYMENT_API_PACKAGE + "..")
            .because("resources are upstream of payments");

    @ArchTest
    static final ArchRule RESOURCE_MODULE_IS_INDEPENDENT_OF_THE_NOTIFICATION_MODULE = noClasses()
            .that()
            .resideInAPackage(RESOURCE_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(NOTIFICATION_API_PACKAGE + "..")
            .because("resources are upstream of notifications");

    @ArchTest
    static final ArchRule PAYMENT_MODULE_IS_INDEPENDENT_OF_THE_NOTIFICATION_MODULE = noClasses()
            .that()
            .resideInAPackage(PAYMENT_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(NOTIFICATION_API_PACKAGE + "..")
            .because("payments are independent of notifications");

    @ArchTest
    static final ArchRule NOTIFICATION_MODULE_IS_INDEPENDENT_OF_THE_RESOURCE_MODULE = noClasses()
            .that()
            .resideInAPackage(NOTIFICATION_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(RESOURCE_API_PACKAGE + "..")
            .because("notifications only care about sending messages, not resource entities");

    @ArchTest
    static final ArchRule NOTIFICATION_MODULE_IS_INDEPENDENT_OF_THE_PAYMENT_MODULE = noClasses()
            .that()
            .resideInAPackage(NOTIFICATION_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PAYMENT_API_PACKAGE + "..")
            .because("notifications only care about sending messages, not payment entities");

    @ArchTest
    static final ArchRule NOTIFICATION_MODULE_IS_INDEPENDENT_OF_THE_BOOKING_MODULE = noClasses()
            .that()
            .resideInAPackage(NOTIFICATION_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BOOKING_API_PACKAGE + "..")
            .because("notifications are invoked with primitive IDs and strings, not booking internal domain models");

    @ArchTest
    static final ArchRule BOOKING_MODULE_IS_INDEPENDENT_OF_THE_PAYMENT_MODULE = noClasses()
            .that()
            .resideInAPackage(BOOKING_API_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PAYMENT_API_PACKAGE + "..")
            .because("bookings are upstream of payments: payments depend on bookings, never the other way around");

    @ArchTest
    static final ArchRule SHARED_CODE_DOES_NOT_DEPEND_ON_BUSINESS_MODULES = noClasses()
            .that()
            .resideInAnyPackage(COMMON_PACKAGE, COMMON_WEB_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    BOOKING_API_PACKAGE + "..",
                    RESOURCE_API_PACKAGE + "..",
                    PAYMENT_API_PACKAGE + "..",
                    NOTIFICATION_API_PACKAGE + "..",
                    AUTH_API_PACKAGE + "..")
            .because("com.booking.app.common is a shared kernel and must not know about concrete modules");

    @ArchTest
    static final ArchRule CONFIGURATION_DOES_NOT_REACH_INTO_MODULE_INTERNALS = noClasses()
            .that()
            .resideInAPackage(CONFIG_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(INTERNAL_PACKAGES);

    @ArchTest
    static final ArchRule MODULE_API_DOES_NOT_LEAK_INTERNAL_TYPES = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage(
                    BOOKING_API_PACKAGE,
                    RESOURCE_API_PACKAGE,
                    PAYMENT_API_PACKAGE,
                    NOTIFICATION_API_PACKAGE,
                    AUTH_API_PACKAGE)
            .and()
            .arePublic()
            .should()
            .notHaveRawReturnType(resideInAPackage(INTERNAL_PACKAGES))
            .as("public methods of a module API should not return internal types");

    @ArchTest
    static final ArchRule SERVICES_ONLY_ACCEPT_PUBLISHED_TYPES = methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .and()
            .arePublic()
            .should()
            .notHaveRawParameterTypes(ApplicationModules.anyTypeResidingInAnInternalPackage())
            .as("public service methods should not accept internal types as parameters");
}
