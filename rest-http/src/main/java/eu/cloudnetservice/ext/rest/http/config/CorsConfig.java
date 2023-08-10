package eu.cloudnetservice.ext.rest.http.config;

import com.google.common.base.Strings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record CorsConfig(
  @NonNull List<Pattern> allowedOrigins,
  @NonNull List<String> allowedHeaders,
  @NonNull List<String> exposedHeaders,
  @Nullable Boolean allowCredentials,
  @Nullable Boolean allowPrivateNetworks,
  @Nullable Duration maxAge
) {

  private static final Pattern ALLOW_ALL = Pattern.compile(".*");

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull CorsConfig config) {
    return builder()
      .allowedOrigins(config.allowedOrigins())
      .allowedHeaders(config.allowedHeaders())
      .exposedHeaders(config.exposedHeaders())
      .allowCredentials(config.allowCredentials())
      .allowPrivateNetworks(config.allowPrivateNetworks())
      .maxAge(config.maxAge());
  }

  public @NonNull CorsConfig combine(@Nullable CorsConfig other) {
    if (other == null) {
      return this;
    }

    return builder(this)
      .allowedOrigins(combine(this.allowedOrigins, other.allowedOrigins(), ALLOW_ALL))
      .allowedHeaders(combine(this.allowedHeaders, other.allowedHeaders(), "*"))
      .exposedHeaders(combine(this.exposedHeaders, other.exposedHeaders(), "*"))
      .allowCredentials(combine(this.allowCredentials, other.allowCredentials()))
      .allowPrivateNetworks(combine(this.allowPrivateNetworks, other.allowPrivateNetworks()))
      .maxAge(combine(this.maxAge(), other.maxAge()))
      .build();
  }

  public @Nullable String findMatchingOrigin(@Nullable String origin) {
    if (this.allowedOrigins.isEmpty() || Strings.isNullOrEmpty(origin)) {
      return null;
    }

    var trimmedOrigin = trimTrailingSlash(origin);
    for (var originPattern : this.allowedOrigins) {
      if (originPattern.matcher(trimmedOrigin).matches()) {
        return trimmedOrigin;
      }
    }

    return null;
  }

  public @Nullable Collection<String> filterHeaders(@NonNull Collection<String> requestedHeaders) {
    if (requestedHeaders.isEmpty()) {
      return List.of();
    }

    if (this.allowedHeaders.isEmpty()) {
      return null;
    }

    List<String> results = new ArrayList<>();
    var allowAll = this.allowedHeaders.contains("*");
    for (var requestedHeader : requestedHeaders) {
      if (!requestedHeader.isBlank()) {
        var trimmedHeader = requestedHeader.trim();
        if (allowAll) {
          // all headers are allowed, no need to check further
          results.add(trimmedHeader);
        } else {
          // check if the requested header is part of the allowed headers
          for (var allowedHeader : this.allowedHeaders) {
            if (allowedHeader.equalsIgnoreCase(trimmedHeader)) {
              results.add(trimmedHeader);
              break;
            }
          }
        }
      }
    }

    return results.isEmpty() ? null : results;
  }

  private static <T> @NonNull List<T> combine(@NonNull List<T> left, @NonNull List<T> right, T permitAll) {
    if (left.contains(permitAll) || right.contains(permitAll)) {
      return List.of(permitAll);
    }

    Set<T> combined = new HashSet<>(left.size() + right.size());
    combined.addAll(left);
    combined.addAll(right);
    return new ArrayList<>(combined);
  }

  private static <T> @Nullable T combine(@Nullable T specific, @Nullable T global) {
    return specific != null ? specific : global;
  }

  private static @NonNull String trimTrailingSlash(@NonNull String input) {
    return input.endsWith("/") ? input.substring(0, input.length() - 1) : input;
  }

  public static final class Builder {

    private List<Pattern> allowedOrigins = new ArrayList<>();
    private List<String> allowedHeaders = new ArrayList<>();
    private List<String> exposedHeaders = new ArrayList<>();
    private Boolean allowCredentials;
    private Boolean allowPrivateNetworks;
    private Duration maxAge;

    public @NonNull Builder addAllowedOrigin(@NonNull String allowedOrigin) {
      return this.addAllowedOrigin(Pattern.compile(Pattern.quote(allowedOrigin)));
    }

    public @NonNull Builder addAllowedOrigin(@NonNull Pattern allowedOrigin) {
      this.allowedOrigins.add(allowedOrigin);
      return this;
    }

    public @NonNull Builder allowedOrigins(@NonNull List<Pattern> allowedOrigins) {
      this.allowedOrigins = new ArrayList<>(allowedOrigins);
      return this;
    }

    public @NonNull Builder modifyAllowedOrigins(@NonNull Consumer<List<Pattern>> allowedOriginConsumer) {
      allowedOriginConsumer.accept(this.allowedOrigins);
      return this;
    }

    public @NonNull Builder addAllowedHeader(@NonNull String allowedHeader) {
      this.allowedHeaders.add(allowedHeader);
      return this;
    }

    public @NonNull Builder allowedHeaders(@NonNull List<String> allowedHeaders) {
      this.allowedHeaders = new ArrayList<>(allowedHeaders);
      return this;
    }

    public @NonNull Builder modifyAllowedHeaders(@NonNull Consumer<List<String>> allowedHeaderConsumer) {
      allowedHeaderConsumer.accept(this.allowedHeaders);
      return this;
    }

    public @NonNull Builder addExposedHeader(@NonNull String exposedHeader) {
      this.exposedHeaders.add(exposedHeader);
      return this;
    }

    public @NonNull Builder exposedHeaders(@NonNull List<String> exposedHeaders) {
      this.exposedHeaders = new ArrayList<>(exposedHeaders);
      return this;
    }

    public @NonNull Builder modifyExposedHeaders(@NonNull Consumer<List<String>> exposedHeaderConsumer) {
      exposedHeaderConsumer.accept(this.exposedHeaders);
      return this;
    }

    public @NonNull Builder allowCredentials(@Nullable Boolean allowCredentials) {
      this.allowCredentials = allowCredentials;
      return this;
    }

    public @NonNull Builder allowPrivateNetworks(@Nullable Boolean allowPrivateNetworks) {
      this.allowPrivateNetworks = allowPrivateNetworks;
      return this;
    }

    public @NonNull Builder maxAge(@Nullable Duration maxAge) {
      this.maxAge = maxAge;
      return this;
    }

    public @NonNull CorsConfig build() {
      return new CorsConfig(
        List.copyOf(this.allowedOrigins),
        List.copyOf(this.allowedHeaders),
        List.copyOf(this.exposedHeaders),
        this.allowCredentials,
        this.allowPrivateNetworks,
        this.maxAge);
    }
  }

}
