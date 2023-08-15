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

package eu.cloudnetservice.ext.rest.http.annotation.parser.processor;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.annotation.Optional;
import eu.cloudnetservice.ext.rest.http.annotation.RequestQueryParam;
import eu.cloudnetservice.ext.rest.http.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.ext.rest.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerInterceptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;

/**
 * A processor for the {@code @RequestQueryParam} annotation.
 *
 * @since 4.0
 */
public final class RequestQueryParamProcessor implements HttpAnnotationProcessor {

  /**
   * {@inheritDoc}
   */
  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handler
  ) {
    var hints = HttpAnnotationProcessorUtil.mapParameters(
      method,
      RequestQueryParam.class,
      (param, annotation) -> (context) -> {
        // get the parameters and error out if no values are present but the parameter is required
        var queryParameters = context.request().queryParameters().get(annotation.value());
        if (!param.isAnnotationPresent(Optional.class) && (queryParameters == null || queryParameters.isEmpty())) {
          throw new AnnotationHttpHandleException(
            context.request(),
            "Missing required query param: " + annotation.value());
        }

        // set the parameters in the context
        return (queryParameters == null || queryParameters.isEmpty()) && annotation.nullWhenAbsent()
          ? null
          : Objects.requireNonNullElse(queryParameters, List.of());
      });
    config.addHandlerInterceptor(new HttpHandlerInterceptor() {
      @Override
      public boolean preProcess(
        @NonNull HttpContext context,
        @NonNull HttpHandler handler,
        @NonNull HttpHandlerConfig config
      ) {
        context.addInvocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY, hints);
        return true;
      }
    });
  }
}
