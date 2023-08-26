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

package eu.cloudnetservice.ext.rest.api.auth;

import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

@ApiStatus.Experimental
public interface RestUser {

  /**
   * Gets the id of this rest user.
   *
   * @return the id of this rest user.
   */
  @NonNull String id();

  @NonNull Map<String, String> properties();

  /**
   * Checks whether the user has the given scope.
   *
   * @param scope the scope to check.
   * @return true if the rest user has that scope assigned, false otherwise.
   * @throws NullPointerException if the given scope is null.
   */
  boolean hasScope(@NonNull String scope);

  /**
   * Checks whether the user has at least one of the given scopes.
   *
   * @param scopes the scopes to check.
   * @return true if the user has at least one of the given scopes.
   * @throws NullPointerException if the given scopes array is null.
   */
  default boolean hasOneScopeOf(@NonNull String[] scopes) {
    for (var scope : scopes) {
      if (this.hasScope(scope)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets an unmodifiable view of the scopes the rest user has.
   *
   * @return an unmodifiable view of the scopes the rest user has.
   */
  @Unmodifiable
  @NonNull Set<String> scopes();
}
