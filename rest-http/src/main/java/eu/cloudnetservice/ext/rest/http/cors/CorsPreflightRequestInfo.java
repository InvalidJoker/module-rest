package eu.cloudnetservice.ext.rest.http.cors;

import java.util.List;
import lombok.NonNull;

public record CorsPreflightRequestInfo(
  @NonNull String origin,
  @NonNull String requestMethod,
  @NonNull List<String> requestHeaders
) {

}
