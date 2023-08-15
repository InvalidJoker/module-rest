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

package eu.cloudnetservice.ext.rest.http.response.type;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.http.response.Response;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class InputStreamResponse extends DefaultResponse<InputStream> {

  private InputStreamResponse(
    @Nullable InputStream body,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, List<String>> headers
  ) {
    super(body, responseCode, headers);
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull Response<InputStream> response) {
    return builder().responseCode(response.responseCode()).headers(response.headers()).body(response.body());
  }

  @Override
  protected void serializeBody(@NonNull HttpResponse response, @NonNull InputStream body) {
    response.body(body);
  }

  @Override
  public @NonNull Response.Builder<InputStream, ?> intoResponseBuilder() {
    return InputStreamResponse.builder(this);
  }

  public static final class Builder extends DefaultResponseBuilder<InputStream, Builder> {

    private Builder() {
    }

    @Override
    public @NonNull Response<InputStream> build() {
      this.httpHeaders.putIfAbsent(HttpHeaders.CONTENT_TYPE, List.of(MediaType.OCTET_STREAM.toString()));
      return new InputStreamResponse(this.body, this.responseCode, Map.copyOf(this.httpHeaders));
    }
  }
}
