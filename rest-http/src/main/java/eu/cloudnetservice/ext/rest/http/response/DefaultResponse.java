package eu.cloudnetservice.ext.rest.http.response;

import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public abstract class DefaultResponse<T> implements Response<T> {

  protected final T body;
  protected final HttpResponseCode responseCode;
  protected final Map<String, List<String>> headers;

  protected DefaultResponse(
    @Nullable T body,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, List<String>> headers
  ) {
    this.body = body;
    this.responseCode = responseCode;
    this.headers = headers;
  }

  @Override
  public @Nullable T body() {
    return this.body;
  }

  @Override
  public @NonNull HttpResponseCode responseCode() {
    return this.responseCode;
  }

  @Override
  public @Unmodifiable @NonNull Map<String, List<String>> headers() {
    return this.headers;
  }

  @Override
  public void serializeIntoResponse(@NonNull HttpResponse response) {
    response.status(this.responseCode);

    // response.header()
    if (this.body != null) {
      this.serializeBody(response, this.body);
    }
  }

  @Override
  public @NonNull Response<T> into() {
    return this;
  }

  protected abstract void serializeBody(@NonNull HttpResponse response, @NonNull T body);
}
