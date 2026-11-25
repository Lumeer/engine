plugins {
    kotlin("multiplatform") version "2.4.21"
    `maven-publish`
}

group = "io.lumeer"
version = (findProperty("version") as String?)?.takeIf { it != "unspecified" } ?: "1.0-SNAPSHOT"

kotlin {
    jvm {
        compilerOptions {
            // engine devel runs on Java 21
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    js {
        outputModuleName.set("lumeer-data-filters")
        nodejs()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
