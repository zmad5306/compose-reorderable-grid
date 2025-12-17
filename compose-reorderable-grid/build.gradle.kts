import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    id("maven-publish")
    id("signing")
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
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        // ✅ Register EARLY so nmcp can see it during configuration
        register<MavenPublication>("release") {
            groupId = "dev.zachmaddox.compose"
            artifactId = "compose-reorderable-grid"
            version = project.version.toString()

            artifact(javadocJar)

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

// ✅ Only the Android "from(components["release"])" needs afterEvaluate
afterEvaluate {
    publishing.publications.named("release", MavenPublication::class.java).configure {
        from(components["release"])
    }
}

signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["release"])
    }
}
