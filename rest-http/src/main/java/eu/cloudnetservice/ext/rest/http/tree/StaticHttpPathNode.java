package eu.cloudnetservice.ext.rest.http.tree;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public final class StaticHttpPathNode extends DefaultHttpPathNode {

  public StaticHttpPathNode(@NonNull String pathId) {
    super(pathId);
  }

  @Override
  public boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart) {
    return this.pathId.equalsIgnoreCase(pathPart);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StaticHttpPathNode that)) {
      return false;
    }
    return this.pathId.equals(that.pathId);
  }

  @Override
  public int hashCode() {
    return this.pathId.hashCode();
  }

  @Override
  @SuppressWarnings("ComparatorMethodParameterNotUsed")
  public int compareTo(@NotNull HttpPathNode other) {
    return other instanceof StaticHttpPathNode ? 0 : -1;
  }
}
