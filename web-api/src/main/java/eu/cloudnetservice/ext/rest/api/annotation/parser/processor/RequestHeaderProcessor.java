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
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHeader;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * A processor for the {@code @RequestHeader} annotation.
 *
 * @since 1.0
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
      (param, annotation) -> {
        var isOptionalAnnotation = param.isAnnotationPresent(Optional.class);
        return (context) -> {
          // check if all headers were requested
          if (Iterable.class.isAssignableFrom(param.getType())) {
            return context.request().headers().values(annotation.value());
          }

          // get the first header value or the default value supplied in the annotation
          var headerValue = context.request().headers().firstValue(annotation.value());
          var parameterValue = DefaultHttpAnnotationParser.applyDefault(annotation.def(), headerValue);

          // check if the value was requested optionally
          if (param.getType() == java.util.Optional.class) {
            return java.util.Optional.ofNullable(parameterValue);
          }

          // ensure that the value for the header is present in case it wasn't defined otherwise
          if (!isOptionalAnnotation && parameterValue == null) {
            throw new AnnotationHttpHandleException(
              context.request(),
              "Missing required header: " + annotation.value());
          }

          return parameterValue;
        };
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
