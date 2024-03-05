package co.hinge.gradle.projectaccessors

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@Suppress("LeakingThis")
abstract class ProjectAccessorsProjectExtension(private val name: String) : Named {

    /**
     * The project paths to generate the path for
     */
    abstract val projectPaths: SetProperty<String>

    /**
     * The package name to generate the accessors in.
     *
     * Defaults an empty string.
     */
    abstract val packageName: Property<String>

    /**
     * The name of the accessor property to generate.
     *
     * This can be changed if you need multiple accessors.
     *
     * Defaults to `projects` when using an included build.
     */
    abstract val accessorName: Property<String>

    init {
        with(projectPaths) {
            finalizeValueOnRead()
        }
        with(packageName) {
            finalizeValueOnRead()
            convention("")
        }
        with(accessorName) {
            finalizeValueOnRead()
        }
    }

    override fun getName(): String = name

    fun fromProject(project: Project) {
        projectPaths.convention(project.rootProject.allprojects.map { it.path })
    }
}