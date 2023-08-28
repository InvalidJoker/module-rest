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

package eu.cloudnetservice.ext.rest.api.annotation.parser.processor;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.AuthenticationResult;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.problem.ProblemHttpHandleException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class AuthenticationProcessor implements HttpAnnotationProcessor {

  private static final ProblemDetail AUTH_METHOD_UNKNOWN = ProblemDetail.builder()
    .title("Auth Method Unknown")
    .status(HttpResponseCode.BAD_REQUEST)
    .type(URI.create("auth-method-unknown"))
    .detail("Requested authentication method is unavailable.")
    .build();

  private static final ProblemDetail AUTH_INVALID = ProblemDetail.builder()
    .title("Auth Invalid")
    .type(URI.create("auth-invalid"))
    .status(HttpResponseCode.UNAUTHORIZED)
    .detail("The provided auth information is invalid.")
    .build();

  private static final ProblemDetail AUTH_REQUIRED_SCOPE_MISSING = ProblemDetail.builder()
    .title("Auth Required Scope Missing")
    .type(URI.create("auth-required-scope-missing"))
    .status(HttpResponseCode.FORBIDDEN)
    .detail("The authenticated user misses a required scope to access the resource.")
    .build();

  private final Supplier<RestUserManagement> management;

  public AuthenticationProcessor(@NonNull Supplier<RestUserManagement> management) {
    this.management = management;
  }

  private static @Nullable Authentication extractAnnotation(@NonNull Method method) {
    if (method.isAnnotationPresent(Authentication.class)) {
      return method.getAnnotation(Authentication.class);
    }

    var declaringClass = method.getDeclaringClass();
    if (declaringClass.isAnnotationPresent(Authentication.class)) {
      return declaringClass.getAnnotation(Authentication.class);
    }

    return null;
  }

  private static @NonNull List<? extends AuthProvider<?>> resolveProvider(@NonNull Authentication authentication) {
    var providers = Arrays.stream(authentication.providers()).map(AuthProviderLoader::resolveAuthProvider).toList();
    if (providers.isEmpty()) {
      throw new IllegalArgumentException("No auth providers given in @Authentication");
    }

    return providers;
  }

  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handlerInstance
  ) {
    var hints = HttpAnnotationProcessorUtil.mapParameters(
      method,
      Authentication.class,
      (param, annotation) -> {
        var provider = resolveProvider(annotation);
        return context -> this.handleAuth(context, provider, annotation.scopes());
      });

    var authentication = extractAnnotation(method);
    var provider = authentication != null ? resolveProvider(authentication) : null;

    config.addHandlerInterceptor(new HttpHandlerInterceptor() {
      @Override
      public boolean preProcess(
        @NonNull HttpContext context,
        @NonNull HttpHandler handler,
        @NonNull HttpHandlerConfig config
      ) {
        if (!hints.isEmpty()) {
          context.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
        } else if (provider != null) {
          AuthenticationProcessor.this.handleAuth(context, provider, authentication.scopes());
        }

        return true;
      }
    });
  }

  private @NonNull RestUser handleAuth(
    @NonNull HttpContext context,
    @NonNull List<? extends AuthProvider<?>> provider,
    @NonNull String[] scopes
  ) {
    if (provider.isEmpty()) {
      throw new IllegalArgumentException("No auth providers given in @Authentication");
    }

    // try all requested auth providers, break after the user was handled at least once
    AuthenticationResult authenticationResult = null;
    for (var authProvider : provider) {
      authenticationResult = authProvider.tryAuthenticate(context, this.management.get());
      if (!authenticationResult.empty()) {
        break;
      }
    }

    // ok, we have a user
    if (authenticationResult.ok()) {
      var user = authenticationResult.user();
      if (scopes.length != 0 && !user.hasOneScopeOf(scopes)) {
        throw new ProblemHttpHandleException(AUTH_REQUIRED_SCOPE_MISSING);
      }

      return user;
    }

    // no auth provider handled the auth attempt
    if (authenticationResult.empty()) {
      throw new ProblemHttpHandleException(AUTH_METHOD_UNKNOWN);
    }

    // either the user does not exist or the credentials are invalid
    throw new ProblemHttpHandleException(AUTH_INVALID);
  }
}
