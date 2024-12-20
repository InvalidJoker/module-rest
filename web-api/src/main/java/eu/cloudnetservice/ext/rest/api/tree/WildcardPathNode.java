/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

public final class WildcardPathNode extends DefaultHttpPathNode {

  public WildcardPathNode() {
    super("*");
  }

  @Override
  public boolean consumesRemainingPath() {
    return true;
  }

  @Override
  public @NonNull String displayName() {
    return "*";
  }

  @Override
  public void unregisterPathPart(@NonNull HttpContext httpContext) {
  }

  @Override
  public boolean validateAndRegisterPathPart(@NonNull HttpContext context, @NonNull String pathPart) {
    return true;
  }

  @Override
  @SuppressWarnings("ComparatorMethodParameterNotUsed")
  public int compareTo(@NonNull HttpPathNode other) {
    return other instanceof WildcardPathNode ? 0 : 1;
  }
}
