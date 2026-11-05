// W-Tap e Blockhit do Pulsar, separado do client: um mod Forge 1.8.9 comum, é só colocar na pasta mods.
plugins {
    java
    id("gg.essential.loom") version "1.11.39"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

group = "dev.pulsarclient"
version = Regex("VERSION = \"([^\"]+)\"").find(file("src/main/java/dev/pulsarclient/wtap/PulsarWTap.java").readText())!!.groupValues[1]

base {
    archivesName.set("pulsar-wtap")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    runConfigs {
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
    }
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("mcmod.info") {
        expand("version" to project.version)
    }
}
