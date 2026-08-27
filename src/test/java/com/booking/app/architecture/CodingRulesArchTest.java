package com.booking.app.architecture;

import static com.booking.app.architecture.ApplicationModules.BOOKING_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.RESOURCE_API_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.ROOT_PACKAGE;
import static com.booking.app.architecture.ApplicationModules.WEB_PACKAGES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(packages = ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class CodingRulesArchTest {

    @ArchTest
    static final ArchRule NO_ACCESS_TO_STANDARD_STREAMS = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule NO_JAVA_UTIL_LOGGING = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule NO_JODA_TIME = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

    @ArchTest
    static final ArchRule NO_FIELD_INJECTION = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule NO_LEGACY_DATE_API = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Date")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Calendar")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.sql.Timestamp")
            .as("no classes should use the legacy date API");

    @ArchTest
    static final ArchRule BUSINESS_LOGIC_READS_TIME_FROM_THE_INJECTED_CLOCK = noClasses()
            .that()
            .resideInAnyPackage(BOOKING_API_PACKAGE + "..", RESOURCE_API_PACKAGE + "..")
            .should()
            .callMethod(Instant.class, "now")
            .orShould()
            .callMethod(LocalDate.class, "now")
            .orShould()
            .callMethod(LocalDateTime.class, "now")
            .orShould()
            .callMethod(ZonedDateTime.class, "now")
            .orShould()
            .callMethod(System.class, "currentTimeMillis")
            .because("time has to come from the injected java.time.Clock so that behaviour stays testable");

    @ArchTest
    static final ArchRule TRANSACTIONS_ARE_DECLARED_ON_THE_SERVICE_LAYER = methods()
            .that()
            .areAnnotatedWith(Transactional.class)
            .should()
            .beDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .andShould()
            .bePublic()
            .because("Spring's proxy based transaction management only applies to public service methods");

    @ArchTest
    static final ArchRule ONLY_SPRINGS_TRANSACTIONAL_ANNOTATION_IS_USED = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("jakarta.transaction.Transactional")
            .as("no classes should use jakarta.transaction.Transactional");

    @ArchTest
    static final ArchRule CONTROLLERS_ARE_NOT_TRANSACTIONAL = noClasses()
            .that()
            .resideInAPackage(WEB_PACKAGES)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(Transactional.class.getName())
            .because("a transaction must not span the whole request handling");

    @ArchTest
    static final ArchRule LOGGERS_ARE_PRIVATE_STATIC_FINAL = fields().that()
            .haveRawType(Logger.class)
            .should()
            .bePrivate()
            .andShould()
            .beStatic()
            .andShould()
            .beFinal();
}
