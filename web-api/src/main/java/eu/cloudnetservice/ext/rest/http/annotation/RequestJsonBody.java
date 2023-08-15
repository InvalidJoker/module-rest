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

package eu.cloudnetservice.ext.rest.http.annotation;

import eu.cloudnetservice.ext.rest.http.HttpRequest;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import lombok.NonNull;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestJsonBody {

  Class<? extends JsonDeserializer> jsonDeserializer() default DefaultJsonDeserializer.class;

  @FunctionalInterface
  interface JsonDeserializer {

    @NonNull Object deserialize(
      @NonNull HttpRequest request,
      @NonNull Type objectType,
      @NonNull InputStream bodyStream);
  }

  final class DefaultJsonDeserializer implements JsonDeserializer {

    @Override
    public @NonNull Object deserialize(
      @NonNull HttpRequest req,
      @NonNull Type objectType,
      @NonNull InputStream bodyStream
    ) {
      throw new UnsupportedOperationException("should never call this directly");
    }
  }
}
