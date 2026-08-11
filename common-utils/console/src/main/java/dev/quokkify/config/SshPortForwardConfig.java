package dev.quokkify.config;

/**
 * Configuration for SSH port forwarding.
 *
 * <p>This record holds immutable configuration details for setting up
 * an SSH connection with local port forwarding:</p>
 * <ul>
 *   <li>Remote host IP and port to connect via SSH</li>
 *   <li>Local host IP and ports used for forwarding</li>
 *   <li>SSH authentication details (username, private key, passphrase)</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>
 *   SshPortForwardConfig cfg = new SshPortForwardConfig(
 *       "192.168.1.10",
 *       "127.0.0.1",
 *       "admin",
 *       "/home/admin/.ssh/id_rsa",
 *       "secret",
 *       22,
 *       8080,
 *       3306
 *   );
 * </pre>
 */
public record SshPortForwardConfig(
    String hostIp,
    String localHostIp,
    String userName,
    String privateKeyPath,
    String privateKeyPassphrase,
    int hostPort,
    int localPort,
    int remotePort
) {

}
