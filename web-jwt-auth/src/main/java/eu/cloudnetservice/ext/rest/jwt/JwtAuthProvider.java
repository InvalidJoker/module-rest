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
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class JwtAuthProvider implements AuthProvider<Map<String, Object>> {

  public static final String JWT_TOKEN_PAIR_KEY = "jwt_token_pair";

  private static final String DEFAULT_ISSUER = "global_issuer";
  private static final Duration DEFAULT_ACCESS_TOKEN_EXPIRATION = Duration.ofHours(10);
  private static final Duration DEFAULT_REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);

  private static final Pattern BEARER_LOGIN_PATTERN = Pattern.compile("Bearer ([a-zA-Z\\d-_.]+)$");

  private final String issuer;
  private final Key jwtSigningKey;

  private final Duration accessDuration;
  private final Duration refreshDuration;

  private final JwtParser jwtParser;

  public JwtAuthProvider() {
    this(
      DEFAULT_ISSUER,
      Keys.secretKeyFor(SignatureAlgorithm.HS512),
      null,
      DEFAULT_ACCESS_TOKEN_EXPIRATION,
      DEFAULT_REFRESH_TOKEN_EXPIRATION);
  }

  public JwtAuthProvider(
    @NonNull String issuer,
    @NonNull Key jwtSigningKey,
    @Nullable Key jwtValidationKey,
    @NonNull Duration accessDuration,
    @NonNull Duration refreshDuration
  ) {
    this.issuer = issuer;
    this.jwtSigningKey = jwtSigningKey;
    this.accessDuration = accessDuration;
    this.refreshDuration = refreshDuration;

    // symmetric keys only use one key for signing and validating
    // asymmetric keys need a separate keys: one to sign and one to validate
    var validationKey = Objects.requireNonNullElse(jwtValidationKey, jwtSigningKey);
    this.jwtParser = Jwts.parserBuilder().requireIssuer(issuer).setSigningKey(validationKey).build();
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
    // check if the authorization header is present
    var authHeader = context.request().headers().firstValue(HttpHeaders.AUTHORIZATION);
    if (authHeader == null) {
      return AuthenticationResult.proceed();
    }

    // check if the authorization header is a bearer token
    var tokenMatcher = BEARER_LOGIN_PATTERN.matcher(authHeader);
    if (!tokenMatcher.matches()) {
      return AuthenticationResult.proceed();
    }

    try {
      // parse the JWT token - this call throws in case the jwt is invalid in any form
      var token = this.jwtParser.parseClaimsJws(tokenMatcher.group(1));

      // validate the token subject
      var subject = token.getBody().getSubject();
      var user = management.restUser(subject);
      if (user == null) {
        return AuthenticationResult.userNotFound();
      }

      // validate that the id of the token still has access granted
      var tokenPairs = user.properties().get(JWT_TOKEN_PAIR_KEY);
      var parsedTokens = JwtTokenPropertyParser.parseTokens(tokenPairs);
      if (this.checkTokenValidity(parsedTokens, token.getBody().getId())) {
        return AuthenticationResult.ok(user);
      } else {
        return AuthenticationResult.invalidCredentials();
      }
    } catch (JwtException exception) {
      return AuthenticationResult.invalidCredentials();
    }
  }

  @Override
  public @NonNull JwtAuthToken generateAuthToken(@NonNull RestUserManagement management, @NonNull RestUser restUser) {
    // generate a new access and refresh token for the user
    var tokenPairId = this.newRandomTokenId();
    var accessToken = this.generateNewJwtToken(
      tokenPairId,
      JwtTokenHolder.ACCESS_TOKEN_TYPE,
      restUser,
      this.accessDuration);
    var refreshToken = this.generateNewJwtToken(
      tokenPairId,
      JwtTokenHolder.REFRESH_TOKEN_TYPE,
      restUser,
      this.refreshDuration);

    // get the tokens that are currently stored in the rest user
    var tokenProperty = restUser.properties().get(JWT_TOKEN_PAIR_KEY);
    var parsedStoredTokens = JwtTokenPropertyParser.parseTokens(tokenProperty);

    // remove the outdated tokens and register the new ones
    var currentTime = Instant.now();
    var tokens = parsedStoredTokens.stream()
      .filter(holder -> currentTime.isBefore(holder.expiresAt()))
      .collect(Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), list -> {
        list.add(accessToken);
        list.add(refreshToken);
        return list;
      }));

    // update the token list
    var compactedTokens = JwtTokenPropertyParser.compactTokens(tokens);
    var updatedUser = management.builder(restUser).property(JWT_TOKEN_PAIR_KEY, compactedTokens).build();
    management.saveRestUser(updatedUser);

    // return the generated token pair
    return new JwtAuthToken(currentTime, accessToken, refreshToken);
  }

  private @NonNull String newRandomTokenId() {
    return UUID.randomUUID().toString();
  }

  private @NonNull JwtTokenHolder generateNewJwtToken(
    @NonNull String tokenId,
    @NonNull String tokenType,
    @NonNull RestUser subject,
    @NonNull Duration validDuration
  ) {
    var expiration = Instant.now().plus(validDuration);
    var jwtToken = Jwts.builder()
      .setIssuer(this.issuer)
      .setSubject(subject.id())
      .setIssuedAt(new Date())
      .setExpiration(Date.from(expiration))
      .setId(tokenId)
      .claim("type", tokenType)
      .signWith(this.jwtSigningKey)
      .compact();
    return new JwtTokenHolder(jwtToken, tokenId, expiration, tokenType);
  }

  protected boolean checkTokenValidity(@NonNull Collection<JwtTokenHolder> tokens, @NonNull String tokenId) {
    var currentTime = Instant.now();
    return tokens.stream()
      .filter(holder -> currentTime.isBefore(holder.expiresAt()))
      .filter(holder -> holder.tokenId().equals(tokenId))
      .anyMatch(holder -> holder.tokenType().equals(JwtTokenHolder.ACCESS_TOKEN_TYPE));
  }
}
