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

package eu.cloudnetservice.ext.rest.http.codec;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;

public final class CodecProvider {

  private static final Map<Class<?>, Object> CODEC_RESOLVE_CACHE = new ConcurrentHashMap<>(16, 0.9f, 1);

  private CodecProvider() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  public static @NonNull <T extends DataformatCodec> T resolveCodec(@NonNull Class<T> type) {
    return (T) CODEC_RESOLVE_CACHE.computeIfAbsent(
      type,
      $ -> ServiceLoader.load(type)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing codec implementation for: " + type.getSimpleName())));
  }
}
