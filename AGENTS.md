# AGENTS.md

## Cursor Cloud specific instructions

### Project overview
TelePads is a Minecraft Bukkit server plugin (Java/Maven). It compiles to a JAR that runs inside a Bukkit-compatible Minecraft server — there is no standalone execution mode.

### Build
- **JDK 8** is required (`JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64`). JDK 21 (the system default) is too new for source/target 1.5 and old Bukkit APIs.
- Build with: `JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn clean package`
- Output JAR: `target/TelePads-1.6.jar`

### Repository URL caveat
The original `pom.xml` referenced `http://repo.bukkit.org/content/groups/public/` which has been defunct since 2014. It has been updated to `https://maven.elmakers.com/repository/` which mirrors the old Bukkit artifacts over HTTPS. Maven 3.8+ blocks plain HTTP repositories by default.

### Testing
No automated tests exist in this repository (`src/test/` is absent). The only verification is that the project compiles and packages successfully via `mvn clean package`.

### Lint
No dedicated linter is configured. Compilation warnings from `mvn compile` serve as the lint check.

### Running
This is a server plugin, not a standalone app. To test end-to-end you would need a CraftBukkit/Spigot 1.7.10 server with the JAR placed in its `plugins/` directory, plus a Minecraft client. This is not feasible in cloud agent environments.
