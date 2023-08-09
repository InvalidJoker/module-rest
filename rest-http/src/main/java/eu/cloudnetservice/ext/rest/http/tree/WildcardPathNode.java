package eu.cloudnetservice.ext.rest.http.tree;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public final class WildcardPathNode extends DefaultHttpPathNode {

  public WildcardPathNode() {
    super("*");
  }

  @Override
  public boolean consumesRemainingPath() {
    return true;
  }

  @Override
  public boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart) {
    return true;
  }

  @Override
  @SuppressWarnings("ComparatorMethodParameterNotUsed")
  public int compareTo(@NotNull HttpPathNode other) {
    return other instanceof WildcardPathNode ? 0 : 1;
  }
}
