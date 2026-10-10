# Unofficial CoreProtect 24.1 port for Paper 26.3

This is a modified community build, not an official PlayPro release.
Base source: https://github.com/PlayPro/CoreProtect/tree/v24.1
Version: `24.1-paper26.3.1`. Branch: `community-paper26.3`.

Changes from the v24.1 tag:

- Compile against the pinned Paper API `26.3.build.169-beta` with Java 25.
- Extend the declared compatibility ceiling to Minecraft 26.3; retain the
  upstream version checks and donation-key handling.
- Add an explicit 26.3 adapter inheriting the existing 26.2 block and container
  behavior, including sulfur spike attachment tracking.
- Identify the modified version in plugin metadata, bundle these notes and the
  original Artistic License 2.0 in the JAR.
- Include a disposable-server verification plugin under `verification/`.

Database schema, SQLite/MySQL defaults, API version 12, commands, permissions,
configuration, logging, and rollback code are retained from v24.1. This is a port
of that release's existing functionality. New entity tracking features from the
upstream development branch, including cushion placement/removal history, are
not backported. CPVisualizer visualizes block/container history, not entities.

The corresponding modified source is supplied with this build. Recipients may
copy, modify and redistribute it under the original Artistic License 2.0,
including making the Source form available to others (section 4(c)(i)). All
upstream copyright notices and attribution remain intact.

Build with Maven 3.9.11 and Java 25: `mvn package -DskipTests`.
The shaded artifact is `target/CoreProtect-24.1-paper26.3.1.jar`.
Paper API and optional WorldEdit/AdvancedChests dependencies are not bundled. HikariCP and
bStats are bundled using the upstream relocations.

The verification plugin changes blocks and shuts the server down. Run it only
on a disposable local server, never on a production server. It is not part of
the CoreProtect JAR.
