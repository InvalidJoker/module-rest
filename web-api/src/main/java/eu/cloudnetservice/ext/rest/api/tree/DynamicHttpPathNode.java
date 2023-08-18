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

package eu.cloudnetservice.ext.rest.api.tree;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import lombok.NonNull;

public final class DynamicHttpPathNode extends DefaultHttpPathNode {

  public DynamicHttpPathNode(@NonNull String pathId) {
    super(pathId);
  }

  @Override
  public @NonNull String displayName() {
    return '{' + this.pathId + '}';
  }

  @Override
  public void unregisterPathPart(@NonNull HttpContext httpContext) {
    httpContext.request().pathParameters().remove(this.pathId);
  }

  @Override
  public boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart) {
    // todo: maybe allow some kind of validation logic to be passed into this?
    context.request().pathParameters().put(this.pathId, pathPart);
    return true;
  }

  @Override
  public int compareTo(@NonNull HttpPathNode other) {
    if (other instanceof DynamicHttpPathNode) {
      return 0;
    }

    if (other instanceof StaticHttpPathNode) {
      return 1;
    }

    return -1;
  }
}
