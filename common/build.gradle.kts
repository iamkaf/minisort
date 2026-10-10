import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.iamkaf.multiloader.common")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libsMc${project.name.replace(".", "")}")

fun integration(name: String): String = providers.gradleProperty("integration.${project.name}.$name").get()

dependencies {
    // Optional integrations compile here and load only when their mod does. Common compiles against the NeoForge
    // builds, which share its Mojang names; Fabric and NeoForge repeat the dependencies for the shared sources.
    compileOnly(catalog.findLibrary("jei-common-api").get())
    compileOnly("maven.modrinth:rei:${integration("rei-neoforge")}")
    compileOnly("maven.modrinth:cloth-config:${integration("cloth-config")}")
    compileOnly("maven.modrinth:controlify:${integration("controlify-neoforge")}")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
}

// The category order tests use plain registry IDs and need no game bootstrap.
sourceSets.test {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().compileClasspath
}

tasks.test {
    useJUnitPlatform()
}
