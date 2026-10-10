# Disposable Paper 26.3 integration check

`PortSmoke.java` is a test plugin, not a production dependency. It creates blocks,
changes a chest inventory, records history through CoreProtect API 12, queries
through CPVisualizer, performs rollback/restore and shuts the server down.
Use a fresh local world. Do not install this plugin on a production server.
Accept Minecraft's EULA yourself before starting the disposable server.

Compile with Java 25 using the Paper 26.3 API and its dependencies, the shaded
CoreProtect port JAR and CPVisualizer 1.1.1 on the classpath. Maven can produce
the API dependency classpath:

```powershell
mvn dependency:build-classpath -Dmdep.outputFile=target/verification-classpath.txt
$deps = (Get-Content target/verification-classpath.txt -Raw).Trim()
$cpv = '../CPVisualizer/build/libs/CPVisualizer.jar'
$classpath = "target/CoreProtect-24.1-paper26.3.1.jar;$cpv;$deps"
New-Item -ItemType Directory target/verification-classes -Force
javac -encoding UTF-8 -cp $classpath -d target/verification-classes verification/PortSmoke.java
Copy-Item verification/plugin.yml target/verification-classes/plugin.yml
jar cf target/PortSmoke.jar -C target/verification-classes .
```

Place the two production JARs plus `PortSmoke.jar` in the disposable server's
`plugins/` directory. Start Paper 26.3 build 169 with Java 25, using a localhost
binding. The server must provide at least two worlds, as normal Paper does with
the Nether enabled. Success ends with `PORT_SMOKE_PASS` in `logs/latest.log`;
failure ends with `PORT_SMOKE_FAIL` and an exception. The test waits up to roughly
30 seconds for the CoreProtect consumer to commit its queued records.

This exercises real SQLite/API/server behavior, not client rendering, player
interaction events, MySQL, WorldEdit or Folia. Test plugin source is supplied for
inspection and reproduction; its JAR is intentionally absent from the release.
