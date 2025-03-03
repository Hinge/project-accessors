# Project Accessors
A Gradle plugin that generates type safe project accessors for included builds.

Gradle already has [native support](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:type-safe-project-accessors)
for type safe project accessors, but it doesn't support accessing parent projects in an included build since included
builds can be used for multiple parent projects.

But, most of the time included builds are only used with a single parent project, and thus it's very useful to be able
to access the projects in a type safe way.

## Setup
Firstly, you need to add the plugin to your included build's build script (for example `build-logic/build.gradle.kts`):
```kotlin
plugin {
    id("co.hinge.gradle.project-accessors") version "1.2.0"
}

// If you also want to use the utilities you can add this:
dependencies {
    id("co.hinge.gradle.project-accessors:project-accessors:1.2.0")
}
```

For most projects, that's all that's needed. You can then access the projects using the generated accessors:
```kotlin
dependencies {
    implementation(projects.someModule)
}
```
or if not using precompiled scripts:
```kotlin
class SomeConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies {
            implementation(project.projects.someModule)
        }
    }
}
```

## Configuration
By default, the plugin detects if it's added to an included build. When this happens an accessor called `projects` is 
generated as an extension on `Project`.

If you need to support multiple parent projects or for any other reason need to customize things, you use the 
`projectAccessors` DSL:
```kotlin
projectAccessors {
    // Parent is the name of the default parent project
    project("parent") {
        // If you need to change the location of the settings file
        fromProject(gradle.parent!!.rootProject)
        // If you want to change the accessor name
        accessorName.set("myProjects") 
        // If you want to change the accessor package name (defaults to the root package)
        packageName.set("com.example.accessors") 
    }
    
    // If you need to support multiple parent projects
    project("other") {
        projectPaths.addAll(":path:to:module")
        accessorName.set("otherProjects")
    }
}
```

## Utilities
### Copying Project Dependencies
Sometimes you need to depend on a specific configuration of a project. This is
annoying to do with Kotlin build scripts as you have to copy the dependency then
change the configuration. 

To make this easier, the plugin provides a utility function that allows you to quickly
copy a dependency and change the configuration:
```kotlin
import co.hinge.gradle.projectaccessors.copy

dependencies {
    implementation(projects.someModule.copy(targetConfiguration = "someConfiguration"))
}
```

## Supported Gradle Versions
The plugin is tested with all major Gradle versions from 8.2, but will likely work with other versions too.

## License
```plain
Copyright 2024 Match Group, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
