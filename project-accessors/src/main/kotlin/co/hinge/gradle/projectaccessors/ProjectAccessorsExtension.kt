package co.hinge.gradle.projectaccessors

import org.gradle.api.NamedDomainObjectContainer

abstract class ProjectAccessorsExtension {
    abstract val projects: NamedDomainObjectContainer<ProjectAccessorsProjectExtension>

    /**
     * Adds a new project to generate accessors for.
     *
     * By default, if the project is a part of an included build and no projects are registered, a project named
     * `parent` will be registered which will point to the parent `settings.gradle.kts` and generate accessors available
     * using `projects`.
     */
    fun project(name: String, configure: ProjectAccessorsProjectExtension.() -> Unit) {
        projects.maybeCreate(name).apply(configure)
    }
}