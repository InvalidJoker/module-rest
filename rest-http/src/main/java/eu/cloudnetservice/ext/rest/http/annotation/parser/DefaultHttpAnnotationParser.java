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

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.rest.http.HttpComponent;
import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.ext.rest.http.annotation.Optional;
import eu.cloudnetservice.ext.rest.http.annotation.RequestBody;
import eu.cloudnetservice.ext.rest.http.annotation.RequestHeader;
import eu.cloudnetservice.ext.rest.http.annotation.RequestPath;
import eu.cloudnetservice.ext.rest.http.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.http.annotation.RequestQueryParam;
import eu.cloudnetservice.ext.rest.http.config.CorsConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerInterceptor;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of a http annotation parser.
 *
 * @param <T> the type of http component associated with this parser.
 * @since 4.0
 */
public final class DefaultHttpAnnotationParser<T extends HttpComponent<T>> implements HttpAnnotationParser<T> {

  public static final String DEFAULTS_TO_NULL_MASK = "__NULL__";
  public static final String PARAM_INVOCATION_HINT_KEY = "__PARAM_INVOCATION_HINT__";

  private final T component;
  private final CorsConfig globalCorsConfig;
  private final Deque<HttpAnnotationProcessor> processors = new LinkedList<>();

  /**
   * Constructs a new DefaultHttpAnnotationParser instance.
   *
   * @param component        the component instance with which this parser is associated.
   * @param globalCorsConfig the global cors configuration to merge with the handler specific one.
   * @throws NullPointerException if the given component is null.
   */
  public DefaultHttpAnnotationParser(@NonNull T component, @NonNull CorsConfig globalCorsConfig) {
    this.component = component;
    this.globalCorsConfig = globalCorsConfig;
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
  private static @Nullable Object applyDefault(@NonNull String defaultValue, @Nullable String actualValue) {
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
   * @param component        the component instance with which this parser is associated.
   * @param globalCorsConfig the global cors configuration to merge with the handler specific one.
   * @return the newly created HttpAnnotationParser instance.
   * @throws NullPointerException if the given component is null.
   */
  public static @NonNull <T extends HttpComponent<T>> HttpAnnotationParser<T> withDefaultProcessors(
    @NonNull T component,
    @NonNull CorsConfig globalCorsConfig
  ) {
    return new DefaultHttpAnnotationParser<>(component, globalCorsConfig).registerDefaultProcessors();
  }

  /**
   * Registers all processors for the default provided http handling annotations.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull HttpAnnotationParser<T> registerDefaultProcessors() {
    return this
      .registerAnnotationProcessor(new FirstRequestQueryParamProcessor())
      .registerAnnotationProcessor(new RequestBodyProcessor())
      .registerAnnotationProcessor(new RequestHeaderProcessor())
      .registerAnnotationProcessor(new RequestPathProcessor())
      .registerAnnotationProcessor(new RequestPathParamProcessor())
      .registerAnnotationProcessor(new RequestQueryParamProcessor());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull T httpComponent() {
    return this.component;
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
  public @NonNull HttpAnnotationParser<T> registerAnnotationProcessor(@NonNull HttpAnnotationProcessor processor) {
    this.processors.addFirst(processor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser<T> unregisterAnnotationProcessor(@NonNull HttpAnnotationProcessor processor) {
    this.processors.remove(processor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser<T> unregisterAnnotationProcessors(@NonNull ClassLoader classLoader) {
    this.processors.removeIf(entry -> entry.getClass().getClassLoader() == classLoader);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser<T> parseAndRegister(@NonNull Class<?> handlerClass) {
    var injectionLayer = InjectionLayer.findLayerOf(handlerClass);
    return this.parseAndRegister(injectionLayer.instance(handlerClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser<T> parseAndRegister(@NonNull Object handlerInstance) {
    for (var method : handlerInstance.getClass().getDeclaredMethods()) {
      // check if the handler is requested to be a request handler
      var annotation = method.getAnnotation(HttpRequestHandler.class);
      if (annotation != null) {
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

        var configBuilder = HttpHandlerConfig.builder();
        // set the supported request method of the handler
        configBuilder.httpMethod(annotation.method());

        // add the processors to the config
        for (var processor : this.processors) {
          if (processor.shouldProcess(method, handlerInstance)) {
            var contextProcessor = processor.buildPreprocessor(method, handlerInstance);
            if (contextProcessor != null) {
              configBuilder.addHandlerInterceptor(contextProcessor);
            }
          }
        }

        // register the handler
        var handler = new MethodHttpHandlerInvoker(handlerInstance, method);
        this.component.registerHandler(annotation.path(), handler, configBuilder.build());
      }
    }

    return this;
  }

  /**
   * A processor for the {@code @FirstRequestQueryParam} annotation.
   *
   * @since 4.0
   */
  private static final class FirstRequestQueryParamProcessor implements HttpAnnotationProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull HttpHandlerInterceptor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        FirstRequestQueryParam.class,
        (param, annotation) -> (context) -> {
          // get the parameters and error out if no values are present but the parameter is required
          var queryParameters = context.request().queryParameters().get(annotation.value());
          if (!param.isAnnotationPresent(Optional.class) && (queryParameters == null || queryParameters.isEmpty())) {
            throw new AnnotationHttpHandleException(
              context.request(),
              "Missing required query param: " + annotation.value());
          }

          // return the first value or null if not possible
          return applyDefault(
            annotation.def(),
            queryParameters == null ? null : Iterables.getFirst(queryParameters, null));
        });
      return new HttpHandlerInterceptor() {
        @Override
        public boolean preProcess(
          @NonNull HttpContext context,
          @NonNull HttpHandler handler,
          @NonNull HttpHandlerConfig config
        ) {
          context.addInvocationHints(PARAM_INVOCATION_HINT_KEY, hints);
          return true;
        }
      };
    }
  }

  /**
   * A processor for the {@code @RequestBody} annotation.
   *
   * @since 4.0
   */
  private static final class RequestBodyProcessor implements HttpAnnotationProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull HttpHandlerInterceptor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        RequestBody.class,
        (param, annotation) -> (context) -> {
          // match the type of the parameter in order to inject the correct value
          if (String.class.isAssignableFrom(param.getType())) {
            return context.request().bodyAsString();
          }

          if (byte[].class.isAssignableFrom(param.getType())) {
            return context.request().body();
          }

          if (InputStream.class.isAssignableFrom(param.getType())) {
            return context.request().bodyStream();
          }

          if (Document.class.isAssignableFrom(param.getType())) {
            return DocumentFactory.json().parse(context.request().bodyStream());
          }

          // unable to handle the type
          throw new AnnotationHttpHandleException(
            context.request(),
            "Unable to inject body of type " + param.getType().getName());
        });
      return new HttpHandlerInterceptor() {
        @Override
        public boolean preProcess(
          @NonNull HttpContext context,
          @NonNull HttpHandler handler,
          @NonNull HttpHandlerConfig config
        ) {
          context.addInvocationHints(PARAM_INVOCATION_HINT_KEY, hints);
          return true;
        }
      };
    }
  }

  /**
   * A processor for the {@code @RequestHeader} annotation.
   *
   * @since 4.0
   */
  private static final class RequestHeaderProcessor implements HttpAnnotationProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull HttpHandlerInterceptor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
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
          return applyDefault(annotation.def(), header);
        });
      return new HttpHandlerInterceptor() {
        @Override
        public boolean preProcess(
          @NonNull HttpContext context,
          @NonNull HttpHandler handler,
          @NonNull HttpHandlerConfig config
        ) {
          context.addInvocationHints(PARAM_INVOCATION_HINT_KEY, hints);
          return true;
        }
      };
    }
  }

  /**
   * A processor for the {@code @RequestPath} annotation.
   *
   * @since 4.0
   */
  private static final class RequestPathProcessor implements HttpAnnotationProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull HttpHandlerInterceptor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        RequestPath.class,
        (param, annotation) -> (context) -> context.request().path());
      return new HttpHandlerInterceptor() {
        @Override
        public boolean preProcess(
          @NonNull HttpContext context,
          @NonNull HttpHandler handler,
          @NonNull HttpHandlerConfig config
        ) {
          context.addInvocationHints(PARAM_INVOCATION_HINT_KEY, hints);
          return true;
        }
      };
    }
  }

  /**
   * A processor for the {@code @RequestPathParam} annotation.
   *
   * @since 4.0
   */
  private static final class RequestPathParamProcessor implements HttpAnnotationProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull HttpHandlerInterceptor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
      var hints = HttpAnnotationProcessorUtil.mapParameters(
        method,
        RequestPathParam.class,
        (param, annotation) -> (context) -> {
          // get the path parameter and error out if no value is present but the parameter is required
          var pathParam = context.request().pathParameters().get(annotation.value());
          if (!param.isAnnotationPresent(Optional.class) && pathParam == null) {
            throw new AnnotationHttpHandleException(
              context.request(),
              "Missing required path parameter: " + annotation.value());
          }

          // set the path parameter in the context
          return pathParam;
        });
      return new HttpHandlerInterceptor() {
        @Override
        public boolean preProcess(
          @NonNull HttpContext context,
          @NonNull HttpHandler handler,
          @NonNull HttpHandlerConfig config
        ) {
          context.addInvocationHints(PARAM_INVOCATION_HINT_KEY, hints);
          return true;
        }
      };
    }
  }

  /**
   * A processor for the {@code @RequestQueryParam} annotation.
   *
   * @since 4.0
   */
  private static final class RequestQueryParamProcessor implements HttpAnnotationProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull HttpHandlerInterceptor buildPreprocessor(@NonNull Method method, @NonNull Object handler) {
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
      return new HttpHandlerInterceptor() {
        @Override
        public boolean preProcess(
          @NonNull HttpContext context,
          @NonNull HttpHandler handler,
          @NonNull HttpHandlerConfig config
        ) {
          context.addInvocationHints(PARAM_INVOCATION_HINT_KEY, hints);
          return true;
        }
      };
    }
  }
}
