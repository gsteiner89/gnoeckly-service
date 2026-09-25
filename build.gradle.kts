plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "at.stoneforge.gnoeckly"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    // mavenLocal zuerst: midgard-core kommt als SNAPSHOT aus ~/.m2 (publishToMavenLocal in
    // I:\Entwicklung\midgard-framework), inklusive der test-fixtures-Variante (Gradle Module
    // Metadata, .module-Datei) - die gibt es nur dort.
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Bringt ueber `api`-Deklarationen in midgard-core bereits Spring Web/Data-JPA/Security/
    // Validation, hibernate-envers und Flyway transitiv mit (siehe midgard-core build.gradle.kts).
    implementation("at.stoneforge.midgard:midgard-core:0.1.0-SNAPSHOT")

    // Eigene Flyway-Skripte unter classpath:db/migration (eigene flyway_schema_history, Midgards
    // laufen getrennt unter midgard/db/migration) - wie in saga-service explizit deklariert.
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // OpenAPI-Spec unter /v3/api-docs fuer die TypeScript-Client-Generierung in gnoeckly-web
    // (openapi-generator-cli, siehe gnoeckly-web/package.json). Swagger-UI nur im dev-Profil
    // freigeschaltet (midgard.security.public-paths in application-dev.yml).
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    // /actuator/health fuer den Hosting-Healthcheck (public-path in application.yml).
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Server-Push (FCM/APNs) ueber Firebase Admin; nur aktiv mit gnoeckly.push.enabled=true (docs/push-fcm.md).
    implementation("com.google.firebase:firebase-admin:9.4.3")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // EmbeddedPostgresTestSupport aus midgard-core: echter Postgres-Prozess ohne Docker
    // (io.zonky.test:embedded-postgres kommt transitiv ueber testFixturesApi mit). Tenant-Filter,
    // Envers und Flyway-Skripte werden damit gegen eine echte DB getestet (midgard CLAUDE.md).
    testImplementation(testFixtures("at.stoneforge.midgard:midgard-core:0.1.0-SNAPSHOT"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Siehe midgard-framework/build.gradle.kts fuer die ausfuehrliche Begruendung: ohne "-parameters"
// scheitern @PathVariable/@RequestParam ohne explizit angegebenen Namen zur Laufzeit, sobald ueber
// die Gradle-CLI statt ueber IntelliJs eigenen Compiler gebaut wird.
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
