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

package eu.cloudnetservice.ext.rest.http.connection;

import eu.cloudnetservice.ext.rest.http.util.HostAndPort;
import java.util.Locale;
import lombok.NonNull;

public record BasicHttpConnectionInfo(
  @NonNull String scheme,
  @NonNull HostAndPort hostAddress,
  @NonNull HostAndPort clientAddress
) {

  public BasicHttpConnectionInfo {
    scheme = scheme.trim().toLowerCase(Locale.ROOT);
  }

  public @NonNull BasicHttpConnectionInfo withScheme(@NonNull String scheme) {
    return new BasicHttpConnectionInfo(scheme, this.hostAddress, this.clientAddress);
  }

  public @NonNull BasicHttpConnectionInfo withHostAddress(@NonNull HostAndPort hostAddress) {
    return new BasicHttpConnectionInfo(this.scheme, hostAddress, this.clientAddress);
  }

  public @NonNull BasicHttpConnectionInfo withClientAddress(@NonNull HostAndPort clientAddress) {
    return new BasicHttpConnectionInfo(this.scheme, this.hostAddress, clientAddress);
  }

  public int defaultPortForScheme() {
    if (this.scheme.equals("http") || this.scheme.equals("ws")) {
      return 80;
    } else {
      return 443;
    }
  }
}
