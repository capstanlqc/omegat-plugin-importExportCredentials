plugins {
    java
    distribution
    id("org.omegat.gradle") version "1.5.7"
}

val credentialKeysScript = file("config/credential-keys.gradle.kts")
if (credentialKeysScript.exists()) {
    apply(from = credentialKeysScript)
} else {
    logger.warn("config/credential-keys.gradle.kts not found — skipping credential key generation.")
}

version = "3.0"

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
            from(tasks["jar"], "README.md", "COPYING")
        }
    }
}
