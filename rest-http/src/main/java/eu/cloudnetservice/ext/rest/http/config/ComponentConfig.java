package eu.cloudnetservice.ext.rest.http.config;

import eu.cloudnetservice.ext.rest.http.connection.EmptyConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.http.connection.HttpConnectionInfoResolver;
import lombok.NonNull;

public record ComponentConfig(
  @NonNull CorsConfig corsConfig,
  @NonNull HttpConnectionInfoResolver connectionInfoResolver
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ComponentConfig contextConfig) {
    return new Builder().corsConfig(contextConfig.corsConfig());
  }

  public static final class Builder {

    private CorsConfig.Builder corsConfigBuilder = CorsConfig.builder();
    private HttpConnectionInfoResolver connectionInfoResolver = EmptyConnectionInfoResolver.INSTANCE;

    public @NonNull Builder corsConfig(@NonNull CorsConfig corsConfig) {
      this.corsConfigBuilder = CorsConfig.builder(corsConfig);
      return this;
    }

    public @NonNull Builder corsConfig(@NonNull CorsConfig.Builder corsConfigBuilder) {
      this.corsConfigBuilder = corsConfigBuilder;
      return this;
    }

    public @NonNull Builder connectionInfoResolver(@NonNull HttpConnectionInfoResolver resolver) {
      this.connectionInfoResolver = resolver;
      return this;
    }

    public @NonNull Builder appendConnectionInfoResolveStep(@NonNull HttpConnectionInfoResolver resolveStep) {
      this.connectionInfoResolver = this.connectionInfoResolver.then(resolveStep);
      return this;
    }

    public @NonNull ComponentConfig build() {
      return new ComponentConfig(this.corsConfigBuilder.build(), this.connectionInfoResolver);
    }
  }
}
