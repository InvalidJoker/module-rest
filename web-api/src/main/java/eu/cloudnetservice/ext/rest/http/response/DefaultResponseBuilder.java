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

package eu.cloudnetservice.ext.rest.http.response;

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.http.HttpResponseCode;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultResponseBuilder<T, B extends Response.Builder<T, B>> implements Response.Builder<T, B> {

  // RFC 9110 Section 8.8.2
  // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified#syntax
  private static final ZoneId GMT = ZoneId.of("GMT");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    .withZone(GMT);

  protected T body;
  protected HttpResponseCode responseCode = HttpResponseCode.OK;
  protected Map<String, List<String>> httpHeaders = new HashMap<>();

  @Override
  public @NonNull B responseCode(@NonNull HttpResponseCode responseCode) {
    this.responseCode = responseCode;
    return this.self();
  }

  @Override
  public @NonNull B notFound() {
    return this.responseCode(HttpResponseCode.NOT_FOUND);
  }

  @Override
  public @NonNull B noContent() {
    return this.responseCode(HttpResponseCode.NO_CONTENT);
  }

  @Override
  public @NonNull B badRequest() {
    return this.responseCode(HttpResponseCode.BAD_REQUEST);
  }

  @Override
  public @NonNull B forbidden() {
    return this.responseCode(HttpResponseCode.FORBIDDEN);
  }

  @Override
  public @NonNull B header(@NonNull String name, String... values) {
    this.httpHeaders.computeIfAbsent(name, __ -> new ArrayList<>(values.length)).addAll(List.of(values));
    return this.self();
  }

  @Override
  public @NonNull B headers(@NonNull Map<String, List<String>> headers) {
    this.httpHeaders = new HashMap<>(headers);
    return this.self();
  }

  @Override
  public @NonNull B modifyHeaders(@NonNull Consumer<Map<String, List<String>>> headerModifier) {
    headerModifier.accept(this.httpHeaders);
    return this.self();
  }

  @Override
  public @NonNull B eTag(@NonNull String etag) {
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
    if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
      etag = "\"" + etag;
    }
    if (!etag.endsWith("\"")) {
      etag += "\"";
    }

    return this.header(HttpHeaders.ETAG, etag);
  }

  @Override
  public @NonNull B lastModified(@NonNull ZonedDateTime lastModified) {
    return this.header(
      HttpHeaders.LAST_MODIFIED,
      DATE_FORMATTER.format(lastModified.withZoneSameInstant(GMT)));
  }

  @Override
  public @NonNull B lastModified(@NonNull Instant lastModified) {
    return this.lastModified(ZonedDateTime.ofInstant(lastModified, GMT));
  }

  @Override
  public @NonNull B location(@NonNull URI location) {
    return this.header(HttpHeaders.LOCATION, location.toASCIIString());
  }

  @Override
  public @NonNull B contentType(@NonNull String contentType) {
    return this.header(HttpHeaders.CONTENT_TYPE, contentType);
  }

  @Override
  public @NonNull B contentLength(long contentLength) {
    return this.header(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
  }

  @Override
  public @NonNull B body(@Nullable T body) {
    this.body = body;
    return this.self();
  }

  @Override
  public @NonNull Response.Builder<T, ?> intoResponseBuilder() {
    return this;
  }

  private @NonNull B self() {
    //noinspection unchecked
    return (B) this;
  }
}
