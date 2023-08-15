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

package eu.cloudnetservice.ext.rest.http.annotation.parser;

import eu.cloudnetservice.ext.rest.http.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.ContentTypeProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.CrossOriginProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.FirstRequestQueryParamProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.RequestBodyProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.RequestHeaderProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.RequestPathParamProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.RequestPathProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.RequestQueryParamProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.processor.RequestTypedBodyProcessor;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.http.registry.HttpHandlerRegistry;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of a http annotation parser.
 *
 * @since 4.0
 */
public final class DefaultHttpAnnotationParser implements HttpAnnotationParser {

  public static final String DEFAULTS_TO_NULL_MASK = "__NULL__";
  public static final String PARAM_INVOCATION_HINT_KEY = "__PARAM_INVOCATION_HINT__";

  private final HttpHandlerRegistry httpHandlerRegistry;
  private final Deque<HttpAnnotationProcessor> processors = new LinkedList<>();

  public DefaultHttpAnnotationParser(@NonNull HttpHandlerRegistry httpHandlerRegistry) {
    this.httpHandlerRegistry = httpHandlerRegistry;
  }

  /**
   * Returns the actual value if present, or the default value of an annotation. If the default value equals to
   * {@code __NULL__} then null is returned, else the given default value.
   *
   * @param defaultValue the defined default value to unmask.
   * @param actualValue  the actual value which is present from the request.
   * @return the actual value if present or the unmasked version of the default value.
   * @throws NullPointerException if the given default value is null.
   */
  public static @Nullable Object applyDefault(@NonNull String defaultValue, @Nullable String actualValue) {
    // return the actual value directly if present
    if (actualValue != null) {
      return actualValue;
    }

    // unmask the default value if needed and return it
    return defaultValue.equals(DEFAULTS_TO_NULL_MASK) ? null : defaultValue;
  }

  /**
   * Constructs a new DefaultHttpAnnotationParser instance and registers all processors for the default provided http
   * handling annotations.
   *
   * @param registry // TODO
   * @return the newly created HttpAnnotationParser instance.
   * @throws NullPointerException if the given component is null.
   */
  public static @NonNull HttpAnnotationParser withDefaultProcessors(
    @NonNull HttpHandlerRegistry registry
  ) {
    return new DefaultHttpAnnotationParser(registry).registerDefaultProcessors();
  }

  /**
   * Registers all processors for the default provided http handling annotations.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull HttpAnnotationParser registerDefaultProcessors() {
    return this
      .registerAnnotationProcessor(new FirstRequestQueryParamProcessor())
      .registerAnnotationProcessor(new RequestBodyProcessor())
      .registerAnnotationProcessor(new RequestHeaderProcessor())
      .registerAnnotationProcessor(new RequestPathProcessor())
      .registerAnnotationProcessor(new RequestPathParamProcessor())
      .registerAnnotationProcessor(new RequestQueryParamProcessor())
      .registerAnnotationProcessor(new ContentTypeProcessor())
      .registerAnnotationProcessor(new RequestTypedBodyProcessor())
      .registerAnnotationProcessor(new CrossOriginProcessor());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnmodifiableView
  public @NonNull Collection<HttpAnnotationProcessor> annotationProcessors() {
    return Collections.unmodifiableCollection(this.processors);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser registerAnnotationProcessor(@NonNull HttpAnnotationProcessor processor) {
    this.processors.addFirst(processor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser unregisterAnnotationProcessor(@NonNull HttpAnnotationProcessor processor) {
    this.processors.remove(processor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser unregisterAnnotationProcessors(@NonNull ClassLoader classLoader) {
    this.processors.removeIf(entry -> entry.getClass().getClassLoader() == classLoader);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser parseAndRegister(@NonNull Class<?> handlerClass) {
    //var injectionLayer = InjectionLayer.findLayerOf(handlerClass);
    //return this.parseAndRegister(injectionLayer.instance(handlerClass));
    // TODO: can/should we support this here?
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser parseAndRegister(@NonNull Object handlerInstance) {
    for (var method : handlerInstance.getClass().getDeclaredMethods()) {
      // check if the handler is requested to be a request handler
      var handlerAnnotation = method.getAnnotation(RequestHandler.class);
      if (handlerAnnotation != null) {
        // we don't support static methods
        if (Modifier.isStatic(method.getModifiers())) {
          throw new IllegalArgumentException(String.format(
            "Http handler method (@HttpRequestHandler) %s in %s must not be static!",
            method.getName(), method.getDeclaringClass().getName()));
        }

        // fail-fast: try to make the method accessible if needed
        if (!Modifier.isPublic(method.getModifiers())) {
          method.setAccessible(true);
        }

        // set the supported request method of the handler
        var configBuilder = HttpHandlerConfig.builder();
        configBuilder.httpMethod(handlerAnnotation.method());

        // add the processors to the corsConfig
        for (var processor : this.processors) {
          if (processor.shouldProcess(method, handlerInstance)) {
            processor.buildPreprocessor(configBuilder, method, handlerInstance);
          }
        }

        // register the handler
        var handler = new MethodHttpHandlerInvoker(handlerInstance, method);
        this.httpHandlerRegistry.registerHandler(handlerAnnotation.path(), handler, configBuilder.build());
      }
    }

    return this;
  }
}
