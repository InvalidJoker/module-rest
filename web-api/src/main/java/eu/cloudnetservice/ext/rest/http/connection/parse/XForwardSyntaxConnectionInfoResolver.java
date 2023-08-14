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

package eu.cloudnetservice.ext.rest.http.connection.parse;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpRequest;
import eu.cloudnetservice.ext.rest.http.connection.BasicHttpConnectionInfo;
import eu.cloudnetservice.ext.rest.http.connection.HttpConnectionInfoResolver;
import eu.cloudnetservice.ext.rest.http.util.HostAndPort;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class XForwardSyntaxConnectionInfoResolver implements HttpConnectionInfoResolver {

  private final String forwardedForHeaderName;
  private final String forwardedHostHeaderName;
  private final String forwardedPortHeaderName;
  private final String forwardedProtoHeaderName;

  public XForwardSyntaxConnectionInfoResolver(
    @Nullable String forwardedForHeaderName,
    @Nullable String forwardedHostHeaderName,
    @Nullable String forwardedPortHeaderName,
    @Nullable String forwardedProtoHeaderName
  ) {
    this.forwardedForHeaderName = forwardedForHeaderName;
    this.forwardedHostHeaderName = forwardedHostHeaderName;
    this.forwardedPortHeaderName = forwardedPortHeaderName;
    this.forwardedProtoHeaderName = forwardedProtoHeaderName;
  }

  private static @Nullable String headerValue(@NonNull HttpRequest request, @Nullable String headerName) {
    return headerName == null ? null : request.header(headerName);
  }

  @Override
  public @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo
  ) {
    // extract information about the forwarded scheme (XFS)
    // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Proto
    var forwardedScheme = headerValue(context.request(), this.forwardedProtoHeaderName);
    if (forwardedScheme != null) {
      var schemeToUse = forwardedScheme.split(",", 2)[0];
      baseInfo = baseInfo.withScheme(schemeToUse);
    }

    // extract information about the originating IP of the request (XFF)
    // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For
    var forwardedFor = headerValue(context.request(), this.forwardedForHeaderName);
    if (forwardedFor != null) {
      var defaultPort = baseInfo.clientAddress().port();
      var ipToUse = forwardedFor.split(",", 2)[0];
      var parsedAddress = AddressParseUtil.parseHostAndPort(this.forwardedForHeaderName, ipToUse, defaultPort);
      baseInfo = baseInfo.withClientAddress(parsedAddress);
    }

    // extract information about the host information (XFH)
    // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host
    var forwardedHost = headerValue(context.request(), this.forwardedHostHeaderName);
    if (forwardedHost != null) {
      var defaultPort = baseInfo.defaultPortForScheme();
      var hostToUse = forwardedHost.split(",", 2)[0];
      var parsedAddress = AddressParseUtil.parseHostAndPort(this.forwardedHostHeaderName, hostToUse, defaultPort);
      baseInfo = baseInfo.withHostAddress(parsedAddress);
    }

    // extracts information about the forwarded port (XFP)
    // while the header itself isn't documented by MDN, it seems to be a standard especially
    // when looking into the documentation of cloud providers like AWS or Oracle
    // https://docs.oracle.com/en-us/iaas/Content/Balance/Reference/httpheaders.htm
    // https://docs.aws.amazon.com/elasticloadbalancing/latest/application/x-forwarded-headers.html#x-forwarded-port
    var forwardedPort = headerValue(context.request(), this.forwardedPortHeaderName);
    if (forwardedPort != null) {
      var portToUse = forwardedPort.split(",", 2)[0];
      var parsedPort = AddressParseUtil.parsePortNumber(this.forwardedPortHeaderName, portToUse);

      var oldHostAddress = baseInfo.hostAddress();
      var hostAddressWithPort = new HostAndPort(oldHostAddress.host(), parsedPort);
      baseInfo = baseInfo.withHostAddress(hostAddressWithPort);
    }

    return baseInfo;
  }
}
