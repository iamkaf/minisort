import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.iamkaf.multiloader.neoforge")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libsMc${project.name.replace(".", "")}")

fun integration(name: String): String = providers.gradleProperty("integration.${project.name}.$name").get()

// Optional integrations compile only and load when their mod does.
dependencies {
    compileOnly(catalog.findLibrary("jei-common-api").get())
    compileOnly("maven.modrinth:rei:${integration("rei-neoforge")}")
    compileOnly("maven.modrinth:cloth-config:${integration("cloth-config")}")
    compileOnly("maven.modrinth:controlify:${integration("controlify-neoforge")}")
}
