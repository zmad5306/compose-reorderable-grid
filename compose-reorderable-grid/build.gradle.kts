import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp")
}

group = "dev.zachmaddox.compose"
version = "1.0.0"

android {
    namespace = "dev.zachmaddox.compose.reorderable.grid"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    // Creates sources + javadoc artifacts for the "release" component
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.00"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Only require creds/signing when publishing tasks are invoked
fun isCentralPublishRequested(): Boolean =
    gradle.startParameter.taskNames.any { it.contains("CentralPortal", ignoreCase = true) }

afterEvaluate {
    // ---- Publication (Android component exists only after evaluation)
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "dev.zachmaddox.compose"
                artifactId = "compose-reorderable-grid"
                version = project.version.toString()

                pom {
                    name.set("compose-reorderable-grid")
                    description.set("A Jetpack Compose LazyVerticalGrid with built-in long-press drag-to-reorder support.")
                    url.set("https://github.com/zmad5306/compose-reorderable-grid")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("zachmaddox")
                            name.set("Zach Maddox")
                            url.set("https://github.com/zmad5306")
                        }
                    }
                    scm {
                        url.set("https://github.com/zmad5306/compose-reorderable-grid")
                        connection.set("scm:git:https://github.com/zmad5306/compose-reorderable-grid.git")
                        developerConnection.set("scm:git:ssh://git@github.com:zmad5306/compose-reorderable-grid.git")
                    }
                }
            }
        }
    }

    // ---- nmcp (✅ publishingType, NOT publicationType)
    nmcp {
        publish("release") {
            val u = providers.environmentVariable("OSSRH_USERNAME")
            val p = providers.environmentVariable("OSSRH_PASSWORD")

            if (isCentralPublishRequested()) {
                if (!u.isPresent) error("Missing OSSRH_USERNAME (Central Portal token username)")
                if (!p.isPresent) error("Missing OSSRH_PASSWORD (Central Portal token password)")
            }

            username.set(u.orElse(""))
            password.set(p.orElse(""))

            // ✅ correct property name
            publishingType.set("AUTOMATIC")
        }
    }

    // ---- signing (only enforce for publish)
    signing {
        val key = providers.environmentVariable("SIGNING_KEY")
        val pass = providers.environmentVariable("SIGNING_PASSWORD")

        if (isCentralPublishRequested()) {
            if (!key.isPresent) error("Missing SIGNING_KEY (ASCII-armored private key)")
            if (!pass.isPresent) error("Missing SIGNING_PASSWORD")
        }

        if (key.isPresent && pass.isPresent) {
            useInMemoryPgpKeys(key.get(), pass.get())
            sign(publishing.publications["release"])
        }
    }
}
