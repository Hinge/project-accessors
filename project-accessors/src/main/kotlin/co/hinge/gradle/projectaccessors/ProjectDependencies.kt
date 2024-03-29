package co.hinge.gradle.projectaccessors

import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionConstraint

/**
 * Returns a copy of this [ProjectDependency] with specified attributes.
 *
 * @receiver The [ProjectDependency] to copy.
 * @param targetConfiguration The requested target configuration of this dependency. This is the name of the configuration in the target module that should be used when selecting the matching configuration. If null, a default configuration will be used.
 * @param isTransitive Whether this dependency should be resolved including or excluding its transitive dependencies. The artifacts belonging to this dependency might themselves have dependencies on other artifacts. The latter are called transitive dependencies.
 * @param endorseStrictVersions Will, if true, endorse version constraints with [VersionConstraint.getStrictVersion] from the target module. Endorsing strict versions of another module/platform means that all strict versions will be interpreted during dependency resolution as if they were defined by the endorsing module.
 */
fun ProjectDependency.copy(
    targetConfiguration: String? = getTargetConfiguration(),
    isTransitive: Boolean = isTransitive(),
    endorseStrictVersions: Boolean = isEndorsingStrictVersions,
) : ProjectDependency = copy().apply {
    setTargetConfiguration(targetConfiguration)
    setTransitive(isTransitive)
    if (endorseStrictVersions) {
        endorseStrictVersions()
    } else {
        doNotEndorseStrictVersions()
    }
}