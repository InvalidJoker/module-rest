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

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagementLoader;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.jwt.JwtAuthProvider;
import eu.cloudnetservice.ext.rest.jwt.JwtAuthToken;
import eu.cloudnetservice.ext.rest.jwt.JwtTokenPropertyParser;
import jakarta.inject.Singleton;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import lombok.NonNull;

@Singleton
public final class V2HttpHandlerAuthorization {

  private final JwtAuthProvider authProvider;
  private final RestUserManagement management;

  public V2HttpHandlerAuthorization() {
    // TODO from file
    KeyPair keyPair;
    try {
      keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    this.authProvider = new JwtAuthProvider(
      "CloudNet Rest",
      keyPair.getPrivate(),
      keyPair.getPublic(),
      Duration.ofHours(12),
      Duration.ofDays(3));
    AuthProviderLoader.installAuthProvider(this.authProvider);

    this.management = RestUserManagementLoader.load();
  }

  @RequestHandler(path = "/api/v2/auth")
  public @NonNull IntoResponse<?> handleBasicAuthLoginRequest(
    @Authentication(providers = "basic") @NonNull RestUser user
  ) {
    var token = (JwtAuthToken) this.authProvider.generateAuthToken(this.management, user);
    return token.intoResponseBuilder();
  }

  @RequestHandler(path = "/api/v2/auth/refresh", method = HttpMethod.POST)
  public @NonNull IntoResponse<?> handleRefreshRequest(@NonNull HttpContext context) {
    var authenticationResult = this.authProvider.tryAuthenticate(context, this.management);
    if (authenticationResult instanceof AuthenticationResult.Success) {
      return ProblemDetail.builder()
        .type("refresh-access-token-used")
        .title("Refresh Access Token Used")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The refresh request requires a refresh token instead of an access token.");
    }

    if (authenticationResult instanceof AuthenticationResult.InvalidTokenType refreshResult) {
      var user = refreshResult.restUser();
      // we want to remove the token that was passed in the request as a refresh is requested
      var parsedTokens = JwtTokenPropertyParser.parseTokens(user.properties().get(JwtAuthProvider.JWT_TOKEN_PAIR_KEY))
        .stream()
        .filter(holder -> !holder.tokenId().equals(refreshResult.tokenId()))
        .toList();

      // convert the tokens back to
      var compactTokens = JwtTokenPropertyParser.compactTokens(parsedTokens);
      user = this.management.builder(user).modifyProperties(properties -> {
        if (compactTokens == null) {
          // no tokens left, just remove the property
          properties.remove(JwtAuthProvider.JWT_TOKEN_PAIR_KEY);
        } else {
          // update the property
          properties.put(JwtAuthProvider.JWT_TOKEN_PAIR_KEY, compactTokens);
        }
      }).build();

      var token = this.authProvider.generateAuthToken(this.management, user);
      return token.intoResponseBuilder();
    } else {
      return ProblemDetail.builder()
        .type("refresh-invalid-token")
        .title("Refresh Invalid Token")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("The provided refresh token is invalid.");
    }
  }
}
