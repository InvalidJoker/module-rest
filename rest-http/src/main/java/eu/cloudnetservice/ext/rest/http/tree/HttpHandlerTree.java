package eu.cloudnetservice.ext.rest.http.tree;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface HttpHandlerTree<N extends HttpPathNode> {

  @NonNull N pathNode();

  @Nullable HttpHandlerTree<N> parentNode();

  @UnmodifiableView
  @NonNull Collection<HttpHandlerTree<N>> children();

  void removeAllChildren();

  void visitFullTree(@NonNull Consumer<HttpHandlerTree<N>> nodeConsumer);

  /**
   * Performs a first level search for a node matching the given filter. Returns null in case no direct child matches
   * the given filter.
   *
   * @param nodeFilter
   * @return
   */
  @Nullable HttpHandlerTree<N> findMatchingChildNode(@NotNull Predicate<HttpHandlerTree<N>> nodeFilter);

  @NonNull HttpHandlerTree<N> registerChildNode(@NonNull N pathNode);

  boolean unregisterChildNode(@NonNull HttpHandlerTree<N> node);
}
