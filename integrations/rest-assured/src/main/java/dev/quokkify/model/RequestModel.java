package dev.quokkify.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Object for api request.
 */
public abstract class RequestModel {

  private Map<String, String> cookies = new HashMap<>();
  private Map<String, String> headers = new HashMap<>();
  private Map<String, String> params = new HashMap<>();
  private String url;

  public Map<String, String> getCookies() {
    return cookies;
  }

  public RequestModel setCookies(Map<String, String> cookies) {
    this.cookies = Objects.requireNonNullElseGet(cookies, HashMap::new);
    return this;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public RequestModel setHeaders(Map<String, String> headers) {
    this.headers = Objects.requireNonNullElseGet(headers, HashMap::new);
    return this;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public RequestModel setParams(Map<String, String> params) {
    this.params = Objects.requireNonNullElseGet(params, HashMap::new);
    return this;
  }

  public String getUrl() {
    return url;
  }

  public RequestModel setUrl(String url) {
    this.url = url;
    return this;
  }

  @Override
  public String toString() {
    return "RequestModel{" +
        "cookies=" + cookies +
        ", headers=" + headers +
        ", params=" + params +
        ", url='" + url + "'" +
        "}";
  }

  @Override
  public int hashCode() {
    return Objects.hash(cookies, headers, params, url);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof RequestModel other)) return false;
    return Objects.equals(cookies, other.cookies)
        && Objects.equals(headers, other.headers)
        && Objects.equals(params, other.params)
        && Objects.equals(url, other.url);
  }
}
