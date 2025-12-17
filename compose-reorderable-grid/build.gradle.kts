plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp")
}

group = "dev.zachmaddox.compose"
version = "1.1.0"

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
        debug { isMinifyEnabled = false }
    }

    buildFeatures { compose = true }

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
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

afterEvaluate{
    publishing {
        publications {
            register<MavenPublication>("release") {
                afterEvaluate { from(components["release"]) }

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
    signing {
        sign(publishing.publications["release"])
    }
}
