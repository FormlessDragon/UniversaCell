# MDG branch of CleanroomModTemplate

A modified version of [CleanroomMC/CleanroomModTemplate](https://github.com/CleanroomMC/CleanroomModTemplate) with some
QoL improvements.

# Differences from kappa Unimined branch

The fork keeps the upstream `net.neoforged.moddev.*` plugin ids but is published to Maven Central as
`io.github.ruixuqi:moddev-gradle` rather than to the Gradle Plugin Portal, so `settings.gradle` points those ids at it.
May have issues; report them [here](https://github.com/RuiXuqi/ModDevGradle).

Use `mixinProvider` for jars that are meant to supply the Mixin runtime (should be unnecessary on Cleanroom), and
plain `implementation` for libraries that must not be remapped.

You **MUST** write AT entries in SRG name (eg: `field_130070_K # fileResourcepacks`), unlike Unimined. No remapping.

For IntelliJ users, it is recommended to switch
to [RuiXuqi/MinecraftDev](https://github.com/RuiXuqi/MinecraftDev/releases) for AT completion. Also RFG and Unimined
support have been merged there.

## Template conventions

The build generates `Tags` under `build/generated/sources/tags` from the Git tag and
short commit hash. Use `Tags.MOD_ID`, `Tags.MOD_NAME`, and `Tags.VERSION` in the mod
entry point; no checked-in `Reference` class is needed.

Resource metadata is kept in `src/main/resources` and expands the `gradle.properties`
values during `processResources`. Access transformer entries use SRG names for the
ModDevGradle Cleanroom toolchain.

## GitHub Actions

The repository workflows are thin callers for the matching `*-mdg.yml` workflows in
`FormlessDragon/.github`. The shared MDG workflows keep the build, release, and
runtime-test sequence used by the template while using the ModDevGradle staging
tasks. The release workflow builds the JitPack repository and publishes to CurseForge
when `CURSEFORGE_TOKEN` and `curseforge_project_id` are configured.
