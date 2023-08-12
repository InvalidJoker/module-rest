package eu.cloudnetservice.ext.rest.http.connection.parse;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.connection.BasicHttpConnectionInfo;
import eu.cloudnetservice.ext.rest.http.connection.HttpConnectionInfoResolver;
import java.util.regex.Pattern;
import lombok.NonNull;

public final class ForwardedSyntaxConnectionInfoResolver implements HttpConnectionInfoResolver {

  private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=\"?([^;,\"]+)\"?");
  private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?");
  private static final Pattern FORWARDED_SCHEME_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?");

  private final String headerName;

  public ForwardedSyntaxConnectionInfoResolver(@NonNull String headerName) {
    this.headerName = headerName;
  }

  @Override
  public @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo
  ) {
    // extract the forwarded information - break in case the info is not present
    var forwardedHeaderValue = context.request().header(this.headerName);
    if (forwardedHeaderValue == null) {
      return baseInfo;
    }

    // the forwarded header basically contains a list of entries for each redirected request.
    // if there are multiple proxy servers, the info of each proxy will be appended at the end, hence
    // we extract the first information to get the information about the first request (from the client).
    // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded#syntax
    var forwardingInfo = forwardedHeaderValue.split(",", 2)[0];

    // extract information about the forwarded scheme
    var forwardedSchemeMatcher = FORWARDED_SCHEME_PATTERN.matcher(forwardingInfo);
    if (forwardedSchemeMatcher.find()) {
      baseInfo = baseInfo.withScheme(forwardedSchemeMatcher.group(1));
    }

    // extract the information from the forwarded for header (the client info)
    var forwardedForMatcher = FORWARDED_FOR_PATTERN.matcher(forwardingInfo);
    if (forwardedForMatcher.find()) {
      var defaultPort = baseInfo.clientAddress().port();
      var parsedAddress = AddressParseUtil.parseHostAndPort(this.headerName, forwardedForMatcher.group(1), defaultPort);
      baseInfo = baseInfo.withClientAddress(parsedAddress);
    }

    // extract the information from the host information (info about the target server listener)
    var forwardedHostMatcher = FORWARDED_HOST_PATTERN.matcher(forwardingInfo);
    if (forwardedHostMatcher.find()) {
      var defaultPort = baseInfo.defaultPortForScheme();
      var parsedAddress = AddressParseUtil.parseHostAndPort(
        this.headerName,
        forwardedHostMatcher.group(1),
        defaultPort);
      baseInfo = baseInfo.withHostAddress(parsedAddress);
    }

    return baseInfo;
  }
}
