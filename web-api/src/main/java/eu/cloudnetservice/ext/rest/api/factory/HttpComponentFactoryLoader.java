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

package eu.cloudnetservice.ext.rest.api.factory;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.ext.rest.api.HttpComponent;
import java.util.Collection;
import java.util.Objects;
import java.util.ServiceLoader;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class HttpComponentFactoryLoader {

  public static @NonNull Collection<? extends HttpComponentFactory<?>> loadComponentFactories() {
    var serviceLoader = ServiceLoader.load(HttpComponentFactory.class);
    return serviceLoader.stream()
      .map(ServiceLoader.Provider::get)
      .map(factory -> (HttpComponentFactory<?>) factory)
      .toList();
  }

  @SuppressWarnings("unchecked")
  public static @NonNull <T extends HttpComponent<T>> Collection<HttpComponentFactory<T>> loadComponentFactories(
    @NonNull Class<T> componentType
  ) {
    var serviceLoader = ServiceLoader.load(HttpComponentFactory.class);
    return serviceLoader.stream()
      .map(ServiceLoader.Provider::get)
      .filter(factory -> factory.supportedComponentType() == componentType)
      .map(factory -> (HttpComponentFactory<T>) factory)
      .toList();
  }

  public static @Nullable <T extends HttpComponent<T>> HttpComponentFactory<T> findFirstComponentFactory(
    @NonNull Class<T> componentType
  ) {
    var componentFactories = loadComponentFactories(componentType);
    return Iterables.getFirst(componentFactories, null);
  }

  public static @NonNull <T extends HttpComponent<T>> HttpComponentFactory<T> getFirstComponentFactory(
    @NonNull Class<T> componentType
  ) {
    return Objects.requireNonNull(
      findFirstComponentFactory(componentType),
      "No component factory for " + componentType);
  }

  private HttpComponentFactoryLoader() {
    throw new UnsupportedOperationException();
  }
}
