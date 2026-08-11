package dev.quokkify.parser;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarLog;
import de.sstoehr.harreader.model.HarPostData;
import de.sstoehr.harreader.model.HarQueryParam;
import de.sstoehr.harreader.model.HarRequest;
import de.sstoehr.harreader.model.HarResponse;

/**
 * Utility class for parsing network Har.
 */
public class HarParser {

  private HarParser() {
  }

  /**
   * Get filtered har request query param value by param name from har by request url.
   *
   * @param har            entity for parsing
   * @param requestUrl     request url for filtering
   * @param queryParamName required query param name
   * @return har request query param value as {@link String}
   */
  public static String getLastRequestQueryParamValue(Har har, String requestUrl, String queryParamName) {
    return getLastRequestQueryParam(har, requestUrl, queryParamName).getValue();
  }

  /**
   * Get filtered har request query param by param name from har by request url.
   *
   * @param har            entity for parsing
   * @param requestUrl     request url for filtering
   * @param queryParamName required query param name
   * @return har request query param as {@link HarQueryParam}
   */
  public static HarQueryParam getLastRequestQueryParam(Har har, String requestUrl, String queryParamName) {
    return getLastRequestQueryParams(har, requestUrl).stream()
        .filter(queryParam -> queryParam.getName().equals(queryParamName))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Can not find '%s' query param".formatted(queryParamName)));
  }

  /**
   * Get filtered har request query params from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har request query params as {@link List}&lt;{@link HarQueryParam}&gt;
   */
  public static List<HarQueryParam> getLastRequestQueryParams(Har har, String requestUrl) {
    return Collections.unmodifiableList(getLastHarRequest(har, requestUrl).getQueryString());
  }

  /**
   * Get filtered har request body string from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har request body as {@link String}
   */
  public static String getLastRequestBodyAsString(Har har, String requestUrl) {
    return getLastHarEntryRequestPostData(har, requestUrl).getText();
  }

  /**
   * Get filtered har post data from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har post data as {@link HarPostData}
   */
  public static HarPostData getLastHarEntryRequestPostData(Har har, String requestUrl) {
    return getLastHarRequest(har, requestUrl).getPostData();
  }

  /**
   * Get filtered har response status code from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har response status code as {@link Integer}
   */
  public static int getLastHarEntryResponseStatusCode(Har har, String requestUrl) {
    return getLastHarResponse(har, requestUrl).getStatus();
  }

  /**
   * Get filtered har response from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har response as {@link HarResponse}
   */
  public static HarResponse getLastHarResponse(Har har, String requestUrl) {
    return getLastHarEntry(har, requestUrl).getResponse();
  }

  /**
   * Get filtered har request from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har request as {@link HarRequest}
   */
  public static HarRequest getLastHarRequest(Har har, String requestUrl) {
    return getLastHarEntry(har, requestUrl).getRequest();
  }

  /**
   * Get last filtered har entry from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har entry as {@link HarEntry}
   */
  public static HarEntry getLastHarEntry(Har har, String requestUrl) {
    return getHarEntries(har, requestUrl)
        .stream()
        .reduce((first, second) -> second)
        .orElseThrow(() -> new NoSuchElementException("Can not find any request with '%s' url".formatted(requestUrl)));
  }

  /**
   * Get filtered har entries from har by request url.
   *
   * @param har        entity for parsing
   * @param requestUrl request url for filtering
   * @return har entries as {@link List}&lt;{@link HarEntry}&gt;
   */
  public static List<HarEntry> getHarEntries(Har har, String requestUrl) {
    return getHarEntries(har).stream()
        .filter(entry -> entry.getRequest().getUrl().startsWith(requestUrl))
        .collect(Collectors.toList());
  }

  /**
   * Get har entries from har.
   *
   * @param har entity for parsing
   * @return har entries as {@link List}&lt;{@link HarEntry}&gt;
   */
  public static List<HarEntry> getHarEntries(Har har) {
    return getHarLog(har).getEntries();
  }

  /**
   * Get har log from har.
   *
   * @param har entity for parsing
   * @return har log as {@link HarLog}
   */
  public static HarLog getHarLog(Har har) {
    return har.getLog();
  }
}
