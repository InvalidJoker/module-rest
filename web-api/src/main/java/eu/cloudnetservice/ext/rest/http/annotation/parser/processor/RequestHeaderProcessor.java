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
import eu.cloudnetservice.ext.rest.http.annotation.RequestHeader;
import eu.cloudnetservice.ext.rest.http.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.ext.rest.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerInterceptor;
import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * A processor for the {@code @RequestHeader} annotation.
 *
 * @since 4.0
 */
public final class RequestHeaderProcessor implements HttpAnnotationProcessor {

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
      RequestHeader.class,
      (param, annotation) -> (context) -> {
        // get the header and error out if no value is present but the header is required
        var header = context.request().header(annotation.value());
        if (!param.isAnnotationPresent(Optional.class) && header == null) {
          throw new AnnotationHttpHandleException(
            context.request(),
            "Missing required header: " + annotation.value());
        }

        // set the header in the context
        return DefaultHttpAnnotationParser.applyDefault(annotation.def(), header);
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
