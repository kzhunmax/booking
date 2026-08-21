package com.booking.app;

import org.springframework.boot.SpringApplication;

public class TestAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(BookingApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
