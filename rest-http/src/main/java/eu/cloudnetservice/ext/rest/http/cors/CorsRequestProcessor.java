package eu.cloudnetservice.ext.rest.http.cors;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpRequest;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface CorsRequestProcessor {

  @Nullable CorsPreflightRequestInfo extractInfoFromPreflightRequest(@NonNull HttpRequest request);

  void processPreflightRequest(
    @NonNull HttpContext context,
    @NonNull CorsPreflightRequestInfo preflightRequestInfo,
    @Nullable HttpHandlerConfig httpHandlerConfig);

  boolean processNormalRequest(@NonNull HttpContext context, @NonNull HttpHandlerConfig httpHandlerConfig);
}
