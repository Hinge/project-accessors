package co.hinge.gradle.projectaccessors

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.util.GradleVersion
import java.util.Locale

internal class ProjectAccessorsGenerator(
    private val projectName: String,
    private val packageName: String,
    private val accessorName: String,
    private val className: String,
    private val gradleVersion: GradleVersion = GradleVersion.current(),
) {

    fun generate(projectPaths: Set<String>): FileSpec {
        val projectDependenciesType = ClassName(packageName, className)
        val projectDependencies = TypeSpec.classBuilder(projectDependenciesType)
            .addModifiers(KModifier.INTERNAL)
            .primaryConstructor(projectDependencyConstructor)
            .addProperty(
                PropertySpec.builder("project", projectType, KModifier.PRIVATE)
                    .initializer("project")
                    .build()
            )

        val projects = mutableMapOf(
            ":" to Module(
                listOf(""),
                projectDependenciesType,
                name = "root",
                isProject = true
            )
        )
        projectPaths
            .asSequence()
            .map { it.removePrefix(":") }
            .filter { it.isNotEmpty() }
            .map { it.split(":") }
            .forEach { path ->
                projects
                    .getOrPut(path.first()) {
                        Module(
                            path.first(),
                            projectDependenciesType
                        )
                    }
                    .add(path)
            }
        for (project in projects.values) {
            project.renderTo(projectDependencies)
        }

        return FileSpec.builder(projectDependenciesType)
            .addProperty(
                PropertySpec.builder(accessorName, projectDependenciesType, KModifier.INTERNAL)
                    .addKdoc("Returns the project dependencies for the %L project.", projectName)
                    .receiver(projectType)
                    .getter(
                        FunSpec.getterBuilder()
                            .addCode("return %T(this)", projectDependenciesType)
                            .build()
                    )
                    .build()
            )
            .addType(projectDependencies.build())
            .build()
    }

    /**
     */
    private fun Module.renderTo(parent: TypeSpec.Builder) {
        parent.addProperty(
            PropertySpec.builder(accessorName, typeName)
                .addKdoc("Creates a project dependency on the project at path \"%L\"", path.joinToString(":", prefix = ":"))
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return %T()", typeName)
                        .build()
                )
                .build()
        )

        parent.addType(
            TypeSpec.classBuilder(typeName)
                .addModifiers(KModifier.INNER)
                .apply {
                    if (isProject) {
                        addSuperinterface(projectDependencyType, projectDependency())
                    }
                    val path = path.joinToString(":", prefix = ":")
                    if (!isProject || gradleVersion < maxGradleVersionForProjectPath) {
                        addProperty(
                            PropertySpec.builder("path", STRING)
                                .addKdoc("Returns the path to the project as a string.")
                                .apply {
                                    if (!isProject) {
                                        addKdoc("\n\nPlease note that %S is not declared project so this path cannot be used as a dependency, this accessor is here for convenience.", path)
                                    }
                                }
                                .initializer("%S", path)
                                .build()
                        )
                    }
                    for (child in children.values) {
                        child.renderTo(this)
                    }
                }
                .build()
        )
    }

    private fun Module.projectDependency(): CodeBlock =
        CodeBlock.of(
            "project.dependencies.project(mapOf(%S to %S)) as %T",
            "path",
            path.joinToString(":", prefix = ":"),
            projectDependencyType
        )

    companion object {
        private val TO_CAMEL_CASE = Regex("[-_]([a-z])")
        private val projectType = ClassName("org.gradle.api", "Project")
        private val projectDependencyType =
            ClassName("org.gradle.api.internal.artifacts.dependencies", "ProjectDependencyInternal")
        private val projectDependencyConstructor = FunSpec.constructorBuilder()
            .addParameter("project", projectType)
            .build()

        private val maxGradleVersionForProjectPath = GradleVersion.version("8.11")
    }

    data class Module(
        val path: List<String>,
        val parent: ClassName,
        val name: String = path.last(),
        var isProject: Boolean = false,
    ) {
        val children = mutableMapOf<String, Module>()
        val accessorName: String = name.replace(TO_CAMEL_CASE) { it.groupValues[1].uppercase(Locale.ROOT) }
        val typeName: ClassName = parent.nestedClass(accessorName.replaceFirstChar(Char::uppercaseChar) + "Project")

        constructor(name: String, parent: ClassName) : this(listOf(name), parent, name)

        fun add(modulePath: List<String>) {
            if (modulePath == path) {
                isProject = true
                return
            }

            require(modulePath.subList(0, path.size) == path) {
                "Module $modulePath does not start with $path"
            }
            val next = modulePath[path.size]
            children.getOrPut(next) { Module(path + next, typeName) }
                .add(modulePath)
        }
    }
}