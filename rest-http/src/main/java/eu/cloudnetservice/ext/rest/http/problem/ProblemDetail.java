package eu.cloudnetservice.ext.rest.http.problem;

import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.response.IntoResponse;
import eu.cloudnetservice.ext.rest.http.response.Response;
import eu.cloudnetservice.ext.rest.http.response.type.JsonResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * An implementation of the problem details response body described in <a
 * href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807</a>.
 */
public final class ProblemDetail implements IntoResponse<Map<String, Object>> {

  private static final String PROBLEM_JSON_CONTENT_TYPE = MediaType.create("application", "problem+json")
    .withCharset(StandardCharsets.UTF_8)
    .toString();

  private final URI type;
  private final String title;
  private final String detail;
  private final String instance;
  private final HttpResponseCode status;
  private final Map<String, Object> additionalFields;

  private ProblemDetail(
    @Nullable URI type,
    @Nullable String title,
    @Nullable String detail,
    @Nullable String instance,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, Object> additionalFields
  ) {
    this.type = type;
    this.title = title;
    this.detail = detail;
    this.instance = instance;
    this.status = responseCode;
    this.additionalFields = additionalFields;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ProblemDetail problemDetail) {
    return builder()
      .type(problemDetail.type())
      .title(problemDetail.title())
      .detail(problemDetail.detail())
      .status(problemDetail.status())
      .instance(problemDetail.instance())
      .additionalFields(problemDetail.additionalFields());
  }

  public @NonNull HttpResponseCode status() {
    return this.status;
  }

  public @Nullable URI type() {
    return this.type;
  }

  public @Nullable String title() {
    return this.title;
  }

  public @Nullable String detail() {
    return this.detail;
  }

  public @Nullable String instance() {
    return this.instance;
  }

  @Unmodifiable
  public @NonNull Map<String, Object> additionalFields() {
    return this.additionalFields;
  }

  @Override
  public @NonNull Response.Builder<Map<String, Object>, ?> intoResponseBuilder() {
    // serializes the members of the problem details body
    // see https://datatracker.ietf.org/doc/html/rfc7807#section-3.1
    Map<String, Object> serializedProblemDetail = new HashMap<>();
    this.registerFieldToSerialize(serializedProblemDetail, "type", this.type);
    this.registerFieldToSerialize(serializedProblemDetail, "title", this.title);
    this.registerFieldToSerialize(serializedProblemDetail, "status", this.status.code());
    this.registerFieldToSerialize(serializedProblemDetail, "detail", this.detail);
    this.registerFieldToSerialize(serializedProblemDetail, "instance", this.instance);
    serializedProblemDetail.putAll(this.additionalFields);

    return JsonResponse.<Map<String, Object>>builder()
      .responseCode(this.status)
      .contentType(PROBLEM_JSON_CONTENT_TYPE)
      .body(Map.copyOf(serializedProblemDetail));
  }

  private void registerFieldToSerialize(
    @NonNull Map<String, Object> properties,
    @NonNull String key,
    @Nullable Object value
  ) {
    if (value != null) {
      properties.put(key, value);
    }
  }

  public static final class Builder {

    private Map<String, Object> additionalFields = new HashMap<>();
    private HttpResponseCode status = HttpResponseCode.INTERNAL_SERVER_ERROR;

    private URI type;
    private String title;
    private String detail;
    private String instance;

    public @NonNull Builder status(@NonNull HttpResponseCode statusCode) {
      this.status = statusCode;
      return this;
    }

    public @NonNull Builder type(@Nullable URI type) {
      this.type = type;
      return this;
    }

    public @NonNull Builder title(@Nullable String title) {
      this.title = title;
      return this;
    }

    public @NonNull Builder detail(@Nullable String detail) {
      this.detail = detail;
      return this;
    }

    public @NonNull Builder instance(@Nullable String instance) {
      this.instance = instance;
      return this;
    }

    public @NonNull Builder addAdditionalFields(@NonNull String key, @NonNull Object value) {
      this.additionalFields.put(key, value);
      return this;
    }

    public @NonNull Builder additionalFields(@NonNull Map<String, Object> additionalFields) {
      this.additionalFields = new HashMap<>(additionalFields);
      return this;
    }

    public @NonNull Builder modifyAdditionalFields(@NonNull Consumer<Map<String, Object>> modifier) {
      modifier.accept(this.additionalFields);
      return this;
    }

    public @NonNull ProblemDetail build() {
      return new ProblemDetail(
        this.type,
        this.title,
        this.detail,
        this.instance,
        this.status,
        Map.copyOf(this.additionalFields));
    }
  }
}
