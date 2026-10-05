package dev.quokkify.util;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import dev.quokkify.config.SshPortForwardConfig;
import dev.quokkify.model.ConstantFormat;
import dev.quokkify.parser.RegexParser;

import com.jcabi.ssh.Shell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utils for ssh commands.
 */
public final class SshUtils {

  private static final Logger LOG = LogManager.getLogger(SshUtils.class);

  private static final String LOAD_PROFILE_COMMAND = "source /etc/profile";
  private static final String HOST_KEY_CHECKING_RULE = "StrictHostKeyChecking";
  private static final CommandLockManager COMMAND_LOCKS = new CommandLockManager();

  private SshUtils() {
  }

  /**
   * Get text matches in file.
   *
   * @param shell      command line interface
   * @param pathToFile path to file
   * @param textToFind text to find in file
   * @return result of command
   */
  public static String getTextMatchesInFile(Shell shell, String pathToFile, String textToFind) {
    final String command = "%s '%s' %s".formatted(
        ShellCommand.GREP.lowerCase(), textToFind, pathToFile);
    return executeCommand(shell, command);
  }

  /**
   * Get last N rows from file.
   *
   * @param shell               command line interface
   * @param pathToFile          path to file
   * @param lastRowsCountToFind last rows count to find in file
   * @return result of command
   */
  public static String getLastRowsFromFile(Shell shell, String pathToFile, int lastRowsCountToFind) {
    final String command = "%s -n %d %s".formatted(
        ShellCommand.TAIL.lowerCase(), lastRowsCountToFind, pathToFile);
    return executeCommand(shell, command);
  }

  /**
   * Get file content.
   *
   * @param shell      command line interface
   * @param pathToFile path to file
   * @return result of command
   */
  public static String getFileContent(Shell shell, String pathToFile) {
    final String command = "%s %s".formatted(ShellCommand.CAT.lowerCase(), pathToFile);
    return executeCommand(shell, command);
  }

  /**
   * Execute ruby command.
   *
   * @param shell       command line interface
   * @param rubyCommand ruby command to execute
   * @return result of executed command
   */
  public static String executeRubyCommand(Shell shell, String rubyCommand) {
    String command = "%s; %s".formatted(LOAD_PROFILE_COMMAND, rubyCommand);
    return executeCommand(shell, command);
  }

  /**
   * Execute a potentially unsafe Ruby command using the given command line interface and capture the result.
   *
   * @param shell       command line interface
   * @param rubyCommand ruby command to execute
   * @return result of executed command
   */
  public static String executeNotSafeRubyCommand(Shell shell, String rubyCommand) {
    String command = "%s; %s".formatted(LOAD_PROFILE_COMMAND, rubyCommand);
    try {
      return executeNotSafeCommand(shell, command);
    } catch (IOException e) {
      throw new RuntimeException("Failed to execute unsafe Ruby command", e);
    }
  }

  /**
   * Execute shell command.
   *
   * @param shell   command line interface
   * @param command command to execute
   * @return result of executed command
   */
  public static String executeCommand(Shell shell, String command) {
    try {
      String string = executeNotSafeCommand(new Shell.Safe(shell), command);
      return clearCertificateWillExpireWarning(string);
    } catch (IOException e) {
      throw new RuntimeException("Failed to execute command safely", e);
    }
  }

  /**
   * Execute shell command, waiting while an identical command runs on the same target.
   *
   * <p>Calls with the same {@code target} and the same command (ignoring surrounding whitespace and
   * line-ending style) run one at a time; any other combination runs in parallel.</p>
   *
   * @param shell        command line interface
   * @param target       name of the remote target, such as a host or environment
   * @param command      command to execute
   * @param queueTimeout maximum time to wait for an identical running command to finish
   * @return result of executed command
   * @throws CommandLockTimeoutException if the identical command is still running after {@code queueTimeout}
   * @throws IllegalStateException       if the thread is interrupted while waiting; the interrupt flag is restored
   */
  public static String executeCommand(Shell shell, String target, String command, Duration queueTimeout) {
    Objects.requireNonNull(shell, "shell");
    CommandLockManager.CommandLockKey key = CommandLockManager.CommandLockKey.of(target, command);
    try (CommandLockManager.LockHandle ignored = acquireCommandLock(key, queueTimeout)) {
      return executeCommand(shell, command);
    }
  }

  private static CommandLockManager.LockHandle acquireCommandLock(CommandLockManager.CommandLockKey key,
      Duration queueTimeout) {
    try {
      return COMMAND_LOCKS.acquire(key, queueTimeout);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to execute command on target '%s': %s"
          .formatted(key.target(), key.normalizedCommand()), e);
    }
  }

  /**
   * Execute not safe shell command.
   *
   * @param shell   command line interface
   * @param command command to execute
   * @return result of executed command
   */
  private static String executeNotSafeCommand(Shell shell, String command) throws IOException {
    return new Shell.Plain(shell).exec(command);
  }

  /**
   * Create SSH session.
   *
   * @param config ssh port forward configuration
   * @return ssh bridge session
   * @throws JSchException on SSH errors
   */
  public static Session createSession(SshPortForwardConfig config) throws JSchException {
    JSch jsch = new JSch();
    // record-аксессоры:
    jsch.addIdentity(config.privateKeyPath(), config.privateKeyPassphrase());
    Session session = jsch.getSession(config.userName(), config.hostIp(), config.hostPort());
    session.setConfig(HOST_KEY_CHECKING_RULE, BooleanUtils.toStringYesNo(false));
    session.connect();
    return session;
  }

  /**
   * Close SSH session.
   *
   * @param session ssh bridge session
   */
  public static void closeSession(Session session) {
    if (session != null && session.isConnected()) {
      session.disconnect();
    }
  }

  /**
   * Set SSH port forward.
   *
   * @param session ssh bridge session
   * @param config  ssh port forward configuration
   * @throws JSchException on SSH errors
   */
  public static void setPortForwarding(Session session, SshPortForwardConfig config) throws JSchException {
    session.setPortForwardingL(
        config.localPort(),
        config.localHostIp(),
        config.remotePort()
    );
  }

  /**
   * Delete SSH port forward.
   *
   * @param session ssh bridge session
   * @param port    ssh forwarding local port
   * @throws JSchException on SSH errors
   */
  public static void deletePortForwarding(Session session, int port) throws JSchException {
    session.delPortForwardingL(port);
  }

  /**
   * The certificate expires from time to time.
   * Certificate expiration warning is not part of JSON.
   */
  private static String clearCertificateWillExpireWarning(String exportFileContentJson) {
    final String certificateWarningPattern = "== WARN: .* certificate will expire.*";
    if (RegexParser.isMatched(certificateWarningPattern, exportFileContentJson)) {
      LOG.warn(RegexParser.parse(certificateWarningPattern, exportFileContentJson, 0));
    }
    return exportFileContentJson.replaceAll(certificateWarningPattern, StringUtils.EMPTY);
  }

  public enum ShellCommand implements ConstantFormat {
    TAIL, GREP, CAT;

    @Override
    public String formatValue() {
      return name();
    }
  }
}
