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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public interface HttpHeaderMap extends Iterable<Map.Entry<String, String>>, Cloneable {

  static @NonNull HttpHeaderMap newHeaderMap() {
    return newHeaderMap(16);
  }

  static @NonNull HttpHeaderMap newHeaderMap(int sizeHint) {
    return new HashHttpHeaderMap(sizeHint);
  }

  @NonNull HttpHeaderMap clone();

  @NonNull HttpHeaderMap unmodifiableClone();

  int size();

  boolean contains(@NonNull String headerName);

  boolean contains(@NonNull String headerName, @NonNull String headerValue);

  @Nullable String firstValue(@NonNull String headerName);

  @UnknownNullability String firstValue(@NonNull String headerName, @Nullable String defaultValue);

  @NonNull Collection<String> values(@NonNull String headerName);

  @NonNull Set<String> names();

  @NonNull HttpHeaderMap add(@NonNull HttpHeaderMap headerMap);

  @NonNull HttpHeaderMap add(@NonNull Map<String, ? extends Iterable<String>> headers);

  @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String headerValue);

  @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull String... headerValues);

  @NonNull HttpHeaderMap add(@NonNull String headerName, @NonNull Iterable<String> headerValues);

  @NonNull HttpHeaderMap set(@NonNull HttpHeaderMap headerMap);

  @NonNull HttpHeaderMap set(@NonNull Map<String, ? extends Iterable<String>> headers);

  @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String headerValue);

  @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull String... headerValues);

  @NonNull HttpHeaderMap set(@NonNull String headerName, @NonNull Iterable<String> headerValues);

  @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String headerValue);

  @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull String... headerValues);

  @NonNull HttpHeaderMap setIfAbsent(@NonNull String headerName, @NonNull Iterable<String> headerValues);

  @NonNull HttpHeaderMap clear();

  @NonNull HttpHeaderMap remove(@NonNull String headerName);

  @NonNull HttpHeaderMap remove(@NonNull String headerName, @NonNull String headerValue);

  @NonNull Stream<Map.Entry<String, String>> stream();

  @NonNull Collection<Map.Entry<String, String>> entries();

  @NonNull Map<String, ? extends Collection<String>> asMap();
}
