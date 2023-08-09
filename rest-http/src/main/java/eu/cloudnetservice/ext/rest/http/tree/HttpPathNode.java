package eu.cloudnetservice.ext.rest.http.tree;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface HttpPathNode extends Comparable<HttpPathNode> {

  static void validatePathId(@NonNull String candidate) {
    if (candidate.isBlank()) {
      throw new IllegalArgumentException("Empty path parts are not allowed");
    }
  }

  boolean consumesRemainingPath();

  /**
   * Get the path entry that is represented by this node.
   *
   * @return
   */
  @NonNull String pathId();

  boolean anyHandlerRegistered();

  @NonNull List<HttpHandlerConfigPair> handlers();

  @Nullable HttpHandlerConfigPair findHandlerForMethod(@NonNull String method);

  void registerHttpHandler(@NonNull HttpHandler httpHandler, @NonNull HttpHandlerConfig config);

  boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart);
}
