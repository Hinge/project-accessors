package co.hinge.gradle.projectaccessors

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider

class ProjectAccessorsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("projectAccessors", ProjectAccessorsExtension::class.java)
        extension.projects.all { ext ->
            target.setUp(ext)
        }
        target.afterEvaluate {
            val parent = target.gradle.parent
            if (extension.projects.isNotEmpty()) {
                validateNames(extension.projects)
            } else if (parent != null) {
                extension.projects.register("parent")
            }
        }
    }

    private fun Project.setUp(extension: ProjectAccessorsProjectExtension): TaskProvider<GenerateProjectAccessorsTask> {
        val name = extension.name
        val task = tasks.register(
            "generate${name.replaceFirstChar(Char::uppercaseChar)}ProjectAccessors",
            GenerateProjectAccessorsTask::class.java
        ) {
            it.description = "Generates the $name project accessors"

            it.projectPaths.set(extension.projectPaths)
            it.packageName.set(extension.packageName)
            it.accessorName.set(extension.accessorName)
            it.className.set("${extension.name.replaceFirstChar(kotlin.Char::uppercaseChar)}ProjectAccessors")
            it.outputDirectory.set(layout.buildDirectory.dir("generated/project-accessors/${name}"))
        }

        plugins.withId("org.jetbrains.kotlin.jvm") {
            extensions.configure<SourceSetContainer>("sourceSets") { sourceSets ->
                sourceSets.named("main") { sourceSet ->
                    sourceSet.java.srcDir(task.flatMap { t -> t.outputDirectory })
                }
            }
        }

        // The parent name is special as it refers to the project that includes this build
        val parent = gradle.parent
        if (name == "parent" && parent != null) {
            parent.rootProject {
                extension.fromProject(it)
                extension.accessorName.convention("projects")
            }
        }

        return task
    }

    private fun validateNames(projects: Iterable<ProjectAccessorsProjectExtension>) {
        val accessors = mutableSetOf<String>()
        projects.forEach {
            val accessorName = it.packageName.get() + "." + it.accessorName.get()
            if (!accessors.add(accessorName)) {
                throw IllegalArgumentException("There are multiple projects accessors with the accessor name $accessorName. When adding multiple accessors, make sure to set explicit and unique accessor names.")
            }
        }
    }
}