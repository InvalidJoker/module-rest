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

package eu.cloudnetservice.ext.rest.api.response;

import eu.cloudnetservice.ext.rest.api.HttpResponse;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.header.HttpHeaderMap;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface Response<T> extends IntoResponse<T> {

  @Nullable T body();

  @NonNull HttpResponseCode responseCode();

  @Unmodifiable
  @NonNull HttpHeaderMap headers();

  void serializeIntoResponse(@NonNull HttpResponse response);

  interface Builder<T, B extends Builder<T, B>> extends IntoResponse<T> {

    @NonNull B responseCode(@NonNull HttpResponseCode responseCode);

    @NonNull B notFound();

    @NonNull B noContent();

    @NonNull B badRequest();

    @NonNull B forbidden();

    @NonNull B header(@NonNull HttpHeaderMap httpHeaderMap);

    @NonNull B header(@NonNull String name, String... values);

    @NonNull B modifyHeaders(@NonNull Consumer<HttpHeaderMap> headerModifier);

    @NonNull B eTag(@NonNull String etag);

    @NonNull B lastModified(@NonNull ZonedDateTime lastModified);

    @NonNull B lastModified(@NonNull Instant lastModified);

    @NonNull B location(@NonNull URI location);

    @NonNull B contentType(@NonNull String contentType);

    @NonNull B contentLength(long contentLength);

    @NonNull B body(@Nullable T body);

    @NonNull Response<T> build();
  }
}
