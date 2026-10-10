# CoreProtect: unofficial Paper 26.3 compatibility build

An unofficial community build of [PlayPro/CoreProtect v24.1](https://github.com/PlayPro/CoreProtect/tree/v24.1), adapted for **Paper 26.3 and Java 25**. This project is not an official PlayPro release and does not claim ownership of CoreProtect.

Version: `24.1-paper26.3.1`. Base: the public v24.1 source tag. License: [Artistic License 2.0](LICENSE).

## Changes

- Compile against Paper API `26.3.build.169-beta`.
- Extend the compatibility ceiling to Minecraft 26.3.
- Add an explicit 26.3 adapter inheriting v26.2 behavior.
- Identify the modified version and include its license and change notes in the JAR.

The database schema, SQLite/MySQL defaults, API 12, commands, permissions, configuration, logging and rollback behavior are preserved from v24.1. Upstream version checks and optional donation-key handling are retained. New entity features from upstream master, including cushion tracking, are not backported.

See [PORT-NOTES.md](PORT-NOTES.md) for build details and redistribution terms.

## Installation

1. Stop your Paper 26.3 server running Java 25.
2. Replace the existing CoreProtect JAR with `CoreProtect-24.1-paper26.3.1.jar`. Keep only one CoreProtect JAR in `plugins/`.
3. Retain your existing CoreProtect data and configuration. Back up production worlds and databases before upgrading.
4. Start the server and check the CoreProtect startup log. Use `/co i` to inspect recorded history.
5. To use an add-on such as CPVisualizer, enable `api-enabled: true` in CoreProtect configuration.

## Build

Use JDK 25 and Maven 3.9.11:

```sh
mvn package -DskipTests
```

Output: `target/CoreProtect-24.1-paper26.3.1.jar`. Paper API and optional plugin dependencies are not bundled. Upstream HikariCP and bStats relocations are retained.

## Validation

Both this plugin and CPVisualizer loaded on an actual Paper 26.3 build 169 server. Real SQLite history was written and read, and rollback/restore was checked for STONE, SULFUR_SPIKE, POTENT_SULFUR and CHEST. The disposable verification harness is in [verification/](verification/).

The harness modifies test blocks and shuts down the server; run it only on a disposable test server. Real game-client events, MySQL, WorldEdit and Folia were not verified.

## Attribution and support

CoreProtect is developed by PlayPro and its upstream contributors. All upstream source copyright notices and the original license are retained. This modified source may be made available and redistributed under Artistic License 2.0 section 4(c)(i).

Use this repository's issue tracker for problems specific to the compatibility build. For original documentation and official releases, visit [CoreProtect documentation](https://docs.coreprotect.net/) and [the upstream repository](https://github.com/PlayPro/CoreProtect).
