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
import eu.cloudnetservice.ext.rest.api.annotation.RequestBody;
import eu.cloudnetservice.ext.rest.api.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.ext.rest.api.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.api.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerInterceptor;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

/**
 * A processor for the {@code @RequestBody} annotation.
 *
 * @since 4.0
 */
public final class RequestBodyProcessor implements HttpAnnotationProcessor {

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
      RequestBody.class,
      (param, annotation) -> (context) -> {
        if (String.class.isAssignableFrom(param.getType())) {
          return context.request().bodyAsString();
        } else if (byte[].class.isAssignableFrom(param.getType())) {
          return context.request().body();
        } else if (ByteBuffer.class.isAssignableFrom(param.getType())) {
          return ByteBuffer.wrap(context.request().body());
        } else if (InputStream.class.isAssignableFrom(param.getType())) {
          return context.request().bodyStream();
        } else if (Reader.class.isAssignableFrom(param.getType())) {
          return new InputStreamReader(context.request().bodyStream(), StandardCharsets.UTF_8);
        } else {
          throw new AnnotationHttpHandleException(
            context.request(),
            "Unable to inject body of type " + param.getType().getName());
        }
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
