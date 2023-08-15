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

import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import dev.derklaro.reflexion.Result;
import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.HttpRequest;
import eu.cloudnetservice.ext.rest.http.annotation.RequestJsonBody;
import eu.cloudnetservice.ext.rest.http.annotation.parser.AnnotationHttpHandleException;
import eu.cloudnetservice.ext.rest.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationProcessor;
import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationProcessorUtil;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerInterceptor;
import io.netty5.handler.codec.http.HttpHeaderNames;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

public final class JsonBodyProcessor implements HttpAnnotationProcessor {

  @Override
  public void buildPreprocessor(
    @NonNull HttpHandlerConfig.Builder config,
    @NonNull Method method,
    @NonNull Object handlerInstance
  ) {
    var hints = HttpAnnotationProcessorUtil.mapParameters(
      method,
      RequestJsonBody.class,
      (param, annotation) -> {
        var type = param.getParameterizedType();
        var deserializer = this.constructDeserializer(annotation.jsonDeserializer());

        return context -> {
          var request = context.request();
          return deserializer.deserialize(request, type, request.bodyStream());
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

  private @NonNull RequestJsonBody.JsonDeserializer constructDeserializer(
    @NonNull Class<? extends RequestJsonBody.JsonDeserializer> deserializerClass
  ) {
    if (deserializerClass == RequestJsonBody.DefaultJsonDeserializer.class) {
      return DefaultGsonJsonDeserializer.INSTANCE;
    } else {
      return Reflexion.on(deserializerClass).findConstructor()
        .map(MethodAccessor::<RequestJsonBody.JsonDeserializer>invoke)
        .map(Result::getOrThrow)
        .orElseThrow(() -> new IllegalArgumentException(
          "Missing no-args constructor in JsonDeserializer class: " + deserializerClass));
    }
  }

  private static final class DefaultGsonJsonDeserializer implements RequestJsonBody.JsonDeserializer {

    private static final Gson GSON = new GsonBuilder()
      .serializeNulls()
      .disableHtmlEscaping()
      .create();
    private static final RequestJsonBody.JsonDeserializer INSTANCE = new DefaultGsonJsonDeserializer();

    private static @NonNull Charset extractRequestCharsetOrUtf8(@NonNull HttpRequest request) {
      // get the content type header - if not present we just assume UTF8
      var contentType = request.header(HttpHeaderNames.CONTENT_TYPE.toString());
      if (contentType == null) {
        return StandardCharsets.UTF_8;
      }

      try {
        var mediaType = MediaType.parse(contentType);
        return mediaType.charset().or(StandardCharsets.UTF_8);
      } catch (IllegalArgumentException exception) {
        return StandardCharsets.UTF_8;
      }
    }

    @Override
    public @NonNull Object deserialize(
      @NonNull HttpRequest req,
      @NonNull Type objectType,
      @NonNull InputStream bodyStream
    ) {
      var requestCharset = extractRequestCharsetOrUtf8(req);
      try (var streamReader = new InputStreamReader(bodyStream, requestCharset)) {
        return GSON.fromJson(streamReader, objectType);
      } catch (IOException | JsonParseException exception) {
        throw new AnnotationHttpHandleException(req, "Unable to deserialize JSON from request body");
      }
    }
  }
}
