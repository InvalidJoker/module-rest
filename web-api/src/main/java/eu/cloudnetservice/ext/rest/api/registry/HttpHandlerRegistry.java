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

package eu.cloudnetservice.ext.rest.api.registry;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.tree.HttpHandlerTree;
import eu.cloudnetservice.ext.rest.api.tree.HttpPathNode;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface HttpHandlerRegistry {

  @Unmodifiable
  @NonNull Collection<HttpHandler> registeredHandlers();

  @Nullable HttpHandlerTree<HttpPathNode> findHandler(@NonNull String path, @NonNull HttpContext context);

  void registerHandler(
    @NonNull String path,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config);

  void unregisterHandler(@NonNull HttpHandler handler);

  void unregisterHandlers(@NonNull ClassLoader classLoader);

  void clearHandlers();

}
