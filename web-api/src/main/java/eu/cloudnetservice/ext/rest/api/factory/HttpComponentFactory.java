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

import eu.cloudnetservice.ext.rest.api.HttpComponent;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import lombok.NonNull;

public interface HttpComponentFactory<T extends HttpComponent<T>> {

  @NonNull String componentTypeName();

  @NonNull Class<T> supportedComponentType();

  @NonNull T construct(@NonNull ComponentConfig componentConfig);
}
