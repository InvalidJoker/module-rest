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

package eu.cloudnetservice.ext.rest.api.header;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class UnmodifiableHttpHeaderMap implements HttpHeaderMap {

  private final HttpHeaderMap delegate;

  public UnmodifiableHttpHeaderMap(@NonNull HttpHeaderMap delegate) {
    this.delegate = delegate;
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public @NonNull HttpHeaderMap clone() {
    return this;
  }

  @Override
  public @NonNull HttpHeaderMap unmodifiableClone() {
    return this;
  }

  @Override
  public int size() {
    return this.delegate.size();
  }

  @Override
  public boolean contains(@NonNull String headerName) {
    return this.delegate.contains(headerName);
  }

  @Override
  public boolean contains(@NonNull String headerName, @NonNull String headerValue) {
    return this.delegate.contains(headerName, headerValue);
  }

  @Override
  public @Nullable String firstValue(@NonNull String headerName) {
    return this.delegate.firstValue(headerName);
  }

  @Override
  public @UnknownNullability String firstValue(@NonNull String headerName, @Nullable String defaultValue) {
    return this.delegate.firstValue(headerName, defaultValue);
  }

  @Override
  public @NonNull Collection<String> values(@NonNull String headerName) {
    return this.delegate.values(headerName);
  }

  @Override
  public @NonNull Set<String> names() {
    return this.delegate.names();
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull HttpHeaderMap headerMap) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull Map<String, ? extends Iterable<String>> headers) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String headerValue) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String... headerValues) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull HttpHeaderMap headerMap) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull Map<String, ? extends Iterable<String>> headers) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String headerValue) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String... headerValues) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String headerValue) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String... headerValues) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull Iterable<String> headerValues) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap clear() {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap remove(@NonNull String headerName) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull HttpHeaderMap remove(@NonNull String headerName, @NonNull String headerValue) {
    throw new UnsupportedOperationException("this header map is unmodifiable");
  }

  @Override
  public @NonNull Stream<Map.Entry<String, String>> stream() {
    return this.delegate.stream();
  }

  @Override
  public @NonNull Collection<Map.Entry<String, String>> entries() {
    return this.delegate.entries();
  }

  @Override
  public @NonNull Map<String, ? extends Collection<String>> asMap() {
    return this.delegate.asMap();
  }

  @Override
  public @NonNull Iterator<Map.Entry<String, String>> iterator() {
    return this.delegate.iterator();
  }

  @Override
  public @NonNull Spliterator<Map.Entry<String, String>> spliterator() {
    return this.delegate.spliterator();
  }
}
