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

package eu.cloudnetservice.ext.rest.http;

import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import java.util.Collection;
import lombok.NonNull;

/**
 * Represents any http component, providing an abstract layer for registering listeners to it.
 *
 * @param <T> the generic type of the component implementing this class.
 * @see HttpServer
 * @since 4.0
 */
public interface HttpComponent<T extends HttpComponent<T>> extends AutoCloseable {

  /**
   * Gets whether this component has ssl enabled or not.
   *
   * @return whether this component has ssl enabled or not.
   */
  boolean sslEnabled();

  /**
   * Get a http annotation parser which is associated with this component and can therefore be used to register
   * annotated handlers to this component.
   *
   * @return the associated http annotation parser instance.
   */
  @NonNull HttpAnnotationParser<T> annotationParser();

  /**
   * Registers the given handlers to this component. The given handlers will get called when a request matches the given
   * path. The path may contain the following placeholders:
   * <ul>
   *   <li>{@code *}: Represents a wildcard character which matches anything provided in the path.
   *   <li>{@code {name}}: Represents a path parameter you want to retrieve when handling the request.
   * </ul>
   *
   * @param path    the path to register the handler to.
   * @param handler the handler to register.
   * @param config the config for the handler.
   * @return the same component instance as used to call the method, for chaining.
   * @throws NullPointerException if either the given path or handler is null.
   */
  @NonNull T registerHandler(@NonNull String path, @NonNull HttpHandler handler, @NonNull HttpHandlerConfig config);

  /**
   * Unregisters the given handler from this component if it was registered previously.
   *
   * @param handler the handler to unregister.
   * @return the same component instance as used to call the method, for chaining.
   * @throws NullPointerException if the given handler is null.
   */
  @NonNull T removeHandler(@NonNull HttpHandler handler);

  /**
   * Removes all registered handlers from this component whose classes were loaded by the given class loader.
   *
   * @param classLoader the loader of the handler classes to unregister.
   * @return the same component instance as used to call the method, for chaining.
   * @throws NullPointerException if the given class loader is null.
   */
  @NonNull T removeHandler(@NonNull ClassLoader classLoader);

  /**
   * Get all handlers which were registered to this component.
   *
   * @return all handlers which were registered to this component.
   */
  @NonNull Collection<HttpHandler> httpHandlers();

  /**
   * Removes all handlers which were previously registered to this component.
   *
   * @return the same component instance as used to call the method, for chaining.
   */
  @NonNull T clearHandlers();
}
