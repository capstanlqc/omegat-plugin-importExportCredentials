import java.io.FileInputStream

plugins {
    java
    distribution
    id("org.omegat.gradle") version "1.5.7"
}

fun isReadableScript(f: File): Boolean {
    return try {
        val bytes = ByteArray(16)
        FileInputStream(f).use { stream -> stream.read(bytes) }
        bytes[0] != 0x00.toByte()
    } catch (ex: Exception) {
        false
    }
}

val credentialKeysScript = file("config/credential-keys.gradle.kts")
if (credentialKeysScript.exists() && isReadableScript(credentialKeysScript)) {
    apply(from = credentialKeysScript)
} else {
    logger.warn("config/credential-keys.gradle.kts not available — building without hardcoded passwords.")
    
    // Generate a fallback stub so the public build compiles successfully
    val generateFallbackKeys by tasks.registering {
        val outputFile = file("build/generated/credentialKeys/be/capstan/omegat/plugin/CredentialKeys.java")
        outputs.file(outputFile)
        doLast {
            outputFile.parentFile.mkdirs()
            outputFile.writeText(
                """
                package be.capstan.omegat.plugin;
                
                import java.util.Collections;
                import java.util.List;
                
                /** AUTO-GENERATED FALLBACK — no hardcoded passwords. */
                public final class CredentialKeys {
                    private CredentialKeys() {}
                    public static List<String> getPasswords() {
                        return Collections.emptyList();
                    }
                }
                """.trimIndent()
            )
        }
    }
    
    // Tell the compiler where to find the generated stub
    sourceSets {
        main {
            java {
                srcDir("build/generated/credentialKeys")
            }
        }
    }
    
    tasks.named("compileJava") {
        dependsOn(generateFallbackKeys)
    }
}

// Check if obfuscation script is available and readable
val obfuscationScript = file("config/obfuscation.gradle.kts")
val useObfuscation = obfuscationScript.exists() && isReadableScript(obfuscationScript)

if (useObfuscation) {
    apply(from = obfuscationScript)
} else {
    logger.warn("config/obfuscation.gradle.kts not available — building without obfuscation.")
}

version = "3.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

omegat {
    version = "5.7.0"
    pluginClass = "be.capstan.omegat.plugin.ImportExportCredentials"
}

dependencies {
    implementation("org.omegat:omegat:5.7.0")
    implementation("org.omegat:lib-mnemonics:1.0")
}

distributions {
    main {
        contents {
            // Package the obfuscated JAR if we have the key, otherwise the standard JAR
            if (useObfuscation) {
                from(tasks.named("obfuscate"))
            } else {
                from(tasks["jar"])
            }
            from("README.md", "COPYING")
        }
    }
}
