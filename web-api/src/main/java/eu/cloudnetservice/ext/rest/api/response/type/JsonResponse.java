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

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.codec.CodecProvider;
import eu.cloudnetservice.ext.rest.api.codec.builtin.JsonCodec;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponse;
import eu.cloudnetservice.ext.rest.api.response.DefaultResponseBuilder;
import eu.cloudnetservice.ext.rest.api.response.Response;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class JsonResponse<T> extends DefaultResponse<T> {

  private JsonResponse(
    @Nullable T body,
    @NonNull HttpHeaderMap httpHeaderMap,
    @NonNull HttpResponseCode responseCode
  ) {
    super(body, httpHeaderMap, responseCode);
  }

  public static <T> @NonNull Builder<T> builder() {
    return new Builder<>();
  }

  public static <T> @NonNull Builder<T> builder(@NonNull Response<? extends T> response) {
    return JsonResponse.<T>builder()
      .responseCode(response.responseCode())
      .header(response.headers())
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
      this.httpHeaderMap.setIfAbsent(HttpHeaders.CONTENT_TYPE, List.of(MediaType.JSON_UTF_8.toString()));
      return new JsonResponse<>(this.body, this.httpHeaderMap.unmodifiableClone(), this.responseCode);
    }
  }
}
