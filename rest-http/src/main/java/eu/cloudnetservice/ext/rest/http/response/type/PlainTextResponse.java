package eu.cloudnetservice.ext.rest.http.response.type;

import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.http.response.Response;
import io.netty5.handler.codec.http.HttpHeaderNames;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class PlainTextResponse extends DefaultResponse<String> {

  private PlainTextResponse(
    @Nullable String body,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, List<String>> headers
  ) {
    super(body, responseCode, headers);
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull Response<String> response) {
    return builder().responseCode(response.responseCode()).headers(response.headers()).body(response.body());
  }

  @Override
  protected void serializeBody(@NonNull HttpResponse response, @NonNull String body) {
    response.body(body);
  }

  public static final class Builder extends DefaultResponseBuilder<String, Builder> {

    private Builder() {
    }

    @Override
    public @NonNull Response<String> build() {
      this.httpHeaders.putIfAbsent(
        HttpHeaderNames.CONTENT_TYPE.toString(),
        List.of(MediaType.PLAIN_TEXT_UTF_8.toString()));

      return new PlainTextResponse(this.body, this.responseCode, Map.copyOf(this.httpHeaders));
    }
  }
}
