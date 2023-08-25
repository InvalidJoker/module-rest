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

package eu.cloudnetservice.ext.rest.api.auth;

import java.util.ServiceLoader;
import lombok.NonNull;

public final class RestUserManagementLoader {

  private static RestUserManagement USER_MANAGEMENT;

  public static @NonNull RestUserManagement load() {
    if (USER_MANAGEMENT != null) {
      return USER_MANAGEMENT;
    }

    return USER_MANAGEMENT = ServiceLoader.load(RestUserManagement.class, RestUserManagement.class.getClassLoader())
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Missing implementation for rest-user management."));
  }
}
