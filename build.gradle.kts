plugins {
    kotlin("jvm") version "2.0.21"
    id("fabric-loom") version "1.8-SNAPSHOT"
}

version = property("mod_version") as String
group = property("maven_group") as String
base { archivesName.set(property("archives_base_name") as String) }

repositories {
    mavenCentral()
    maven("https://maven.impactdev.net/repository/development/")

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("com.cobblemon:fabric:${property("cobblemon_version")}")
    modImplementation(files("libs/CobbleDollars-fabric-2.0.0+Beta-5.1+1.21.1.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

kotlin { jvmToolchain(21) }

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}

tasks.jar {
    val archiveName = project.base.archivesName.get()
    from("LICENSE") {
        rename { "${it}_$archiveName" }
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    from("LICENSE")
}

