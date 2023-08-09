package eu.cloudnetservice.ext.rest.http.response;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class JsonResponse extends DefaultResponse<Object> {

  private JsonResponse(
    @Nullable Object body,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, List<String>> headers
  ) {
    super(body, responseCode, headers);
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull Response<Object> response) {
    return builder().responseCode(response.responseCode()).headers(response.headers()).body(response.body());
  }

  @Override
  protected void serializeBody(@NonNull HttpResponse response) {
    var bodyDocument = DocumentFactory.json().newDocument(this.body);
    response.body(bodyDocument.serializeToString(StandardSerialisationStyle.COMPACT));
  }

  public static final class Builder extends DefaultResponseBuilder<Object, Builder> {

    private Builder() {
    }

    @Override
    public @NonNull Response<Object> build() {
      return new JsonResponse(this.body, this.responseCode, Map.copyOf(this.httpHeaders));
    }
  }
}
