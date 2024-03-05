package co.hinge.gradle.projectaccessors

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

class ProjectAccessorsGeneratorTest {
    private val generator = ProjectAccessorsGenerator(
        packageName = "com.example",
        accessorName = "projects",
        className = "ProjectsAccessors"
    )

    @Test
    fun `generate empty accessor`() {
        test(
            setOf(":"),
            """
            package com.example

            import kotlin.String
            import org.gradle.api.Project
            import org.gradle.api.`internal`.artifacts.dependencies.ProjectDependencyInternal
            
            internal val Project.projects: ProjectsAccessors
              get() = ProjectsAccessors(this)
            
            internal class ProjectsAccessors(
              private val project: Project,
            ) {
              public val root: RootProject
                get() = RootProject()
            
              public inner class RootProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":")) as ProjectDependencyInternal {
                public val path: String = ":"
              }
            }
            """
        )
    }

    @Test
    fun `with projects`() {
        test(
            setOf(
                ":",
                ":module1",
                ":module2",
                ":module2:submodule1",
                ":module2:submodule2",
                ":module2:submodule2:subsubmodule1",
                ":module2:submodule3:subsubmodule1",
            ),
            """
            package com.example

            import kotlin.String
            import org.gradle.api.Project
            import org.gradle.api.`internal`.artifacts.dependencies.ProjectDependencyInternal
            
            internal val Project.projects: ProjectsAccessors
              get() = ProjectsAccessors(this)
            
            internal class ProjectsAccessors(
              private val project: Project,
            ) {
              public val root: RootProject
                get() = RootProject()
            
              public val module1: Module1Project
                get() = Module1Project()
            
              public val module2: Module2Project
                get() = Module2Project()
            
              public inner class RootProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":")) as ProjectDependencyInternal {
                public val path: String = ":"
              }
            
              public inner class Module1Project : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":module1")) as ProjectDependencyInternal {
                public val path: String = ":module1"
              }
            
              public inner class Module2Project : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":module2")) as ProjectDependencyInternal {
                public val path: String = ":module2"
            
                public val submodule1: Submodule1Project
                  get() = Submodule1Project()
            
                public val submodule2: Submodule2Project
                  get() = Submodule2Project()
            
                public val submodule3: Submodule3Project
                  get() = Submodule3Project()
            
                public inner class Submodule1Project : ProjectDependencyInternal by
                    project.dependencies.project(mapOf("path" to ":module2:submodule1")) as
                    ProjectDependencyInternal {
                  public val path: String = ":module2:submodule1"
                }
            
                public inner class Submodule2Project : ProjectDependencyInternal by
                    project.dependencies.project(mapOf("path" to ":module2:submodule2")) as
                    ProjectDependencyInternal {
                  public val path: String = ":module2:submodule2"
            
                  public val subsubmodule1: Subsubmodule1Project
                    get() = Subsubmodule1Project()
            
                  public inner class Subsubmodule1Project : ProjectDependencyInternal by
                      project.dependencies.project(mapOf("path" to ":module2:submodule2:subsubmodule1")) as
                      ProjectDependencyInternal {
                    public val path: String = ":module2:submodule2:subsubmodule1"
                  }
                }
            
                public inner class Submodule3Project {
                  public val path: String = ":module2:submodule3"

                  public val subsubmodule1: Subsubmodule1Project
                    get() = Subsubmodule1Project()
            
                  public inner class Subsubmodule1Project : ProjectDependencyInternal by
                      project.dependencies.project(mapOf("path" to ":module2:submodule3:subsubmodule1")) as
                      ProjectDependencyInternal {
                    public val path: String = ":module2:submodule3:subsubmodule1"
                  }
                }
              }
            }
            """
        )
    }

    @Test
    fun `with dashes`() {
        test(
            setOf(":", ":some-module"),
            """
            package com.example

            import kotlin.String
            import org.gradle.api.Project
            import org.gradle.api.`internal`.artifacts.dependencies.ProjectDependencyInternal
            
            internal val Project.projects: ProjectsAccessors
              get() = ProjectsAccessors(this)
            
            internal class ProjectsAccessors(
              private val project: Project,
            ) {
              public val root: RootProject
                get() = RootProject()
            
              public val someModule: SomeModuleProject
                get() = SomeModuleProject()
            
              public inner class RootProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":")) as ProjectDependencyInternal {
                public val path: String = ":"
              }
            
              public inner class SomeModuleProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":some-module")) as ProjectDependencyInternal {
                public val path: String = ":some-module"
              }
            }
            """
        )
    }

    @Test
    fun `with underscore`() {
        test(
            setOf(":some_module"),
            """
            package com.example

            import kotlin.String
            import org.gradle.api.Project
            import org.gradle.api.`internal`.artifacts.dependencies.ProjectDependencyInternal

            internal val Project.projects: ProjectsAccessors
              get() = ProjectsAccessors(this)

            internal class ProjectsAccessors(
              private val project: Project,
            ) {
              public val root: RootProject
                get() = RootProject()

              public val someModule: SomeModuleProject
                get() = SomeModuleProject()

              public inner class RootProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":")) as ProjectDependencyInternal {
                public val path: String = ":"
              }

              public inner class SomeModuleProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":some_module")) as ProjectDependencyInternal {
                public val path: String = ":some_module"
              }
            }
            """
        )
    }

    @Test
    fun `with more things in settings`() {
        test(
            setOf(":project-accessors"),
            """
            package com.example

            import kotlin.String
            import org.gradle.api.Project
            import org.gradle.api.`internal`.artifacts.dependencies.ProjectDependencyInternal
            
            internal val Project.projects: ProjectsAccessors
              get() = ProjectsAccessors(this)
            
            internal class ProjectsAccessors(
              private val project: Project,
            ) {
              public val root: RootProject
                get() = RootProject()
            
              public val projectAccessors: ProjectAccessorsProject
                get() = ProjectAccessorsProject()
            
              public inner class RootProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":")) as ProjectDependencyInternal {
                public val path: String = ":"
              }
            
              public inner class ProjectAccessorsProject : ProjectDependencyInternal by
                  project.dependencies.project(mapOf("path" to ":project-accessors")) as
                  ProjectDependencyInternal {
                public val path: String = ":project-accessors"
              }
            }
            """
        )
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun test(projectPaths: Set<String>, expected: String) {
        val output = generator.generate(projectPaths).toString().trim()
        assertThat(output).isEqualTo(expected.trimIndent())
        val result = KotlinCompilation().run {
            sources = listOf(SourceFile.kotlin("ProjectsAccessors.kt", expected))
            inheritClassPath = true
            compile()
        }
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        println(result.outputDirectory.resolve("com/example").list()?.asList())
        result.classLoader.loadClass("com.example.ProjectsAccessors")
        result.classLoader.loadClass("com.example.ProjectsAccessorsKt")
    }
}