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

package eu.cloudnetservice.ext.modules.rest.auth;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;

public record DefaultRestUser(
  @NonNull String id,
  @NonNull Set<String> scopes,
  @NonNull Map<String, String> properties
) implements RestUser {

  private static final String PASSWORD_KEY = "password";
  private static final String PASSWORD_SALT_KEY = "salt";

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull RestUser user) {
    return builder()
      .id(user.id())
      .scopes(user.scopes())
      .properties(user.properties());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasScope(@NonNull String scope) {
    return this.scopes.contains("admin") || this.scopes.contains(StringUtil.toLower(scope));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<String> scopes() {
    return Collections.unmodifiableSet(this.scopes);
  }

  /**
   * {@inheritDoc}
   */
  public static final class Builder {

    private String id;
    private Map<String, String> properties;
    private final Set<String> scopes = new HashSet<>();

    public @NonNull Builder id(@NonNull String id) {
      this.id = id;
      return this;
    }

    public @NonNull Builder password(@NonNull String password) {
      var hashedPasswordInfo = EncryptionUtil.encrypt(password);
      this.properties.put(PASSWORD_KEY, hashedPasswordInfo.second());
      this.properties.put(PASSWORD_SALT_KEY, hashedPasswordInfo.first());
      return this;
    }

    public @NonNull Builder properties(@NonNull Map<String, String> properties) {
      this.properties = new HashMap<>(properties);
      return this;
    }

    public @NonNull Builder modifyProperties(@NonNull Consumer<Map<String, String>> propertiesConsumer) {
      propertiesConsumer.accept(this.properties);
      return this;
    }

    public @NonNull Builder addScope(@NonNull String scope) {
      var matcher = RestUserManagement.SCOPE_NAMING_PATTERN.matcher(scope);
      if (scope.equals("admin") || matcher.matches()) {
        this.scopes.add(StringUtil.toLower(scope));
      } else {
        throw new IllegalArgumentException(String.format(
          "The given scope %s does not match the desired scope regex %s",
          scope,
          RestUserManagement.SCOPE_NAMING_REGEX));
      }

      return this;
    }

    public @NonNull Builder removeScope(@NonNull String scope) {
      this.scopes.remove(StringUtil.toLower(scope));
      return this;
    }

    public @NonNull Builder scopes(@NonNull Set<String> scopes) {
      this.scopes.clear();
      for (var scope : scopes) {
        this.addScope(scope);
      }

      return this;
    }

    public @NonNull RestUser build() {
      Preconditions.checkNotNull(this.id, "Missing rest user id");
      Preconditions.checkArgument(this.properties.containsKey(PASSWORD_KEY), "Missing rest user password");
      Preconditions.checkArgument(this.properties.containsKey(PASSWORD_SALT_KEY), "Missing rest user salt key");

      return new DefaultRestUser(this.id, Set.copyOf(this.scopes), Map.copyOf(this.properties));
    }
  }
}
