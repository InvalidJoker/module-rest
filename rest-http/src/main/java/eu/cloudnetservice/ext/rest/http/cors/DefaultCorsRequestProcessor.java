package eu.cloudnetservice.ext.rest.http.cors;

import com.google.common.base.Splitter;
import eu.cloudnetservice.ext.rest.http.HttpChannel;
import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpRequest;
import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.config.CorsConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import io.netty5.handler.codec.http.HttpHeaderNames;
import io.netty5.handler.codec.http.HttpMethod;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultCorsRequestProcessor implements CorsRequestProcessor {

  private static final Splitter REQUEST_HEADERS_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

  @Override
  public @Nullable CorsPreflightRequestInfo extractInfoFromPreflightRequest(@NonNull HttpRequest request) {
    // check if the request method is the expected
    var requestMethod = request.method();
    if (!requestMethod.equalsIgnoreCase(HttpMethod.OPTIONS.name())) {
      return null;
    }

    // check if the required headers origin & request method are present
    var origin = request.header(HttpHeaderNames.ORIGIN.toString());
    var clientRequestMethod = request.header(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD.toString());
    if (origin == null || clientRequestMethod == null) {
      return null;
    }

    // extract the client request headers, default to empty list if not given
    var clientRequestHeaders = request.header(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS.toString());
    if (clientRequestHeaders == null) {
      return new CorsPreflightRequestInfo(origin, clientRequestMethod, List.of());
    }

    return new CorsPreflightRequestInfo(
      origin,
      clientRequestMethod,
      REQUEST_HEADERS_SPLITTER.splitToList(clientRequestHeaders));
  }

  @Override
  public void processPreflightRequest(
    @NonNull HttpContext context,
    @NonNull CorsPreflightRequestInfo preflightRequestInfo,
    @Nullable HttpHandlerConfig httpHandlerConfig
  ) {
    // check if the request needs CORS information
    var crossOrigin = this.extractCrossOrigin(context.channel(), context.request());
    if (crossOrigin == null) {
      return;
    }

    // ensure that a corsConfig configuration is present in order to process the CORS request
    var corsConfig = httpHandlerConfig != null ? httpHandlerConfig.corsConfig() : null;
    if (corsConfig == null) {
      this.rejectRequest(context.response());
      return;
    }

    this.filterAndPreprocessCorsRequest(
      preflightRequestInfo.origin(),
      preflightRequestInfo.requestMethod(),
      httpHandlerConfig.httpMethod(),
      preflightRequestInfo.requestHeaders(),
      corsConfig,
      context.request(),
      context.response(),
      true);
  }

  @Override
  public boolean processNormalRequest(@NonNull HttpContext context, @NonNull HttpHandlerConfig httpHandlerConfig) {
    // check if the request needs CORS information
    var crossOrigin = this.extractCrossOrigin(context.channel(), context.request());
    if (crossOrigin == null) {
      return true;
    }

    // check if a corsConfig corsConfig is present for the handler
    var corsConfig = httpHandlerConfig.corsConfig();
    if (corsConfig == null) {
      return true;
    }

    // get the base information needed to handle the CORS request
    var request = context.request();
    var headerNames = context.request().headers().keySet();

    // actually handle and validate the request
    return this.filterAndPreprocessCorsRequest(
      crossOrigin,
      request.method(),
      httpHandlerConfig.httpMethod(),
      headerNames,
      corsConfig,
      request,
      context.response(),
      false);
  }

  private boolean filterAndPreprocessCorsRequest(
    @NonNull String origin,
    @NonNull String method,
    @NonNull String allowedMethod,
    @NonNull Collection<String> headerNames,
    @NonNull CorsConfig corsConfig,
    @NonNull HttpRequest httpRequest,
    @NonNull HttpResponse httpResponse,
    boolean preflight
  ) {
    // append information about the headers that on change might lead to a different handling result
    httpResponse.addHeader(HttpHeaderNames.VARY.toString(), HttpHeaderNames.ORIGIN.toString());
    httpResponse.addHeader(HttpHeaderNames.VARY.toString(), HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD.toString());
    httpResponse.addHeader(HttpHeaderNames.VARY.toString(), HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS.toString());

    // check if the given origin is valid
    var validOrigin = corsConfig.findMatchingOrigin(origin);
    if (validOrigin == null) {
      this.rejectRequest(httpResponse);
      return false;
    }

    // check if the used request method is valid
    if (!method.equalsIgnoreCase(allowedMethod)) {
      this.rejectRequest(httpResponse);
      return false;
    }

    if (preflight) {
      // validate that the supplied headers are allowed
      var filteredHeaders = corsConfig.filterHeaders(headerNames);
      if (filteredHeaders == null) {
        this.rejectRequest(httpResponse);
        return false;
      }

      // respond with the allowed filtered headers
      if (!filteredHeaders.isEmpty()) {
        var allowedHeaderString = String.join(", ", filteredHeaders);
        httpResponse.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString(), allowedHeaderString);
      }

      // add info about the allowed method for the request
      httpResponse.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString(), allowedMethod);

      // add info about the max time the preflight response can be cached, if provided
      var maxAge = corsConfig.maxAge();
      if (maxAge != null) {
        httpResponse.header(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE.toString(), Long.toString(maxAge.getSeconds()));
      }

      // indicates to the browser is requests from public networks to private networks are allowed
      // https://developer.chrome.com/blog/private-network-access-preflight
      if (httpRequest.hasHeader(HttpHeaderNames.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK.toString())) {
        var privateNetworkAllowed = Boolean.TRUE.equals(corsConfig.allowPrivateNetworks());
        httpResponse.header(
          HttpHeaderNames.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK.toString(),
          Boolean.toString(privateNetworkAllowed));
      }

      // preflight requests are answered with 204 (no content)
      httpResponse.status(HttpResponseCode.NO_CONTENT);
    }

    // set the origin that we filtered out to be allowed
    httpResponse.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), validOrigin);

    // add the exposed headers to the response in case there are any
    var exposedHeaders = corsConfig.exposedHeaders();
    if (!exposedHeaders.isEmpty()) {
      var exposedHeadersString = String.join(", ", exposedHeaders);
      httpResponse.header(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS.toString(), exposedHeadersString);
    }

    // mark credentials to be allowed if configured
    if (Boolean.TRUE.equals(corsConfig.allowCredentials())) {
      httpResponse.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(), "true");
    }

    return true;
  }

  private void rejectRequest(@NonNull HttpResponse response) {
    response.status(HttpResponseCode.FORBIDDEN);
    response.body("Provided information does not match CORS rules");
  }

  private @Nullable String extractCrossOrigin(@NonNull HttpChannel channel, @NonNull HttpRequest request) {
    // check if the origin header is present
    var origin = request.header(HttpHeaderNames.ORIGIN.toString());
    if (origin == null) {
      return null;
    }

    // extract the origin (request) information
    var parsedOrigin = URI.create(origin);
    var originScheme = parsedOrigin.getScheme();
    var originHost = parsedOrigin.getHost();
    var originPort = parsedOrigin.getPort();

    // get the service information
    var serverScheme = channel.scheme();
    var serverHost = channel.serverAddress().host();
    var serverPort = channel.serverAddress().port();

    if (Objects.equals(originScheme, serverScheme)
      && Objects.equals(originHost, serverHost)
      && originPort == serverPort) {
      // request is from the same origin, no CORS needed
      return null;
    } else {
      // request is from a different origin
      return origin;
    }
  }
}
