package eu.cloudnetservice.ext.rest.http.response.type;

import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.http.response.Response;
import io.netty5.handler.codec.http.HttpHeaderNames;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class FileResponse extends DefaultResponse<Path> {

  private FileResponse(
    @Nullable Path body,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, List<String>> headers
  ) {
    super(body, responseCode, headers);
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull Response<Path> response) {
    return builder().responseCode(response.responseCode()).headers(response.headers()).body(response.body());
  }

  @Override
  protected void serializeBody(@NonNull HttpResponse response, @NonNull Path body) {
    try {
      response.body(Files.newInputStream(body, StandardOpenOption.READ));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  public @NonNull Response.Builder<Path, ?> intoResponseBuilder() {
    return FileResponse.builder(this);
  }

  public static final class Builder extends DefaultResponseBuilder<Path, Builder> {

    private Builder() {
    }

    @Override
    public @NonNull Response<Path> build() {
      if (this.body != null) {
        Preconditions.checkArgument(Files.exists(this.body), "File %s does not exist.", this.body);

        var fileName = this.body.getFileName();
        var attachment = String.format("attachment%s", fileName == null ? "" : "; filename=" + fileName);

        this.httpHeaders.putIfAbsent(HttpHeaderNames.CONTENT_DISPOSITION.toString(), List.of(attachment));
        this.httpHeaders.putIfAbsent(
          HttpHeaderNames.CONTENT_TYPE.toString(),
          List.of(MediaType.OCTET_STREAM.toString()));
      }

      return new FileResponse(this.body, this.responseCode, Map.copyOf(this.httpHeaders));
    }
  }
}
