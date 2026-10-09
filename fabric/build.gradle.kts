import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.iamkaf.multiloader.fabric")
}

multiloaderFabric {
    commonDatagen.set(true)
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libsMc${project.name.replace(".", "")}")
val obfuscated = project.name.startsWith("1.")

fun integration(name: String): String = providers.gradleProperty("integration.${project.name}.$name").get()

// Optional integrations compile only; Fabric Loom 1.x remaps the Fabric builds, while 26.x runs unobfuscated.
dependencies {
    val compileOnlyLane = if (obfuscated) "modCompileOnly" else "compileOnly"
    add(compileOnlyLane, catalog.findLibrary("jei-common-api").get())
    add(compileOnlyLane, "maven.modrinth:rei:${integration("rei-fabric")}")
    add(compileOnlyLane, "maven.modrinth:controlify:${integration("controlify-fabric")}")
    // Only REI's Rectangle comes from this jar, and it has no Minecraft types to remap.
    compileOnly("maven.modrinth:cloth-config:${integration("cloth-config")}")
}
