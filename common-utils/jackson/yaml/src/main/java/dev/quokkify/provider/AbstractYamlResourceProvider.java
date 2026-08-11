package dev.quokkify.provider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import dev.quokkify.ex.ResourceException;
import dev.quokkify.parser.YamlParser;

/**
 * Abstract YAML resource provider with explicit typing and thread-safe resource locking.
 *
 * <p>Child classes provide:
 * <ul>
 *   <li>{@link #getPath()} – resource path in classpath</li>
 *   <li>{@link #getEntityClass()} – entity class of T</li>
 *   <li>{@link #getShape()} – resource shape: LIST, MAP, or SINGLE</li>
 * </ul>
 *
 * <p>Resource data is cached per path. Lockable entities (extending {@link LockingResource})
 * can be acquired via {@link #getFreeLockingResource(List)} and must be released with
 * {@link #freeLockingResources()} (typically in a finally block).</p>
 *
 * @param <T> entity type
 * @param <V> converted value type: List&lt;T&gt; | Map&lt;String, T&gt; | T
 */
public abstract class AbstractYamlResourceProvider<T, V> implements LocalResource {

  /** Cache of loaded resources keyed by path. */
  private final Map<String, V> resourceData = new ConcurrentHashMap<>();

  /** Thread-local list of resources acquired (locked) by the current thread. */
  private final ThreadLocal<List<LockingResource>> threadLockingResources = ThreadLocal.withInitial(ArrayList::new);

  /** Resource path within classpath (override). */
  protected abstract String getPath();

  /** Entity class of T (override). */
  protected abstract Class<T> getEntityClass();

  /** Resource shape: defines how YAML is interpreted (override). */
  protected abstract Shape getShape();

  /** Supported YAML shapes. */
  public enum Shape { LIST, MAP, SINGLE }

  /**
   * Acquire the least recently updated free resource from the provided list.
   * The resource is locked and registered in the thread-local list.
   *
   * @param lockingResources list of candidate resources
   * @return acquired resource cast to T
   * @throws RuntimeException if no resource can be acquired
   */
  @SuppressWarnings("unchecked")
  protected T getFreeLockingResource(List<? extends LockingResource> lockingResources) {
    List<? extends LockingResource> candidates = lockingResources.stream()
        .filter(LockingResource::isFree)
        .sorted(Comparator.comparing(LockingResource::getUpdatedAt))
        .toList();
    for (LockingResource candidate : candidates) {
      if (candidate.tryLock()) {
        threadLockingResources.get().add(candidate);
        return (T) candidate;
      }
    }
    throw new RuntimeException("No available locking resource");
  }

  /**
   * Returns the resource data, lazily loading and caching by path.
   */
  protected V getResourceData() {
    final String path = getPath();
    return resourceData.computeIfAbsent(path, p -> readResources());
  }

  /**
   * Release all resources acquired by the current thread.
   * Safe to call multiple times.
   */
  public void freeLockingResources() {
    List<LockingResource> list = threadLockingResources.get();
    for (LockingResource r : list) {
      r.unlock();
    }
    list.clear();
  }

  @SuppressWarnings("unchecked")
  private V readResources() {
    final String path = getPath();
    final Class<T> entityClass = getEntityClass();
    final Shape shape = getShape();

    return switch (shape) {
      case LIST -> {
        List<T> list = YamlParser.loadValuesFromMapFromResources(path, entityClass);
        if (list == null || list.isEmpty()) {
          throw new ResourceException(path);
        }
        yield (V) list;
      }
      case MAP -> {
        Map<String, T> map = YamlParser.loadAsMapFromResources(path, entityClass);
        if (map == null || map.isEmpty()) {
          throw new ResourceException(path);
        }
        yield (V) map;
      }
      case SINGLE -> {
        T obj = YamlParser.loadAsObjectFromResources(path, entityClass);
        if (Objects.isNull(obj)) {
          throw new ResourceException(path);
        }
        yield (V) obj;
      }
    };
  }
}
