package eu.cloudnetservice.ext.rest.http.connection.parse;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.connection.BasicHttpConnectionInfo;
import eu.cloudnetservice.ext.rest.http.connection.HttpConnectionInfoResolver;
import io.netty5.handler.codec.http.HttpHeaderNames;
import lombok.NonNull;

public final class HostHeaderConnectionInfoResolver implements HttpConnectionInfoResolver {

  public static final HttpConnectionInfoResolver INSTANCE = new HostHeaderConnectionInfoResolver();

  private HostHeaderConnectionInfoResolver() {
  }

  @Override
  public @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo
  ) {
    // extract the server target information from the given host header
    // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Host
    var hostHeaderValue = context.request().header(HttpHeaderNames.HOST.toString());
    if (hostHeaderValue != null) {
      var defaultPort = baseInfo.defaultPortForScheme();
      var parsedAddress = AddressParseUtil.parseHostAndPort("Host", hostHeaderValue, defaultPort);
      baseInfo = baseInfo.withHostAddress(parsedAddress);
    }

    return baseInfo;
  }
}
