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

package eu.cloudnetservice.ext.modules.rest.v2;

import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class V2HttpHandlerAuthorization {

  private final AuthProvider authProvider;

  public V2HttpHandlerAuthorization() {
    this.authProvider = AuthProviderLoader.resolveAuthProvider("jwt");
  }

  @RequestHandler(path = "/api/v2/auth")
  @Authentication(providers = "basic")
  public @NonNull IntoResponse<?> handleBasicAuthLogin(@NonNull RestUser user) {
    var token = this.authProvider.generateAuthToken(user);
  }
}
