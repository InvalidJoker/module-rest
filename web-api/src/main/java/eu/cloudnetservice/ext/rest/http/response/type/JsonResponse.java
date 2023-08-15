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

import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.http.HttpResponse;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import eu.cloudnetservice.ext.rest.http.codec.CodecProvider;
import eu.cloudnetservice.ext.rest.http.codec.builtin.JsonCodec;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.http.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.http.response.Response;
import io.netty5.handler.codec.http.HttpHeaderNames;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class JsonResponse<T> extends DefaultResponse<T> {

  private JsonResponse(
    @Nullable T body,
    @NonNull HttpResponseCode responseCode,
    @NonNull Map<String, List<String>> headers
  ) {
    super(body, responseCode, headers);
  }

  public static <T> @NonNull Builder<T> builder() {
    return new Builder<>();
  }

  public static <T> @NonNull Builder<T> builder(@NonNull Response<? extends T> response) {
    return JsonResponse.<T>builder()
      .responseCode(response.responseCode())
      .headers(response.headers())
      .body(response.body());
  }

  @Override
  protected void serializeBody(@NonNull HttpResponse response, @NonNull T body) {
    var codec = CodecProvider.resolveCodec(JsonCodec.class);
    response.body(codec.serialize(body.getClass(), body));
  }

  @Override
  public @NonNull Response.Builder<T, ?> intoResponseBuilder() {
    return JsonResponse.builder(this);
  }

  public static final class Builder<T> extends DefaultResponseBuilder<T, Builder<T>> {

    private Builder() {
    }

    @Override
    public @NonNull Response<T> build() {
      this.httpHeaders.putIfAbsent(HttpHeaderNames.CONTENT_TYPE.toString(), List.of(MediaType.JSON_UTF_8.toString()));
      return new JsonResponse<>(this.body, this.responseCode, Map.copyOf(this.httpHeaders));
    }
  }
}
