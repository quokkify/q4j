package dev.quokkify.filter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.restassured.filter.log.LogDetail;
import io.restassured.internal.print.RequestPrinter;
import io.restassured.internal.print.ResponsePrinter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ApiLogFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ApiLogFilter.class);

  protected static final int MAX_BODY_LENGTH_FOR_LOG_TO_CONSOLE = 5000;
  private static final boolean PRETTY_PRINT = true;
  private static final Set<String> NO_BLACKLIST = Collections.emptySet();

  protected Response processFilter(FilterableRequestSpecification requestSpec, Response response) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(buildCurlLog(requestSpec, response));
    }
    return response;
  }

  private String buildCurlLog(FilterableRequestSpecification requestSpec, Response response) {
    String reqText = captureText(ps ->
        RequestPrinter.print(requestSpec, requestSpec.getMethod(), requestSpec.getURI(),
            LogDetail.ALL, NO_BLACKLIST, ps, PRETTY_PRINT));

    String resBody = response.getBody().asString();
    boolean bodyTooLong = StringUtils.isNotBlank(resBody) && resBody.length() > MAX_BODY_LENGTH_FOR_LOG_TO_CONSOLE;

    String resText = buildResponseText(response, resBody, bodyTooLong);

    String requestBlock = Arrays.stream(reqText.split("\n", -1))
        .filter(l -> StringUtils.isNotBlank(l) && !l.trim().endsWith("<none>"))
        .map(l -> "> " + l)
        .collect(Collectors.joining("\n"));

    List<String> resLines = Arrays.stream(resText.split("\n", -1))
        .filter(l -> StringUtils.isNotBlank(l) && !l.trim().endsWith("<none>"))
        .collect(Collectors.toList());
    if (!resLines.isEmpty()) {
      resLines.set(0, resLines.get(0).stripTrailing() + " (" + response.getTime() + "ms)");
    }
    String responseBlock = resLines.stream()
        .map(l -> "< " + l)
        .collect(Collectors.joining("\n"));

    return requestBlock + "\n\n" + responseBlock;
  }

  private String buildResponseText(Response response, String resBody, boolean bodyTooLong) {
    if (!bodyTooLong) {
      return captureText(ps ->
          ResponsePrinter.print(response, response, ps, LogDetail.ALL, PRETTY_PRINT, NO_BLACKLIST));
    }
    String statusPart = captureText(ps ->
        ResponsePrinter.print(response, response, ps, LogDetail.STATUS, PRETTY_PRINT, NO_BLACKLIST));
    String headersPart = captureText(ps ->
        ResponsePrinter.print(response, response, ps, LogDetail.HEADERS, PRETTY_PRINT, NO_BLACKLIST));
    String cookiesPart = captureText(ps ->
        ResponsePrinter.print(response, response, ps, LogDetail.COOKIES, PRETTY_PRINT, NO_BLACKLIST));
    return statusPart.stripTrailing()
        + "\n" + headersPart.stripTrailing()
        + "\n" + cookiesPart.stripTrailing()
        + "\n\nBody:\n[body too long: " + resBody.length() + " chars, exceeds limit]";
  }

  private String captureText(Function<PrintStream, String> printer) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (PrintStream ps = new PrintStream(out, false, StandardCharsets.UTF_8)) {
      return printer.apply(ps);
    }
  }
}
