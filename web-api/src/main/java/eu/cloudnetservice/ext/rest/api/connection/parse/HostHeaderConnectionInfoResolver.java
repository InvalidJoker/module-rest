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

package eu.cloudnetservice.ext.rest.api.connection.parse;

import com.google.common.net.HttpHeaders;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.connection.BasicHttpConnectionInfo;
import eu.cloudnetservice.ext.rest.api.connection.HttpConnectionInfoResolver;
import lombok.NonNull;

public final class HostHeaderConnectionInfoResolver implements HttpConnectionInfoResolver {

  public static final HttpConnectionInfoResolver INSTANCE = new HostHeaderConnectionInfoResolver();

  private HostHeaderConnectionInfoResolver() {
  }

  @Override
  public @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo
  ) {
    // extract the server target information from the given host header
    // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Host
    var hostHeaderValue = context.request().header(HttpHeaders.HOST);
    if (hostHeaderValue != null) {
      var defaultPort = baseInfo.defaultPortForScheme();
      var parsedAddress = AddressParseUtil.parseHostAndPort("Host", hostHeaderValue, defaultPort);
      baseInfo = baseInfo.withHostAddress(parsedAddress);
    }

    return baseInfo;
  }
}