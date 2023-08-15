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

package eu.cloudnetservice.ext.rest.http.cors;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpRequest;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface CorsRequestProcessor {

  @Nullable CorsPreflightRequestInfo extractInfoFromPreflightRequest(@NonNull HttpRequest request);

  void processPreflightRequest(
    @NonNull HttpContext context,
    @NonNull CorsPreflightRequestInfo preflightRequestInfo,
    @Nullable HttpHandlerConfig httpHandlerConfig);

  boolean processNormalRequest(@NonNull HttpContext context, @NonNull HttpHandlerConfig httpHandlerConfig);
}