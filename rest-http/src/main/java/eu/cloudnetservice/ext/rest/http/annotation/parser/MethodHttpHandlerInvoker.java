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

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.response.Response;
import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * A http handler which delegates its calls to a method invocation, resolving the parameters for that from the
 * preprocessed http context.
 *
 * @since 4.0
 */
final class MethodHttpHandlerInvoker implements HttpHandler {

  private final Object instance;
  private final Class<?>[] handlerParameterTypes;
  private final MethodAccessor<?> handlerMethodAccessor;

  /**
   * Constructs a new MethodHttpHandlerInvoker instance.
   *
   * @param handlerInstance the instance in which the http handler method is located.
   * @param handlerMethod   the method to delegate matching http calls to.
   * @throws NullPointerException   if the given instance, method or methods collection is null.
   */
  public MethodHttpHandlerInvoker(@NonNull Object handlerInstance, @NonNull Method handlerMethod) {
    this.instance = handlerInstance;
    this.handlerParameterTypes = handlerMethod.getParameterTypes();
    this.handlerMethodAccessor = Reflexion.onBound(handlerInstance).unreflect(handlerMethod);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Response<?> handle(@NonNull HttpContext context) {
    var arguments = this.buildInvocationArguments(context);
    return this.handlerMethodAccessor.<Response<?>>invoke(this.instance, arguments).getOrThrow();
  }

  /**
   * Constructs the arguments array which will be passed to the method which is actually handling the associated
   * request.
   *
   * @param context the current context of the request.
   * @throws NullPointerException          if the given path or context is null.
   * @throws AnnotationHttpHandleException if any exception occurs while resolving the arguments.
   */
  private @NonNull Object[] buildInvocationArguments(@NonNull HttpContext context) {
    var arguments = new Object[this.handlerParameterTypes.length];
    var invocationHints = context.invocationHints(DefaultHttpAnnotationParser.PARAM_INVOCATION_HINT_KEY);

    // check if enough arguments are passed from the preprocessing (-1 as the context is always the first argument)
    var expectedArgumentCount = arguments.length - 1;
    if (invocationHints.size() != expectedArgumentCount) {
      throw new AnnotationHttpHandleException(context.request(), String.format(
        "Arguments count to call handler does not match (got: %d; expected: %d)",
        invocationHints.size(),
        expectedArgumentCount));
    }

    // get the value of each hint and store it in the args array
    for (var invocationHint : invocationHints) {
      // validate that we got and invocation hint
      if (invocationHint instanceof ParameterInvocationHint hint) {
        // validate the index
        if (hint.index() <= 0 || hint.index() >= arguments.length) {
          throw new AnnotationHttpHandleException(context.request(), "Invocation hint index " + hint.index() + " is out of bounds");
        }

        // validate that the value type is matching the expected type at the index
        var value = hint.resolveValue(context);
        var expectedType = this.handlerParameterTypes[hint.index()];
        if (value != null && !expectedType.isAssignableFrom(value.getClass())) {
          throw new AnnotationHttpHandleException(context.request(), String.format(
            "Parameter at index %d is of type %s; expected type %s",
            hint.index(),
            value.getClass().getName(),
            expectedType.getName()));
        }

        // don't accidentally try to inject null into a primitive type
        if (value == null && expectedType.isPrimitive()) {
          throw new AnnotationHttpHandleException(context.request(), String.format(
            "Parameter at index %d is primitive but null was resolved as the parameter value",
            hint.index()));
        }

        // all fine, store the argument
        arguments[hint.index()] = value;
      } else {
        throw new AnnotationHttpHandleException(
          context.request(),
          "Hint " + invocationHint + " is not an ParameterInvocationHint");
      }
    }

    // put in the context argument and return the completed array
    arguments[0] = context;
    return arguments;
  }
}
