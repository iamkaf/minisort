import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.iamkaf.multiloader.forge")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libsMc${project.name.replace(".", "")}")

// Only JEI has Forge builds (1.21.1). REI and Controlify publish none, so the shared sources for them stay out.
dependencies {
    compileOnly(catalog.findLibrary("jei-common-api").get())
}

tasks.withType<JavaCompile>().configureEach {
    exclude("**/compat/rei/**", "**/compat/controlify/**")
}

tasks.withType<Javadoc>().configureEach {
    exclude("**/compat/rei/**", "**/compat/controlify/**")
}
