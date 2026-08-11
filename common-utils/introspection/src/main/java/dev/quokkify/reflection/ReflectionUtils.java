package dev.quokkify.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utils for working with reflection.
 */
public final class ReflectionUtils {

  private ReflectionUtils() {
  }

  /**
   * Returns the raw Class for generic parameter at {@code genericIndex} of the given class'
   * direct superclass. Throws if superclass is not parameterized or index is out of range.
   */
  public static Class<?> getGenericClassType(Class<?> clazz, int genericIndex) {
    Type genericSuperclass = clazz.getGenericSuperclass();
    if (!(genericSuperclass instanceof ParameterizedType pt)) {
      throw new IllegalStateException("Superclass of %s is not parameterized."
          .formatted(clazz.getCanonicalName()));
    }

    Type[] args = pt.getActualTypeArguments();
    if (genericIndex < 0 || genericIndex >= args.length) {
      throw new IllegalStateException("Generic index %d out of bounds [0..%d] for %s"
          .formatted(genericIndex, args.length - 1, clazz.getCanonicalName()));
    }

    return toRawClass(args[genericIndex])
        .orElseThrow(() -> new IllegalStateException(
            "Cannot resolve generic class for index %d in %s"
                .formatted(genericIndex, clazz.getCanonicalName())));
  }

  /**
   * Tries to normalize any {@link Type} into its raw {@link Class}.
   * Iteratively unwraps ParameterizedType, TypeVariable (first bound) and WildcardType (first upper bound).
   */
  private static Optional<Class<?>> toRawClass(Type type) {
    Type t = type;
    while (true) {
      if (t instanceof Class<?> c) {
        return Optional.of(c);
      }
      if (t instanceof ParameterizedType p) {
        t = p.getRawType();
        continue;
      }
      if (t instanceof TypeVariable<?> tv) {
        Type[] bounds = tv.getBounds();
        t = bounds.length > 0 ? bounds[0] : Object.class;
        continue;
      }
      if (t instanceof WildcardType wt) {
        Type[] ub = wt.getUpperBounds();
        t = ub.length > 0 ? ub[0] : Object.class;
        continue;
      }
      return Optional.empty();
    }
  }

  /**
   * Search class in superclass chain and return it if found (exact match).
   */
  public static Class<?> getClassByTypeFromHierarchy(Class<?> source, Class<?> target) {
    Class<?> current = source;
    while (current != null) {
      if (current.equals(target)) {
        return current;
      }
      current = current.getSuperclass();
    }
    throw new IllegalStateException("Cannot find class '%s' in hierarchy of '%s'"
        .formatted(target.getCanonicalName(), Objects.requireNonNull(source).getCanonicalName()));
  }

  /**
   * Get any method from {@link Class} with reflection.
   */
  public static Method getMethodWithAccessible(Class<?> sourceClass,
                                               String methodName,
                                               Class<?>... parameterTypes)
      throws NoSuchMethodException {
    Method method = sourceClass.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return method;
  }

  /**
   * Get field from {@link Class} with reflection.
   */
  @SuppressFBWarnings("REFLF_REFLECTION_MAY_INCREASE_ACCESSIBILITY_OF_FIELD")
  public static Field getFieldWithAccessible(Class<?> sourceClass, String fieldName)
      throws NoSuchFieldException {
    Field field = sourceClass.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field;
  }
}
