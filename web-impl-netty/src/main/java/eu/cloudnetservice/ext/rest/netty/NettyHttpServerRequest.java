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

package eu.cloudnetservice.ext.rest.netty;

import com.google.common.net.HttpHeaders;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpCookie;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.HttpVersion;
import io.netty5.buffer.BufferInputStream;
import io.netty5.handler.codec.http.FullHttpRequest;
import io.netty5.handler.codec.http.QueryStringDecoder;
import io.netty5.handler.codec.http.headers.DefaultHttpCookiePair;
import io.netty5.handler.codec.http.headers.HttpCookiePair;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of a http request.
 *
 * @since 1.0
 */
final class NettyHttpServerRequest extends NettyHttpMessage implements HttpRequest {

  private final NettyHttpServerContext context;

  private final URI uri;
  private final io.netty5.handler.codec.http.HttpRequest httpRequest;

  private final Map<String, String> pathParameters;
  private final Map<String, List<String>> queryParameters;

  private byte[] body;

  /**
   * Constructs a new netty http request instance.
   *
   * @param context        the context in which the request is processed.
   * @param httpRequest    the original netty request which gets wrapped.
   * @param pathParameters the extracted path parameters from the uri.
   * @param uri            the original uri of the request.
   * @throws NullPointerException if one of the given properties is null.
   */
  public NettyHttpServerRequest(
    @NonNull NettyHttpServerContext context,
    @NonNull io.netty5.handler.codec.http.HttpRequest httpRequest,
    @NonNull Map<String, String> pathParameters,
    @NonNull URI uri
  ) {
    this.context = context;
    this.httpRequest = httpRequest;
    this.uri = uri;
    this.pathParameters = pathParameters;
    this.queryParameters = new QueryStringDecoder(httpRequest.uri()).parameters();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, String> pathParameters() {
    return this.pathParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String path() {
    return this.uri.getPath();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String uri() {
    return this.httpRequest.uri();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String method() {
    return this.httpRequest.method().name();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, List<String>> queryParameters() {
    return this.queryParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext context() {
    return this.context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable String header(@NonNull String name) {
    var headerValue = this.httpRequest.headers().get(name);
    return headerValue == null ? null : headerValue.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Integer headerAsInt(@NonNull String name) {
    var headerValue = this.header(name);
    return headerValue == null ? null : Ints.tryParse(headerValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean headerAsBoolean(@NonNull String name) {
    var headerValue = this.header(name);
    return Boolean.parseBoolean(headerValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest header(@NonNull String name, @NonNull String value) {
    this.httpRequest.headers().set(name, value);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest header(@NonNull String name, @NonNull Collection<String> values) {
    this.httpRequest.headers().set(name, values);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest addHeader(@NonNull String name, @NonNull String value) {
    this.httpRequest.headers().add(name, value);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest removeHeader(@NonNull String name) {
    this.httpRequest.headers().remove(name);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest clearHeaders() {
    this.httpRequest.headers().clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasHeader(@NonNull String name) {
    return this.httpRequest.headers().contains(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, String> headers() {
    // init the target map to copy all headers into - that target map is immune to later resizes
    var headers = this.httpRequest.headers();
    Map<String, String> headerMap = new HashMap<>(headers.size() + 1, 1f);

    // copy over all headers
    for (var entry : headers) {
      headerMap.put(entry.getKey().toString(), entry.getValue().toString());
    }

    return headerMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpVersion version() {
    return super.versionFromNetty(this.httpRequest.protocolVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest version(@NonNull HttpVersion version) {
    this.httpRequest.setProtocolVersion(super.versionToNetty(version));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] body() {
    if (this.httpRequest instanceof FullHttpRequest request) {
      if (this.body == null) {
        // initialize the body
        var length = request.payload().readableBytes();
        this.body = new byte[length];

        // copy out the bytes of the buffer
        request.payload().copyInto(request.payload().readerOffset(), this.body, 0, length);
      }

      return this.body;
    }

    return new byte[0];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String bodyAsString() {
    return new String(this.body(), StandardCharsets.UTF_8);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest body(byte[] byteArray) {
    throw new UnsupportedOperationException("Unable to set body in request");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest body(@NonNull String text) {
    return this.body(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InputStream bodyStream() {
    if (this.httpRequest instanceof FullHttpRequest fullHttpRequest) {
      return new BufferInputStream(fullHttpRequest.payload().send());
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest body(@Nullable InputStream body) {
    throw new UnsupportedOperationException("Unable to set body in request");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasBody() {
    return this.httpRequest instanceof FullHttpRequest request && request.payload().readableBytes() > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable HttpCookie cookie(@NonNull String name) {
    var cookie = this.httpRequest.headers().getCookie(name);
    return cookie == null ? null : this.convertFromNettyCookiePair(cookie);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<HttpCookie> cookies() {
    List<HttpCookie> cookies = new ArrayList<>();
    this.httpRequest.headers().getCookies().forEach(cookie -> {
      var convertedCookie = this.convertFromNettyCookiePair(cookie);
      cookies.add(convertedCookie);
    });
    return cookies;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasCookie(@NonNull String name) {
    return this.httpRequest.headers().getCookie(name) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest cookies(@NonNull Collection<HttpCookie> cookies) {
    this.httpRequest.headers().remove(HttpHeaders.COOKIE);
    cookies.forEach(this::addCookie);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest addCookie(@NonNull HttpCookie httpCookie) {
    var convertedCookie = this.convertToNettyCookiePair(httpCookie);
    this.httpRequest.headers().addCookie(convertedCookie);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest removeCookie(@NonNull String name) {
    this.httpRequest.headers().removeCookies(name);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest clearCookies() {
    this.httpRequest.headers().remove(HttpHeaders.COOKIE);
    return this;
  }

  /**
   * Converts the netty cookie pair to a {@link HttpCookie}.
   *
   * @param cookie the cookie to convert.
   * @return the converted cookie.
   * @throws NullPointerException if the given cookie is null.
   */
  private @NonNull HttpCookie convertFromNettyCookiePair(@NonNull HttpCookiePair cookie) {
    return new HttpCookie(
      cookie.name().toString(),
      cookie.value().toString(),
      null,
      null,
      false,
      false,
      cookie.isWrapped(),
      Long.MAX_VALUE);
  }

  /**
   * Converts the given {@link HttpCookie} to a netty cookie pair.
   *
   * @param cookie the cookie to convert.
   * @return the converted cookie.
   * @throws NullPointerException if the given cookie is null.
   */
  private @NonNull HttpCookiePair convertToNettyCookiePair(@NonNull HttpCookie cookie) {
    return new DefaultHttpCookiePair(cookie.name(), cookie.value(), cookie.wrap());
  }
}
