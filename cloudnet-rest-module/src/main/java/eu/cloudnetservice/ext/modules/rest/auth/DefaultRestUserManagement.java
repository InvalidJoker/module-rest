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

package eu.cloudnetservice.ext.modules.rest.auth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import java.time.Duration;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultRestUserManagement implements RestUserManagement {

  private static final String REST_USER_DB_NAME = "cloudnet_rest_users";

  private final LocalDatabase localDatabase;
  private final LoadingCache<String, RestUser> restUserCache;

  public DefaultRestUserManagement() {
    this(InjectionLayer.ext().instance(NodeDatabaseProvider.class));
  }

  /**
   * Creates the default user management and initializes the used database.
   *
   * @param databaseProvider the node database provider to use to create the user database.
   */
  public DefaultRestUserManagement(@NonNull NodeDatabaseProvider databaseProvider) {
    this.localDatabase = databaseProvider.database(REST_USER_DB_NAME);
    this.restUserCache = Caffeine.newBuilder()
      .scheduler(Scheduler.systemScheduler())
      .expireAfterWrite(Duration.ofMinutes(5))
      .build(key -> {
        var userDocument = this.localDatabase.get(key);
        return userDocument == null ? null : userDocument.toInstanceOf(DefaultRestUser.class);
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable RestUser restUser(@NonNull String id) {
    return this.restUserCache.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveRestUser(@NonNull RestUser user) {
    this.restUserCache.invalidate(user.id());
    this.localDatabase.insert(user.id(), DocumentFactory.json().newDocument(user));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteRestUser(@NonNull String id) {
    this.restUserCache.invalidate(id);
    return this.localDatabase.delete(id);
  }

  @Override
  public @NonNull RestUser.Builder builder() {
    return new DefaultRestUser.Builder();
  }

  @Override
  public @NonNull RestUser.Builder builder(@NonNull RestUser restUser) {
    return this.builder()
      .id(restUser.id())
      .scopes(restUser.scopes())
      .properties(restUser.properties());
  }
}
