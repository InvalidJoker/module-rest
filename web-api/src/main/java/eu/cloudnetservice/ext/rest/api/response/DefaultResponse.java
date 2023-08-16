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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public abstract class DefaultResponse<T> implements Response<T> {

  protected final T body;
  protected final HttpHeaderMap httpHeaderMap;
  protected final HttpResponseCode responseCode;

  protected DefaultResponse(
    @Nullable T body,
    @NonNull HttpHeaderMap httpHeaderMap,
    @NonNull HttpResponseCode responseCode
  ) {
    this.body = body;
    this.httpHeaderMap = httpHeaderMap;
    this.responseCode = responseCode;
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
  public @Unmodifiable @NonNull HttpHeaderMap headers() {
    return this.httpHeaderMap;
  }

  @Override
  public void serializeIntoResponse(@NonNull HttpResponse response) {
    response.status(this.responseCode);
    response.headers().add(this.httpHeaderMap);

    if (this.body != null) {
      this.serializeBody(response, this.body);
    }
  }

  @Override
  public @NonNull Response<T> intoResponse() {
    return this;
  }

  protected abstract void serializeBody(@NonNull HttpResponse response, @NonNull T body);
}
