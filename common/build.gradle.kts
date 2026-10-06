plugins {
    id("com.iamkaf.multiloader.common")
}

dependencies {
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
