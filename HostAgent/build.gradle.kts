/*
 * CS Launcher Lite — host agent module
 *
 * A `-javaagent` for Minecraft: Java Edition that lifts the integrated server's hard-coded 8-player
 * LAN limit to the value of `-Dcs.host.maxPlayers`. Pure ASM, mapping-free: it recognises the class
 * by shape, so it works on obfuscated vanilla, Fabric intermediary and Forge/NeoForge official names
 * alike. See CsHostAgent.java for the shape it looks for.
 */
plugins {
    `java-library`
    // Shadow 9.x: this repo builds on Gradle 9.5 (see gradle/wrapper/gradle-wrapper.properties), and
    // the 8.x line predates Gradle 9's configuration-cache and task APIs — it fails at apply time.
    id("com.gradleup.shadow") version "9.6.1"
}

// The game ships its own ASM, and Fabric Loader refuses to start when it finds a second
// org.objectweb.asm.ClassReader ("duplicate ASM classes found on classpath"). A -javaagent jar is
// appended to the system class path, so ASM is RELOCATED into this agent's own package: the built jar
// must contain no org/objectweb/asm classes at all. The doLast guard below fails the build if one
// ever leaks back in.
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.ow2.asm:asm:9.11.1")
    implementation("org.ow2.asm:asm-tree:9.11.1")
}

// The comments in CsHostAgent.java are UTF-8 (em dashes, section marks). javac falls back to the
// platform encoding, which is US-ASCII on plenty of CI images and would fail the build on a comment.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveBaseName.set("cs-host-agent")
    archiveClassifier.set("")
    archiveVersion.set("")
    relocate("org.objectweb.asm", "com.movtery.zalithlauncher.hostagent.shaded.asm")
    exclude(
        "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
        "META-INF/maven/**", "module-info.class", "META-INF/versions/**"
    )
    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Premain-Class" to "com.movtery.zalithlauncher.hostagent.CsHostAgent",
            "Can-Retransform-Classes" to "false"
        )
    }
    doLast {
        val leaked = zipTree(archiveFile.get().asFile).matching { include("org/objectweb/asm/**") }.files
        if (leaked.isNotEmpty()) {
            throw GradleException("cs-host-agent leaks unrelocated ASM classes: $leaked")
        }
    }
}

// The plain jar is the shaded one: what the app copies into its assets must be the relocated build.
tasks.jar {
    enabled = false
    dependsOn(tasks.shadowJar)
}
