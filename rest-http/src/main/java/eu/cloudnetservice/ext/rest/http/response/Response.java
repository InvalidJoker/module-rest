package eu.cloudnetservice.ext.rest.http.response;

import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface Response<T> extends IntoResponse<T> {

  @Nullable T body();

  @NonNull HttpResponseCode responseCode();

  @Unmodifiable
  @NonNull Map<String, List<String>> headers();

  void serializeIntoResponse(@NonNull HttpResponse response);

  interface Builder<T, B extends Builder<T, B>> extends IntoResponse<T> {

    @NonNull B responseCode(@NonNull HttpResponseCode responseCode);

    @NonNull B notFound();

    @NonNull B noContent();

    @NonNull B badRequest();

    @NonNull B forbidden();

    @NonNull B header(@NonNull String name, String... values);

    @NonNull B headers(@NonNull Map<String, List<String>> headers);

    @NonNull B modifyHeaders(@NonNull Consumer<Map<String, List<String>>> headerModifier);

    @NonNull B eTag(@NonNull String etag);

    @NonNull B lastModified(@NonNull ZonedDateTime lastModified);

    @NonNull B lastModified(@NonNull Instant lastModified);

    @NonNull B location(@NonNull URI location);

    @NonNull B contentType(@NonNull String contentType);

    @NonNull B contentLength(long contentLength);

    @NonNull B body(@Nullable T body);

    @NonNull Response<T> build();
  }
}
