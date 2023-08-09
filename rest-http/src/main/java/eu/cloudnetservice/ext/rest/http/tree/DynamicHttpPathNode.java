package eu.cloudnetservice.ext.rest.http.tree;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public final class DynamicHttpPathNode extends DefaultHttpPathNode {

  public DynamicHttpPathNode(@NonNull String pathId) {
    super(pathId);
  }

  @Override
  public boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart) {
    // todo: maybe allow some kind of validation logic to be passed into this?
    context.request().pathParameters().put(this.pathId, pathPart);
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DynamicHttpPathNode that)) {
      return false;
    }
    return this.pathId.equals(that.pathId);
  }

  @Override
  public int hashCode() {
    return this.pathId.hashCode();
  }

  @Override
  public int compareTo(@NotNull HttpPathNode other) {
    if (other instanceof DynamicHttpPathNode) {
      return 0;
    }

    if (other instanceof StaticHttpPathNode) {
      return 1;
    }

    return -1;
  }
}
