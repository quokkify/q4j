package dev.quokkify.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MatchingStrategy;

public class Mapper {

  private static final ModelMapper MODEL_MAPPER = new ModelMapper();

  private Mapper() {
  }

  /**
   * Created copy of object. Used 'STRICT' matching strategy by default.
   *
   * @param value object to copy
   * @param type  type of object
   * @return copied object
   */
  public static <T> T clone(T value, Class<T> type) {
    return clone(value, type, MatchingStrategies.STRICT);
  }

  /**
   * Created copy of object.
   *
   * @param value            object to copy
   * @param type             type of object
   * @param matchingStrategy clone matching strategy
   * @return copied object
   */
  public static <T> T clone(T value, Class<T> type, MatchingStrategy matchingStrategy) {
    MODEL_MAPPER.getConfiguration()
        .setDeepCopyEnabled(true)
        .setMatchingStrategy(matchingStrategy);
    return MODEL_MAPPER.map(value, type);
  }
}
