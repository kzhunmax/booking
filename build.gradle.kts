plugins {
    java
    checkstyle
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.0"
    id("net.ltgt.errorprone") version "5.1.0"
}

group = "com.booking"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

checkstyle {
    toolVersion = "14.0.0"
    configDirectory = file("config")
    maxWarnings = 0
}

spotless {
    java {
        target(
            "src/*/java/**/*.java",
        )
        targetExclude("**/build/generated/**")
        palantirJavaFormat("2.97.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

jacoco {
    toolVersion = "0.8.15"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    errorprone("com.google.errorprone:error_prone_core:2.50.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required = true
    }
}

tasks.register<Exec>("installGitHooks") {
    group = "setup"
    description = "Configures Git to use the repository hooks"
    commandLine("git", "config", "--local", "core.hooksPath", ".githooks")
}
