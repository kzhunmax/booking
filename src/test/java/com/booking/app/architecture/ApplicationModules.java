package com.booking.app.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.Set;

/**
 * Describes the module layout of the monolith so the architecture rules can be expressed generically:
 * every direct subpackage of the root package is a business module, except the shared ones.
 */
final class ApplicationModules {

    static final String ROOT_PACKAGE = "com.booking.app";
    static final String BOOKING_API_PACKAGE = ROOT_PACKAGE + ".booking";
    static final String RESOURCE_API_PACKAGE = ROOT_PACKAGE + ".resource";
    static final String PAYMENT_API_PACKAGE = ROOT_PACKAGE + ".payment";
    static final String NOTIFICATION_API_PACKAGE = ROOT_PACKAGE + ".notification";
    static final String AUTH_API_PACKAGE = ROOT_PACKAGE + ".auth";
    static final String COMMON_PACKAGE = ROOT_PACKAGE + ".common";
    static final String COMMON_WEB_PACKAGE = COMMON_PACKAGE + ".web";
    static final String CONFIG_PACKAGE = ROOT_PACKAGE + ".config";

    static final String INTERNAL_PACKAGES = "..internal..";
    static final String DOMAIN_PACKAGES = "..internal.domain..";
    static final String APPLICATION_PACKAGES = "..internal.application..";
    static final String INFRASTRUCTURE_PACKAGES = "..internal.infrastructure..";
    static final String WEB_PACKAGES = "..internal.web..";
    static final String EXCEPTION_PACKAGES = "..internal.exception..";

    private static final Set<String> SHARED_SEGMENTS = Set.of("common", "config");
    private static final String SHARED = "<shared>";

    private ApplicationModules() {}

    static DescribedPredicate<List<JavaClass>> anyTypeResidingInAnInternalPackage() {
        DescribedPredicate<JavaClass> internal = JavaClass.Predicates.resideInAPackage(INTERNAL_PACKAGES);
        return new DescribedPredicate<>("any type residing in an internal package") {
            @Override
            public boolean test(List<JavaClass> types) {
                return types.stream().anyMatch(internal);
            }
        };
    }

    static ArchCondition<JavaClass> onlyBeUsedWithinTheirOwnModule() {
        return new ArchCondition<>("only be used from within their own module") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String owningModule = moduleOf(javaClass);
                for (Dependency dependency : javaClass.getDirectDependenciesToSelf()) {
                    if (!owningModule.equals(moduleOf(dependency.getOriginClass()))) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static String moduleOf(JavaClass javaClass) {
        String prefix = ROOT_PACKAGE + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return SHARED;
        }
        String firstSegment = packageName.substring(prefix.length()).split("\\.", 2)[0];
        return SHARED_SEGMENTS.contains(firstSegment) ? SHARED : firstSegment;
    }
}
