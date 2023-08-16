/*
 * Copyright 2019-2023 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.ext.rest.api.response.type;

import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.api.response.Response;
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

        this.httpHeaders.putIfAbsent(HttpHeaders.CONTENT_DISPOSITION, List.of(attachment));
        this.httpHeaders.putIfAbsent(
          HttpHeaders.CONTENT_TYPE,
          List.of(MediaType.OCTET_STREAM.toString()));
      }

      return new FileResponse(this.body, this.responseCode, Map.copyOf(this.httpHeaders));
    }
  }
}
