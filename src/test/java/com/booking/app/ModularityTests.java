package com.booking.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(BookingApplication.class);

    @Test
    void verifiesModularity() {
        modules.verify();
    }
}
