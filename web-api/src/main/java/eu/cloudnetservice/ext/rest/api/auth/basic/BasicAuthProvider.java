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

package eu.cloudnetservice.ext.rest.api.auth.basic;

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthToken;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;
import lombok.NonNull;

public class BasicAuthProvider implements AuthProvider<Void> {

  private static final byte BASIC_AUTH_DELIM_CHAR = 0xFF & ':';
  private static final Pattern BASIC_LOGIN_PATTERN = Pattern.compile("Basic ([a-zA-Z0-9-_=]+)$");

  @Override
  public int priority() {
    return 10;
  }

  @Override
  public boolean supportsTokenGeneration() {
    return false;
  }

  @Override
  public @NonNull String name() {
    return "basic";
  }

  @Override
  public @NonNull AuthToken<Void> generateAuthToken(@NonNull RestUserManagement management, @NonNull RestUser user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull AuthenticationResult tryAuthenticate(
    @NonNull HttpContext context,
    @NonNull RestUserManagement management
  ) {
    // check if the authorization header is present
    var authHeader = context.request().headers().firstValue(HttpHeaders.AUTHORIZATION);
    if (authHeader == null) {
      return AuthenticationResult.proceed();
    }

    // check if the authorization header is a basic auth value
    var basicAuthMatcher = BASIC_LOGIN_PATTERN.matcher(authHeader);
    if (!basicAuthMatcher.matches()) {
      return AuthenticationResult.proceed();
    }

    try {
      // decode the user-password value
      var decodedBasicValue = Base64.getUrlDecoder().decode(basicAuthMatcher.group(1));
      var basicAuthDelimiterIdx = this.findBasicDelimiter(decodedBasicValue);
      if (basicAuthDelimiterIdx == -1) {
        return AuthenticationResult.invalidCredentials();
      }

      // extract the username
      var usernameBytes = Arrays.copyOfRange(decodedBasicValue, 0, basicAuthDelimiterIdx);
      var username = new String(usernameBytes, StandardCharsets.UTF_8);
      var extractedUser = management.restUser(username);
      if (extractedUser == null) {
        return AuthenticationResult.userNotFound();
      }

      // get the password, validate it and erase the info
      var passwordBytes = Arrays.copyOfRange(decodedBasicValue, basicAuthDelimiterIdx + 1, decodedBasicValue.length);
      var suppliedValidPassword = this.validatePassword(extractedUser, passwordBytes);
      this.erasePasswordBytes(passwordBytes);
      if (suppliedValidPassword) {
        // valid user and password
        return AuthenticationResult.ok(extractedUser);
      } else {
        // invalid password
        return AuthenticationResult.invalidCredentials();
      }
    } catch (IllegalArgumentException exception) {
      // invalid base64
      return AuthenticationResult.invalidCredentials();
    }
  }

  private int findBasicDelimiter(byte[] basicAuthValue) {
    for (int index = 0; index < basicAuthValue.length; index++) {
      if (basicAuthValue[index] == BASIC_AUTH_DELIM_CHAR) {
        return index;
      }
    }
    return -1;
  }

  protected boolean validatePassword(@NonNull RestUser user, byte[] passwordBytes) {
    return true;
  }

  protected void erasePasswordBytes(byte[] passwordBytes) {
    Arrays.fill(passwordBytes, (byte) 0);
  }
}
