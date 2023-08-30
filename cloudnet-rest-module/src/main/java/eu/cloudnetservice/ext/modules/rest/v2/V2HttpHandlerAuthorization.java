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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagementLoader;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.jwt.JwtAuthProvider;
import eu.cloudnetservice.ext.rest.jwt.JwtAuthToken;
import jakarta.inject.Singleton;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import lombok.NonNull;

@Singleton
public final class V2HttpHandlerAuthorization {

  private final JwtAuthProvider authProvider;
  private final RestUserManagement management;

  public V2HttpHandlerAuthorization() throws NoSuchAlgorithmException {
    // TODO from file
    var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    this.authProvider = new JwtAuthProvider(
      "CloudNet Rest",
      keyPair.getPrivate(),
      keyPair.getPublic(),
      Duration.ofHours(12),
      Duration.ofDays(3));
    this.management = RestUserManagementLoader.load();
  }

  @RequestHandler(path = "/api/v2/auth")
  @Authentication(providers = "basic")
  public @NonNull IntoResponse<?> handleBasicAuthLoginRequest(@NonNull RestUser user) {
    var token = (JwtAuthToken) this.authProvider.generateAuthToken(this.management, user);
    return JsonResponse.builder().body(Document.newJsonDocument().appendTree(token).append("userId", user));
  }

  @RequestHandler(path = "/api/v2/auth/refresh", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleRefreshRequest(
    @NonNull HttpContext context,
    @NonNull @RequestTypedBody Map<String, String> body
  ) {
    var token = body.get("token");
    if (token == null) {
      return ProblemDetail.builder()
        .type("refresh-token-missing")
        .title("Refresh Token Missing")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The request body does not contain a 'token' field with the refresh token.");
    }

    var user = this.authProvider.tryAuthenticate(context, this.management);
  }
}
