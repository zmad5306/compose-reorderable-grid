import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp") version "0.0.9"
}

group = "dev.zachmaddox.compose"
version = "1.0.1"

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
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
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
    
    nmcp {
        publish("release") {
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
            publicationType.set("AUTOMATIC")
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
}
