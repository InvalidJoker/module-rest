package eu.cloudnetservice.ext.rest.http.connection;

import eu.cloudnetservice.driver.network.HostAndPort;
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
