package eu.cloudnetservice.ext.rest.http.tree;

import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

abstract class DefaultHttpPathNode implements HttpPathNode {

  protected final String pathId;
  protected final List<HttpHandlerConfigPair> handlers;

  DefaultHttpPathNode(@NonNull String pathId) {
    this.pathId = pathId;
    this.handlers = new ArrayList<>();
  }

  @Override
  public boolean consumesRemainingPath() {
    return false;
  }

  @Override
  public @NonNull String pathId() {
    return this.pathId;
  }

  @Override
  public boolean anyHandlerRegistered() {
    return !this.handlers.isEmpty();
  }

  @Override
  public @NonNull List<HttpHandlerConfigPair> handlers() {
    return this.handlers;
  }

  @Override
  public @Nullable HttpHandlerConfigPair findHandlerForMethod(@NonNull String method) {
    return this.handlers.stream()
      .filter(pair -> pair.config().httpMethod().equalsIgnoreCase(method))
      .findFirst()
      .orElse(null);
  }

  @Override
  public void registerHttpHandler(@NonNull HttpHandler httpHandler, @NonNull HttpHandlerConfig config) {
    this.handlers.add(new HttpHandlerConfigPair(httpHandler, config));
  }
}
