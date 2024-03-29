# Publishing
To publish you need to do the following:
1. Check out `main` and update it:
   - `git checkout main && git pull origin main` 
2. Update the version in [`gradle.properties`](gradle.properties) to a non `-SNAPSHOT` version.
3. Update the version in [the readme](README.md) setup docs.
4. Run checks:
   - `./gradlew check publishPlugins --validate-only`
5. Commit with the message "Prepare for release X.Y.Z.":
   - `git commit -am "Prepare for release X.Y.Z."`
6. Push:
   - `git push origin main`
7. Create a new release: https://github.com/Hinge/project-accessors/releases/new
   - Create new tag version: `X.Y.Z`
   - Release title: `X.Y.Z`
   - Description: Automatically generate the notes and describe the highlights above.
8. Check the [publish workflow](https://github.com/Hinge/project-accessors/actions/workflows/publish.yml).
9. Update the version in `gradle.properties` to the next `-SNAPSHOT` version
   - You get the next version by incrementing the minor version and adding `-SNAPSHOT` to the end.
   - For example, `1.2.3` would become `1.3.0-SNAPSHOT`.
10. Commit with the message "Prepare for next development iteration":
    - `git commit -am "Prepare for next development iteration"`
11. Push:
    - `git push origin main`
