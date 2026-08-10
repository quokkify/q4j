package dev.quokkify.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class NavigationUrlUtils {

  private NavigationUrlUtils() {
  }

  static String addQueryParameters(String path, Map<String, Object> queryParams) throws URISyntaxException {
    URI uri = URI.create(path);
    StringBuilder query = new StringBuilder(uri.getRawQuery() == null ? "" : uri.getRawQuery());
    for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
      Object value = Objects.requireNonNull(entry.getValue(), "Query param value must not be null for key: " + entry.getKey());
      if (value instanceof List<?> list) {
        for (Object element : list) {
          appendQueryParam(query, entry.getKey(), String.valueOf(element));
        }
      } else {
        appendQueryParam(query, entry.getKey(), String.valueOf(value));
      }
    }
    return new URI(
        uri.getScheme(),
        uri.getRawAuthority(),
        uri.getRawPath(),
        query.length() == 0 ? null : query.toString(),
        uri.getRawFragment()
    ).toString();
  }

  private static void appendQueryParam(StringBuilder query, String key, String value) {
    if (query.length() > 0) {
      query.append('&');
    }
    query.append(urlEncode(key)).append('=').append(urlEncode(value));
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
