package co.hinge.gradle.projectaccessors

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class ProjectAccessorsGeneratorFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private val includedBuildPlugin: File get() = projectDir.resolve("gradle-plugin/src/main/kotlin/com/example/ExamplePlugin.kt")

    private fun gradleRunner(version: String) = GradleRunner.create()
        .withGradleVersion(version)
        .withProjectDir(projectDir)

    @BeforeEach
    fun setup() {
        projectDir.resolve("settings.gradle.kts").writeKotlin(
            """
            pluginManagement {
                includeBuild("gradle-plugin")
            }

            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            
            include(":dependency")
            include(":dependant")
            """
        )

        projectDir.resolve("build.gradle.kts").writeKotlin(
            """
            plugins {
                kotlin("jvm") version "${System.getProperty("kotlinVersion")}" apply false
            }
            """.trimIndent()
        )

        projectDir.resolve("gradle-plugin/settings.gradle.kts").writeKotlin(
            """
            pluginManagement {
                repositories {
                    maven("${System.getProperty("mavenRepo")}")
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent()
        )

        projectDir.resolve("gradle-plugin/build.gradle.kts").writeKotlin(
            """
            plugins {
                id("co.hinge.gradle.project-accessors") version "${System.getProperty("pluginVersion")}"
                kotlin("jvm") version "${System.getProperty("kotlinVersion")}"
                `java-gradle-plugin`
            }

            gradlePlugin {
                plugins {
                    register("com.example") {
                        id = name
                        implementationClass = "com.example.ExamplePlugin"
                    }
                }
            }

            kotlin {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
                }
            }

            tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
                sourceCompatibility = "1.8"
                targetCompatibility = "1.8"
            }

            projectAccessors {
                // Should just compile
                projects.configureEach {}
            }
            """
        )

        includedBuildPlugin.writeKotlin(
            """
            package com.example

            import org.gradle.api.Plugin
            import org.gradle.api.Project
            import projects

            class ExamplePlugin : Plugin<Project> {
                override fun apply(target: Project) {
                    target.pluginManager.apply("org.jetbrains.kotlin.jvm")
                    target.dependencies.add("implementation", target.projects.dependency)
                }
            }
        """
        )

        projectDir.resolve("dependency/build.gradle.kts").writeKotlin(
            """
            plugins {
                kotlin("jvm")
            }
            """
        )
        projectDir.resolve("dependency/src/main/kotlin/Dependency.kt").writeKotlin(
            """
            package com.example.dependency

            abstract class Dependency
        """.trimIndent()
        )

        projectDir.resolve("dependant/build.gradle.kts").writeKotlin(
            """
            plugins {
                id("com.example")
                kotlin("jvm")
            }
            """
        )
        projectDir.resolve("dependant/src/main/kotlin/Dependant.kt").writeKotlin(
            """
            package com.example.dependant

            import com.example.dependency.Dependency

            object Dependant : Dependency()
        """.trimIndent()
        )
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest
    @MethodSource("provideGradleVersions")
    fun setsUpDependencyCorrectly(gradleVersion: String) {
        val result = gradleRunner(gradleVersion)
            .withArguments(":dependant:assemble", "--stacktrace")
            .build()

        assertThat(result.task(":dependency:compileKotlin"))
            .isNotNull()
            .prop(BuildTask::getOutcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        assertThat(result.task(":dependency:compileKotlin"))
            .isNotNull()
            .prop(BuildTask::getOutcome)
            .isEqualTo(TaskOutcome.SUCCESS)
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest
    @MethodSource("provideGradleVersions")
    fun worksWithProjectIsolation(gradleVersion: String) {
        gradleRunner(gradleVersion)
            .withArguments(
                "-Dorg.gradle.internal.invalidate-coupled-projects=false",
                "-Dorg.gradle.unsafe.isolated-projects=true",
                "--stacktrace"
            )
            .build()
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest
    @MethodSource("provideGradleVersions")
    fun doesNotAllowDuplicateAccessorNames(gradleVersion: String) {
        projectDir.resolve("gradle-plugin/build.gradle.kts").appendText(
            """
            projectAccessors {
                project("projects1") {
                    settingsFile.fileValue(file("../settings.gradle.kts"))
                    accessorName.set("projects")
                }
                project("projects2") {
                    settingsFile.fileValue(file("../settings.gradle.kts"))
                    accessorName.set("projects")
                }
            }
        """.trimIndent()
        )

        assertFailure {
            gradleRunner(gradleVersion)
                .withArguments("--stacktrace")
                .build()
        }.isInstanceOf<UnexpectedBuildFailure>()
    }

    private fun File.writeKotlin(@Language("kts") content: String) {
        parentFile.mkdirs()
        writeText(content.trimIndent())
    }

    companion object {
        @JvmStatic
        private fun provideGradleVersions(): Stream<Arguments> {
            return setOf(
                System.getProperty("currentGradleVersion"),
                "8.13",
                "8.12.1",
                "8.11.1",
                "8.10.2",
                "8.9",
                "8.8",
                "8.7",
                "8.6",
                "8.5",
                "8.4",
                "8.3",
                "8.2.1",
                "8.1.1",
                "8.0.2",
                "7.6.4",
            ).map { Arguments.of(it) }.stream()
        }
    }
}
