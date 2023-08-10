package eu.cloudnetservice.ext.rest.http.connection.parse;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.network.HostAndPort;
import lombok.NonNull;

final class AddressParseUtil {

  private AddressParseUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull HostAndPort parseHostAndPort(
    @NonNull String headerName,
    @NonNull String address,
    int defaultPort
  ) {
    // ensure that the input address is not prefixed/suffixed by any spaces
    address = address.trim();

    var host = address;
    var port = defaultPort;

    var portSeparatorIdx = address.lastIndexOf(':');
    var ipv6SeparatorIdx = address.lastIndexOf(']');

    if (ipv6SeparatorIdx != -1) {
      // ipv6 address
      host = address.substring(1, ipv6SeparatorIdx);

      // ensure that the port index is after the ipv6 closing bracket and not inside it
      if (portSeparatorIdx != -1 && portSeparatorIdx > ipv6SeparatorIdx) {
        port = parsePortNumber(headerName, address, portSeparatorIdx);
      }
    } else if (portSeparatorIdx != -1) {
      // ipv4 address
      host = address.substring(0, portSeparatorIdx);
      port = parsePortNumber(headerName, address, portSeparatorIdx);
    }

    return new HostAndPort(host, port);
  }

  private static int parsePortNumber(@NonNull String headerName, @NonNull String address, int separatorIndex) {
    var portPart = address.substring(separatorIndex + 1);
    return parsePortNumber(headerName, portPart);
  }

  public static int parsePortNumber(@NonNull String headerName, @NonNull String portPart) {
    var parsedPortNumber = Ints.tryParse(portPart.trim());
    Preconditions.checkArgument(
      parsedPortNumber != null && parsedPortNumber >= 0 && parsedPortNumber <= 0xFFFF,
      "Invalid port number '%d' supplied by proxy forward header '%s'",
      portPart, headerName
    );

    return parsedPortNumber;
  }
}
