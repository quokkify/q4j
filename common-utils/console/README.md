Manages SSH tunnels and executes remote shell commands over port-forwarded sessions in integration tests.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-ssh):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-ssh:0.2.2")
}
```

## Environment variables

| Variable               | Description                           |
| ---------------------- | ------------------------------------- |
| `SSH_HOST_IP`          | Remote host IP address                |
| `SSH_USER`             | SSH username                          |
| `SSH_PRIVATE_KEY_PATH` | Path to the private key file          |
| `SSH_PASSPHRASE`       | Private key passphrase (may be empty) |
| `SSH_HOST_PORT`        | Remote SSH port (default 22)          |
| `SSH_LOCAL_PORT`       | Local forwarded port                  |
| `SSH_REMOTE_PORT`      | Remote service port to forward to     |

## Initialization in BaseTest

Define an Owner config interface to read env vars in a type-safe way (requires `common-utils/config`):

```java
@Config.Sources({"system:env"})
interface SshConfig extends Config {
    @Key("SSH_HOST_IP")          String hostIp();
    @Key("SSH_USER")             String user();
    @Key("SSH_PRIVATE_KEY_PATH") String privateKeyPath();
    @Key("SSH_PASSPHRASE")       @DefaultValue("") String passphrase();
    @Key("SSH_HOST_PORT")        @DefaultValue("22") int hostPort();
    @Key("SSH_LOCAL_PORT")       int localPort();
    @Key("SSH_REMOTE_PORT")      int remotePort();
}
```

Then initialize in `@BeforeClass`:

```java
private static Session session;
private static Shell   shell;

@BeforeClass
public static void openTunnel() throws Exception {
    SshConfig cfg = ConfigRegistry.get(SshConfig.class);
    SshPortForwardConfig config = new SshPortForwardConfig(
        cfg.hostIp(), "127.0.0.1", cfg.user(),
        cfg.privateKeyPath(), cfg.passphrase(),
        cfg.hostPort(), cfg.localPort(), cfg.remotePort()
    );
    session = SshUtils.createSession(config);
    SshUtils.setPortForwarding(session, config);
    shell = new Shell.Plain(new SSH(cfg.hostIp(), cfg.hostPort(),
                                   cfg.user(), cfg.privateKeyPath()));
}

@AfterClass
public static void closeTunnel() {
    SshConfig cfg = ConfigRegistry.get(SshConfig.class);
    SshUtils.deletePortForwarding(session, cfg.localPort());
    SshUtils.closeSession(session);
}
```

> **Alternative** (without Owner): pass values directly via `System.getenv("SSH_HOST_IP")`, `Integer.parseInt(System.getenv("SSH_LOCAL_PORT"))`, etc.

````

## Usage in tests

```java
@Test
public void verifyServiceIsRunning() throws Exception {
    String status = SshUtils.executeCommand(shell, "systemctl status app-service");
    assertThat(status).contains("active (running)");
}

@Test
public void findErrorsInLog() throws Exception {
    String matches = SshUtils.getTextMatchesInFile(shell, "/var/log/app.log", "ERROR");
    assertThat(matches).isEmpty();
}

@Test
public void inspectRecentLogLines() throws Exception {
    String tail = SshUtils.getLastRowsFromFile(shell, "/var/log/app.log", 100);
    assertThat(tail).doesNotContain("FATAL");
}
````

## Key API

| Method                                             | Returns   | Notes                                 |
| -------------------------------------------------- | --------- | ------------------------------------- |
| `SshUtils.createSession(config)`                   | `Session` | Opens JSch session                    |
| `SshUtils.setPortForwarding(session, config)`      | `void`    | Binds local port to remote port       |
| `SshUtils.deletePortForwarding(session, port)`     | `void`    | Releases local port binding           |
| `SshUtils.closeSession(session)`                   | `void`    | Disconnects and frees resources       |
| `SshUtils.executeCommand(shell, command)`          | `String`  | Returns stdout of the command         |
| `SshUtils.getTextMatchesInFile(shell, path, text)` | `String`  | grep result for `text` in remote file |
| `SshUtils.getLastRowsFromFile(shell, path, n)`     | `String`  | tail -n result from remote file       |
| `SshUtils.executeRubyCommand(shell, command)`      | `String`  | Runs command via ruby interpreter     |
