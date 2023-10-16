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
import eu.cloudnetservice.ext.modules.rest.auth.util.PasswordEncryptionUtil;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

// don't make this a record, the constructor of this class should be sealed
public final class DefaultRestUser implements RestUser {

  public static final String PASSWORD_KEY = "password";
  public static final String PASSWORD_SALT_KEY = "salt";

  private final @NonNull String id;
  private final @NonNull Set<String> scopes;
  private final @NonNull Map<String, String> properties;

  private DefaultRestUser(
    @NonNull String id,
    @NonNull Set<String> scopes,
    @NonNull Map<String, String> properties
  ) {
    this.id = id;
    this.scopes = scopes;
    this.properties = properties;
  }

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
    return this.scopes.contains(RestUser.GLOBAL_ADMIN_SCOPE) || this.scopes.contains(StringUtil.toLower(scope));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<String> scopes() {
    return this.scopes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String id() {
    return this.id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, String> properties() {
    return this.properties;
  }

  public static final class Builder implements RestUser.Builder {

    private String id;
    private Set<String> scopes = new HashSet<>();
    private Map<String, String> properties = new HashMap<>();

    @Override
    public @NonNull Builder id(@NonNull String id) {
      this.id = id;
      return this;
    }

    public @NonNull Builder password(@NonNull String password) {
      var hashedPasswordInfo = PasswordEncryptionUtil.encrypt(password);
      this.properties.put(PASSWORD_KEY, hashedPasswordInfo.second());
      this.properties.put(PASSWORD_SALT_KEY, hashedPasswordInfo.first());
      return this;
    }

    @Override
    public @NonNull Builder property(@NonNull String key, @Nullable String value) {
      if (value != null) {
        this.properties.put(key, value);
      }
      return this;
    }

    @Override
    public @NonNull Builder properties(@NonNull Map<String, String> properties) {
      this.properties = new HashMap<>(properties);
      return this;
    }

    @Override
    public @NonNull Builder modifyProperties(@NonNull Consumer<Map<String, String>> modifier) {
      modifier.accept(this.properties);
      return this;
    }

    @Override
    public @NonNull Builder scope(@NonNull String scope) {
      this.validateScope(scope);
      this.scopes.add(scope);
      return this;
    }

    @Override
    public @NonNull Builder scopes(@NonNull Collection<String> scopes) {
      this.modifyScopes(currentScopes -> {
        currentScopes.clear();
        currentScopes.addAll(scopes);
      });
      return this;
    }

    @Override
    public @NonNull Builder modifyScopes(@NonNull Consumer<Collection<String>> modifier) {
      // copies the scope two times to validate that all scopes names are correct and to prevent
      //   1. mutation of the builder and then throwing an exception because of an illegal scope name (1. copy)
      //   2. mutation of the scope collection after checking that all names are valid (2. copy)
      var modifiableScopes = new HashSet<>(this.scopes);
      modifier.accept(modifiableScopes);

      modifiableScopes.forEach(this::validateScope);
      this.scopes = new HashSet<>(modifiableScopes);
      return this;
    }

    @Override
    public @NonNull RestUser build() {
      Preconditions.checkNotNull(this.id, "Missing rest user id");
      Preconditions.checkArgument(this.properties.containsKey(PASSWORD_KEY), "Missing rest user password");
      Preconditions.checkArgument(this.properties.containsKey(PASSWORD_SALT_KEY), "Missing rest user salt key");

      return new DefaultRestUser(this.id, Set.copyOf(this.scopes), Map.copyOf(this.properties));
    }

    private void validateScope(@Nullable String scope) {
      if (scope == null) {
        // might be the case when someone adds null into the scopes set during modification
        throw new IllegalArgumentException("Scope name cannot be null");
      }

      var scopeNameMatcher = RestUser.SCOPE_NAMING_PATTERN.matcher(scope);
      if (!scopeNameMatcher.matches()) {
        throw new IllegalArgumentException(String.format(
          "Scope %s does not fulfil the scope naming requirements (Regex: %s)",
          scope,
          RestUser.SCOPE_NAMING_PATTERN.pattern()));
      }
    }
  }
}
