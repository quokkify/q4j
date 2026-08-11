package dev.quokkify.converter;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import dev.quokkify.constant.StringConstant;
import dev.quokkify.parser.RegexParser;

import io.hypersistence.utils.hibernate.query.SQLExtractor;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlConverter {

  private static final Logger LOG = LoggerFactory.getLogger(SqlConverter.class);

  private static final Pattern QMARK = Pattern.compile("\\?");
  private static final Pattern P_OFFSET = Pattern.compile("(?is)(offset)\\s+\\?(\\s+rows)?");
  private static final Pattern P_FETCH_FIRST = Pattern.compile("(?is)(fetch\\s+first)\\s+\\?(\\s+rows\\s+only)");

  public static String convertToString(Query query) {
    String sql = SQLExtractor.from(query);
    LOG.trace("Raw SQL: {}", System.lineSeparator() + sql);
    List<Parameter<?>> params = query.getParameters().stream()
        .sorted(Comparator.comparingInt(Parameter::getPosition))
        .toList();
    for (Parameter<?> parameter : params) {
      Object value = query.getParameterValue(parameter.getPosition());
      sql = replaceByValue(sql, value, parameter.getPosition());
    }
    sql = applyOffset(sql, query.getFirstResult());
    sql = applyFetchFirst(sql, query.getMaxResults());
    String prettySql = FormatStyle.BASIC.getFormatter().format(sql);
    LOG.debug("Executable SQL: {}", System.lineSeparator() + prettySql);
    return prettySql;
  }

  private static String replaceByValue(String sql, Object value, int position) {
    if (value == null) {
      return replaceNull(sql, position);
    }
    if (value instanceof Iterable<?> iterable) {
      return replaceIterable(sql, iterable, position);
    }
    if (value.getClass().isArray()) {
      return replaceArray(sql, value, position);
    }
    return replaceScalar(sql, value, position);
  }

  private static String replaceNull(String sql, int pos) {
    LOG.trace("Replacing '?' with parameter[{}] = NULL", pos);
    return replaceNext(sql, "NULL");
  }

  private static String replaceIterable(String sql, Iterable<?> iterable, int position) {
    String out = sql;
    for (Object element : iterable) {
      String sqlValue = formatScalar(element);
      LOG.trace("Replacing '?' with collection element for parameter[{}] = {}", position, sqlValue);
      out = replaceNext(out, sqlValue);
    }
    return out;
  }

  private static String replaceArray(String sql, Object array, int position) {
    String out = sql;
    int len = Array.getLength(array);
    for (int index = 0; index < len; index++) {
      Object element = Array.get(array, index);
      String sqlValue = formatScalar(element);
      LOG.trace("Replacing '?' with array element for parameter[{}] = {}", position, sqlValue);
      out = replaceNext(out, sqlValue);
    }
    return out;
  }

  private static String replaceScalar(String sql, Object value, int position) {
    String string = formatScalar(value);
    LOG.trace("Replacing '?' with parameter[{}] = {}", position, string);
    return replaceNext(sql, string);
  }

  private static String replaceNext(String sql, String value) {
    return RegexParser.replaceFirst(QMARK, sql, value);
  }

  private static String applyOffset(String sql, int offset) {
    if (offset <= 0) return sql;
    if (RegexParser.nonMatched(P_OFFSET, sql)) return sql;
    String group1 = RegexParser.parse(P_OFFSET, sql, 1);
    String group2 = RegexParser.parse(P_OFFSET, sql, 2);
    String rowsPart = Objects.requireNonNullElse(group2, StringUtils.EMPTY);
    String replacement = group1 + StringUtils.SPACE + offset + rowsPart;
    LOG.trace("Applied OFFSET {}", offset);
    return RegexParser.replaceFirst(P_OFFSET, sql, replacement);
  }

  private static String applyFetchFirst(String sql, int max) {
    if (max <= 0) return sql;
    if (RegexParser.nonMatched(P_FETCH_FIRST, sql)) return sql;
    String group1 = RegexParser.parse(P_FETCH_FIRST, sql, 1);
    String group2 = RegexParser.parse(P_FETCH_FIRST, sql, 2);
    String replacement = group1 + StringUtils.SPACE + max + group2;
    LOG.trace("Applied FETCH FIRST {}", max);
    return RegexParser.replaceFirst(P_FETCH_FIRST, sql, replacement);
  }

  private static String formatScalar(Object value) {
    return switch (value) {
      case null -> "NULL";
      case Boolean bool -> BooleanUtils.toStringTrueFalse(bool);
      case Number number -> number.toString();
      default -> {
        String string = value.toString().replace(StringConstant.APOSTROPHE, StringConstant.DOUBLE_APOSTROPHE);
        yield StringConstant.APOSTROPHE + string + StringConstant.APOSTROPHE;
      }
    };
  }
}
