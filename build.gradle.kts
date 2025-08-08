plugins {
    java
    distribution
    id("org.omegat.gradle") version "1.5.7"
}

version = "1.0"

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
