// Mod do Pulsar Client para Minecraft 1.8.9 (Forge 11.15.1.2318).
// Usa o architectury-loom da Essential, que roda em Gradle moderno e compila para Java 8.
plugins {
    idea
    java
    id("gg.essential.loom") version "1.11.39"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

group = "dev.pulsarclient"
version = "0.6.0"

base {
    archivesName.set("pulsar-client")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    runConfigs {
        "client" {
            isIdeConfigGenerated = true
        }
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
    // ASM já vem com o Forge no jogo; aqui é só para compilar o transformer da tela de carregamento.
    compileOnly("org.ow2.asm:asm-debug-all:5.0.3")
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

// O jar também é um "coremod": carrega antes do Minecraft para trocar a tela de carregamento do Forge.
tasks.withType(Jar::class) {
    manifest.attributes(
        "FMLCorePlugin" to "dev.pulsarclient.core.PulsarCore",
        "FMLCorePluginContainsFMLMod" to "true",
        "ForceLoadAsMod" to "true",
    )
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("mcmod.info") {
        expand("version" to project.version)
    }
}
