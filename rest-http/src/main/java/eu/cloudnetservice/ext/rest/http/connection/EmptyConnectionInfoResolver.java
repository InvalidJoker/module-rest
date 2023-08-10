package eu.cloudnetservice.ext.rest.http.connection;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import lombok.NonNull;

public final class EmptyConnectionInfoResolver implements HttpConnectionInfoResolver {

  public static final HttpConnectionInfoResolver INSTANCE = new EmptyConnectionInfoResolver();

  private EmptyConnectionInfoResolver() {
  }

  @Override
  public @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo
  ) {
    return baseInfo;
  }
}
