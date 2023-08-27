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

package eu.cloudnetservice.ext.rest.jwt;

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagementLoader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.NonNull;

public class JwtAuthProvider implements AuthProvider<Map<String, Object>> {

  public static final String JWT_TOKEN_PAIR_KEY = "jwt_token_pair";
  public static final Pattern JWT_TOKEN_PAIR_PATTERN =
    Pattern.compile("([0-9a-fA-F\\-]+);([0-9a-fA-F\\-]+)(,([0-9a-fA-F\\-]+);([0-9a-fA-F\\-]+))*");

  private static final Pattern BEARER_LOGIN_PATTERN = Pattern.compile("Bearer ([a-zA-Z\\d-_.]+)$");

  private final String issuer;
  private final KeyPair keyPair;
  private final JwtParser parser;
  private final Duration accessDuration;
  private final Duration refreshDuration;
  private final RestUserManagement restUserManagement;

  public JwtAuthProvider(
    @NonNull KeyPair keyPair,
    @NonNull String issuer,
    @NonNull Duration accessDuration,
    @NonNull Duration refreshDuration
  ) {
    this.issuer = issuer;
    this.keyPair = keyPair;
    this.accessDuration = accessDuration;
    this.refreshDuration = refreshDuration;
    this.parser = Jwts.parserBuilder()
      .setSigningKey(keyPair.getPrivate())
      .requireIssuer(issuer)
      .build();
    this.restUserManagement = RestUserManagementLoader.load();
  }

  @Override
  public boolean supportsTokenGeneration() {
    return true;
  }

  @Override
  public int priority() {
    return 10;
  }

  @Override
  public @NonNull String name() {
    return "jwt";
  }

  @Override
  public @NonNull AuthenticationResult tryAuthenticate(
    @NonNull HttpContext context,
    @NonNull RestUserManagement management
  ) {
    var authHeader = context.request().headers().firstValue(HttpHeaders.AUTHORIZATION);
    if (authHeader == null) {
      return AuthenticationResult.proceed();
    }

    var tokenMatcher = BEARER_LOGIN_PATTERN.matcher(authHeader);
    if (!tokenMatcher.matches()) {
      return AuthenticationResult.proceed();
    }

    try {
      var token = this.parser.parseClaimsJws(tokenMatcher.group(1));

      var subject = token.getBody().getSubject();
      var user = this.restUserManagement.restUser(subject);
      if (user == null) {
        return AuthenticationResult.userNotFound();
      }

      var tokenId = token.getBody().getId();
      var tokenPairs = user.properties().get(JWT_TOKEN_PAIR_KEY);
      if (tokenPairs == null || !tokenPairs.contains(tokenId + ';')) {
        return AuthenticationResult.invalidCredentials();
      }

      return AuthenticationResult.ok(user);
    } catch (JwtException exception) {
      return AuthenticationResult.invalidCredentials();
    }
  }

  @Override
  public @NonNull JwtAuthToken generateAuthToken(@NonNull RestUser user) {
    var accessTokenId = UUID.randomUUID().toString();
    var refreshTokenId = UUID.randomUUID().toString();

    var accessToken = this.generateAccessToken(user, accessTokenId);
    var refreshToken = this.generateRefreshToken(user, refreshTokenId);

    var updatedUser = this.restUserManagement.builder(user)
      .modifyProperties(properties -> properties.compute(JWT_TOKEN_PAIR_KEY, (__, value) -> {
        var token = accessTokenId + ';' + refreshTokenId;
        return value == null ? token : value + ',' + token;
      }))
      .build();
    this.restUserManagement.saveRestUser(updatedUser);

    return new JwtAuthToken(Instant.now(), accessToken, refreshToken);
  }

  private @NonNull JwtBuilder constructToken(@NonNull RestUser subject, @NonNull String id) {
    return Jwts.builder()
      .setIssuer(this.issuer)
      .signWith(this.keyPair.getPrivate())
      .setSubject(subject.id())
      .setIssuedAt(new Date())
      .setId(id);
  }

  private @NonNull JwtTokenHolder generateAccessToken(@NonNull RestUser subject, @NonNull String id) {
    var expiration = Date.from(Instant.now().plus(this.accessDuration));
    var token = this.constructToken(subject, id).setExpiration(expiration).compact();

    return new JwtTokenHolder(token, this.accessDuration.toMillis(), JwtTokenHolder.ACCESS_TOKEN_TYPE);
  }

  private @NonNull JwtTokenHolder generateRefreshToken(@NonNull RestUser subject, @NonNull String id) {
    var expiration = Date.from(Instant.now().plus(this.refreshDuration));
    var token = this.constructToken(subject, id).setExpiration(expiration).claim("refresh", true).compact();

    return new JwtTokenHolder(token, this.refreshDuration.toMillis(), JwtTokenHolder.REFRESH_TOKEN_TYPE);
  }
}
