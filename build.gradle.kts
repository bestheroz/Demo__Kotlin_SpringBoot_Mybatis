plugins {
    val kotlinVersion = "2.2.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion

    id("com.google.devtools.ksp") version "2.2.20-2.0.3"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.0.0"
    id("com.github.ben-manes.versions") version "0.53.0"
    idea
}

group = "com.github.bestheroz"
version = "0.0.1"
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.6")
    }
}

dependencies {
    // Kotlin
    implementation(kotlin("noarg"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    ksp("com.google.dagger:dagger-compiler:2.57.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.apache.commons:commons-lang3")

    // Database
    implementation("com.mysql:mysql-connector-j:9.4.0")
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.12.0")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5")
    implementation("io.github.bestheroz:mybatis-repository:0.8.1")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.hibernate.orm:hibernate-core:7.1.2.Final")

    // Logging and Sentry
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.23.0")
    implementation("io.sentry:sentry-logback:8.23.0")

    // OpenAPI (UI includes API dependency)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

    // Utility
    implementation("org.fusesource.jansi:jansi:2.4.2")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.bootJar {
    archiveFileName.set("demo.jar")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktfmt("0.58").googleStyle()
        ktlint("1.7.1").editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "ktlint_official",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
            ),
        )
    }

    kotlinGradle {
        ktlint("1.7.1")
    }
}

configurations.compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
}
