package eu.cloudnetservice.ext.rest.http.config;

import lombok.NonNull;

public record ContextConfig(@NonNull CorsConfig cors) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ContextConfig contextConfig) {
    return new Builder().cors(contextConfig.cors());
  }

  public static final class Builder {

    private CorsConfig cors = CorsConfig.builder().build();

    public @NonNull Builder cors(@NonNull CorsConfig cors) {
      this.cors = cors;
      return this;
    }

    public @NonNull ContextConfig build() {
      return new ContextConfig(this.cors);
    }
  }

}
