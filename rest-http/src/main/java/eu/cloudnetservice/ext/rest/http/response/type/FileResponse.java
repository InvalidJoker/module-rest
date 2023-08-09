package eu.cloudnetservice.ext.rest.http.response.type;

import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.http.response.Response;
import java.io.IOException;
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
      // todo: headers with attachment & last modified & content type
      // also this should probably throw in case the file does not exist?
      response.body(Files.newInputStream(body, StandardOpenOption.READ));
    } catch (IOException exception) {
      // todo: what to do with this?
    }
  }

  public static final class Builder extends DefaultResponseBuilder<Path, Builder> {

    private Builder() {
    }

    @Override
    public @NonNull Response<Path> build() {
      // todo: throw exception in case the file does not exist?
      return new FileResponse(this.body, this.responseCode, Map.copyOf(this.httpHeaders));
    }
  }
}
