import java.util.zip.ZipFile

plugins {
    application
    `maven-publish`
}

group = "dev.tjxjnoobie.customminecraftserver"
extra["versionTagPrefix"] = "CustomMinecraftServer"
extra["fallbackVersion"] = "0.1.0"
apply(from = "gradle/git-version.gradle.kts")
version = extra["gitVersion"] as String

val resourceGameVersion = providers.gradleProperty("resourceGameVersion")
    .orElse("0.1.1-working_migrate-to-gradle-SNAPSHOT")
val tavallToolsVersion = "1.0.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

application {
    mainClass = "dev.tjxjnoobie.customminecraftserver.bootstrap.ServerMain"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "TavallResourceGamePackages"
        url = uri("https://maven.pkg.github.com/TavallStudios/tavall-hytale-resource-game")
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
    val githubToken = providers.environmentVariable("GITHUB_TOKEN").orNull
    if (!githubToken.isNullOrBlank()) {
        listOf(
            "tavall-di",
            "tavall-cache",
            "tavall-concurrency",
            "tavall-database",
            "tavall-eventbus",
            "tavall-logging",
            "tavall-reflection",
            "tavall-registry",
            "tavall-scheduler",
        ).forEach { repository ->
            maven("https://maven.pkg.github.com/TavallStudios/$repository") {
                name = "github${repository.replace("-", "")}"
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").orElse("github").get()
                    password = githubToken
                }
            }
        }
    }
}

dependencies {
    implementation(platform("io.netty:netty-bom:4.1.121.Final"))
    implementation("io.netty:netty-buffer")
    implementation("io.netty:netty-codec")
    implementation("io.netty:netty-common")
    implementation("io.netty:netty-transport")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.4")
    implementation("com.tavall:game-api:${resourceGameVersion.get()}")

    implementation("org.tavall:tavall-di:$tavallToolsVersion")
    implementation("org.tavall:tavall-concurrency:$tavallToolsVersion")
    implementation("org.tavall:tavall-logging:$tavallToolsVersion")

    // Transitional until existing logger calls are migrated onto Tavall Logging.
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.jar {
    archiveFileName = "custom-minecraft-server.jar"
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

val verifyThinJar = tasks.register("verifyThinJar") {
    dependsOn(tasks.jar)
    val archive = tasks.jar.flatMap { it.archiveFile }
    inputs.file(archive)
    doLast {
        val forbidden = listOf("com/fasterxml/", "io/netty/", "org/slf4j/")
        ZipFile(archive.get().asFile).use { jar ->
            val embedded = jar.entries().asSequence().map { it.name }
                .firstOrNull { entry -> forbidden.any(entry::startsWith) }
            check(embedded == null) { "Third-party class embedded in thin JAR: $embedded" }
        }
    }
}

val stageDistribution = tasks.register<Sync>("stageDistribution") {
    dependsOn(tasks.jar)
    into(layout.projectDirectory.dir("distribution/server"))
    from(tasks.jar.flatMap { it.archiveFile }) {
        rename { "application.jar" }
    }
    from("server-settings.json")
    into("libs") {
        from(configurations.runtimeClasspath)
    }
}

tasks.check {
    dependsOn(verifyThinJar)
}

tasks.assemble {
    dependsOn(stageDistribution)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "customminecraftserver"
        }
    }
    repositories {
        val token = providers.environmentVariable("GITHUB_TOKEN")
        if (token.isPresent) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/TavallStudios/CustomMinecraftServer")
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").orNull
                    password = token.get()
                }
            }
        }
    }
}
