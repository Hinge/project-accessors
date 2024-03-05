import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.gradle.publish)
}

if (providers.gradleProperty("signArtifacts").orNull?.toBooleanStrict() == true) {
    pluginManager.apply("signing")
}

group = "co.hinge.gradle.project-accessors"
version = providers.gradleProperty("version").get()

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    maxHeapSize = "1g"
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AZUL)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

val buildDirMavenRepo = layout.buildDirectory.dir("repo")
publishing {
    repositories {
        val buildDirRepo = maven {
            name = "buildDir"
            url = uri(buildDirMavenRepo)
        }
        tasks.withType<AbstractPublishToMaven>().configureEach {
            doLast {
                if (this is PublishToMavenRepository && repository == buildDirRepo) {
                    return@doLast
                }
                println("Published ${publication.groupId}:${publication.artifactId}:${publication.version}")
            }
        }
    }
}


val functionalTest: SourceSet by sourceSets.creating

@Suppress("UnstableApiUsage")
gradlePlugin {
    isAutomatedPublishing = true
    testSourceSets(sourceSets.test.get(), functionalTest)
    plugins {
        register("projectAccessors") {
            id = "co.hinge.gradle.project-accessors"
            implementationClass = "co.hinge.gradle.projectaccessors.ProjectAccessorsPlugin"
            displayName = "Project Accessors Plugin"
            description = "A Gradle plugin that can generate type safe project accessors for included builds."
            tags = setOf("accessors", "projects")
        }
    }
    vcsUrl.set("https://github.com/Hinge/project-accessors")
    website.set("https://github.com/Hinge/project-accessors")

}

tasks.publishPlugins {
    doFirst {
        check(!version.toString().endsWith("-SNAPSHOT")) {
            "You cannot publish snapshot versions"
        }
    }
}

pluginManager.withPlugin("org.gradle.signing") {
    configure<SigningExtension> {
        val signingKeyId: String? by project
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    dependsOn("publishAllPublicationsToBuildDirRepository")
    mustRunAfter(tasks.test)
    systemProperty("mavenRepo", buildDirMavenRepo.get().asFile.absolutePath)
    systemProperty("pluginVersion", version.toString())
    systemProperty("currentGradleVersion", gradle.gradleVersion)
    systemProperty("kotlinVersion", libs.versions.kotlin.get())
}

tasks.check {
    dependsOn(functionalTestTask)
}

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    "functionalTestImplementation"(project)
    "functionalTestImplementation"(libs.kotlin.test)
    "functionalTestImplementation"(platform(libs.junit.bom))
    "functionalTestImplementation"(libs.junit.jupiter)
    "functionalTestImplementation"(libs.assertk)
    "functionalTestRuntimeOnly"(libs.junit.platform.launcher)
}