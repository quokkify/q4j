package dev.quokkify.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Mutable;
import org.aeonbits.owner.Reloadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized access point for Owner configuration interfaces.
 * <p>
 * This class provides type-safe factory methods for different kinds of
 * configuration interfaces:
 * <ul>
 *   <li>{@link #get(Class)} for standard read-only {@link Config} interfaces</li>
 *   <li>{@link #getMutable(Class)} for {@link Config} interfaces that also implement {@link Mutable}</li>
 *   <li>{@link #getReloadable(Class)} for {@link Config} interfaces that implement {@link Mutable} and {@link Reloadable}</li>
 * </ul>
 * <p>
 * All configuration instances are cached by {@link ConfigCache}, so each
 * interface is effectively treated as a singleton.
 */
public class ConfigRegistry {

  private static final Logger log = LoggerFactory.getLogger(ConfigRegistry.class);

  private ConfigRegistry() {
  }

  /**
   * Returns a cached instance of the given configuration interface.
   * <p>
   * Use this for simple read-only configurations.
   *
   * @param clazz the configuration interface type
   * @param <T>   a type extending {@link Config}
   * @return a cached configuration instance
   */
  public static <T extends Config> T get(Class<T> clazz) {
    log.debug("Fetching Config instance for {}", clazz.getSimpleName());
    return ConfigCache.getOrCreate(clazz);
  }

  /**
   * Returns a cached instance of the given configuration interface
   * that also supports mutability.
   * <p>
   * Use this when you need to dynamically override configuration
   * properties at runtime.
   *
   * @param clazz the configuration interface type
   * @param <T>   a type extending {@link Config} and {@link Mutable}
   * @return a cached mutable configuration instance
   */
  public static <T extends Config & Mutable> T getMutable(Class<T> clazz) {
    log.debug("Fetching Mutable Config instance for {}", clazz.getSimpleName());
    return ConfigCache.getOrCreate(clazz);
  }

  /**
   * Returns a cached instance of the given configuration interface
   * that supports both mutability and reloading.
   * <p>
   * Use this when you need to override properties and then refresh
   * the configuration with {@link Reloadable#reload()}.
   *
   * @param clazz the configuration interface type
   * @param <T>   a type extending {@link Config}, {@link Mutable}, and {@link Reloadable}
   * @return a cached reloadable configuration instance
   */
  public static <T extends Config & Mutable & Reloadable> T getReloadable(Class<T> clazz) {
    log.debug("Fetching Reloadable Config instance for {}", clazz.getSimpleName());
    return ConfigCache.getOrCreate(clazz);
  }

  /**
   * Overlays the provided key-value pairs on top of the current configuration.
   * After applying the overrides, the configuration is reloaded.
   *
   * @param cfg   the configuration instance (must implement Mutable &amp; Reloadable)
   * @param props the map of property overrides
   * @param <T>   the type of the configuration
   */
  public static <T extends Mutable & Reloadable> void overlay(T cfg, Map<String, String> props) {
    Objects.requireNonNull(cfg, "config is null");
    Objects.requireNonNull(props, "props is null");
    log.info("Applying {} property overrides to {}", props.size(), cfg.getClass().getSimpleName());
    props.forEach((key, value) -> {
      log.debug("override {} = {}", key, value);
      cfg.setProperty(key, value);
    });
    cfg.reload();
    log.info("Configuration {} reloaded after map overlay", cfg.getClass().getSimpleName());
  }

  /**
   * Overlays the provided Properties object on top of the current configuration.
   * After applying the overrides, the configuration is reloaded.
   *
   * @param cfg   the configuration instance (must implement Mutable &amp; Reloadable)
   * @param props the properties to overlay
   * @param <T>   the type of the configuration
   */
  public static <T extends Mutable & Reloadable> void overlay(T cfg, Properties props) {
    Objects.requireNonNull(cfg, "config is null");
    Objects.requireNonNull(props, "props is null");
    log.info("Applying {} property overrides (Properties) to {}", props.size(), cfg.getClass().getSimpleName());
    props.forEach((key, value) -> {
      log.debug("override {} = {}", key, value);
      cfg.setProperty(String.valueOf(key), String.valueOf(value));
    });
    cfg.reload();
    log.info("Configuration {} reloaded after Properties overlay", cfg.getClass().getSimpleName());
  }

  /**
   * Loads properties from the given InputStream into the configuration,
   * replacing existing values where applicable. After loading, the configuration is reloaded.
   *
   * @param cfg the configuration instance (must implement Mutable &amp; Reloadable)
   * @param in  the input stream to load from
   * @param <T> the type of the configuration
   * @throws IOException if the stream cannot be read
   */
  public static <T extends Mutable & Reloadable> void load(T cfg, InputStream in) throws IOException {
    Objects.requireNonNull(cfg, "config is null");
    Objects.requireNonNull(in, "input stream is null");
    log.info("Loading properties from InputStream into {}", cfg.getClass().getSimpleName());
    cfg.load(in);
    cfg.reload();
    log.info("Configuration {} reloaded after InputStream load", cfg.getClass().getSimpleName());
  }

  /**
   * Loads properties from the given Reader into the configuration,
   * replacing existing values where applicable. After loading, the configuration is reloaded.
   *
   * @param cfg    the configuration instance (must implement Mutable &amp; Reloadable)
   * @param reader the reader to load from
   * @param <T>    the type of the configuration
   * @throws IOException if the reader cannot be read
   */
  public static <T extends Mutable & Reloadable> void load(T cfg, Reader reader) throws IOException {
    Objects.requireNonNull(cfg, "config is null");
    Objects.requireNonNull(reader, "reader is null");
    log.info("Loading properties from Reader into {}", cfg.getClass().getSimpleName());
    cfg.load(reader);
    cfg.reload();
    log.info("Configuration {} reloaded after Reader load", cfg.getClass().getSimpleName());
  }
}
