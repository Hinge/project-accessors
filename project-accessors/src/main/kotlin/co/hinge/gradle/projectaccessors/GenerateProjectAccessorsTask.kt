package co.hinge.gradle.projectaccessors

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@Suppress("LeakingThis")
@CacheableTask
abstract class GenerateProjectAccessorsTask : DefaultTask() {
    @get:Input
    abstract val projectPaths: SetProperty<String>

    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val accessorName: Property<String>

    @get:Input
    abstract val className: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "build"
        projectPaths.finalizeValueOnRead()
        packageName.finalizeValueOnRead()
        accessorName.finalizeValueOnRead()
        className.finalizeValueOnRead()
        outputDirectory.finalizeValueOnRead()
    }

    @TaskAction
    fun generateAccessors() {
        with(outputDirectory.get().asFile) {
            deleteRecursively()
            mkdirs()
        }
        val generator = ProjectAccessorsGenerator(
            projectName = projectName.get(),
            packageName = packageName.get(),
            accessorName = accessorName.get(),
            className = className.get(),
        )

        generator.generate(projectPaths.get())
            .writeTo(outputDirectory.get().asFile)
    }
}
