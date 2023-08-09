package eu.cloudnetservice.ext.rest.http.config;

import com.google.common.base.Strings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

  private static @NonNull String trimTrailingSlash(@NonNull String input) {
    return input.endsWith("/") ? input.substring(0, input.length() - 1) : input;
  }

}
