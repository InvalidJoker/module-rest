package eu.cloudnetservice.ext.rest.http.connection;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;

@FunctionalInterface
public interface HttpConnectionInfoResolver {

  @NonNull BasicHttpConnectionInfo extractConnectionInfo(
    @NonNull HttpContext context,
    @NonNull BasicHttpConnectionInfo baseInfo);

  @CheckReturnValue
  default @NonNull HttpConnectionInfoResolver then(@NonNull HttpConnectionInfoResolver next) {
    return (context, baseInfo) -> {
      var replacedBaseInfo = this.extractConnectionInfo(context, baseInfo);
      return next.extractConnectionInfo(context, replacedBaseInfo);
    };
  }
}
