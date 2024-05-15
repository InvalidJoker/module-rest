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

package eu.cloudnetservice.ext.modules.rest;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.ext.modules.rest.auth.DefaultRestUser;
import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.api.auth.AuthProviderLoader;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.auth.RestUserManagement;
import eu.cloudnetservice.ext.rest.api.auth.basic.BasicAuthProvider;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;

@Singleton
@CommandPermission("cloudnet.command.rest")
@Description("module-rest-command-description")
public final class RestCommand {

  private final AuthProvider<?> authProvider;
  private final RestUserManagement restUserManagement;

  @Inject
  public RestCommand(@NonNull RestUserManagement restUserManagement) {
    this.restUserManagement = restUserManagement;
    this.authProvider = AuthProviderLoader.resolveAuthProvider("basic");
  }

  @Parser
  public @NonNull DefaultRestUser defaultRestUserParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var userName = input.remove();
    var user = this.restUserManagement.restUser(userName);
    if (user == null) {
      throw new ArgumentNotAvailableException(I18n.trans("module-rest-user-not-found", userName));
    }

    // this implementation is based on our rest user implementation
    if (user instanceof DefaultRestUser defaultRestUser) {
      return defaultRestUser;
    }

    throw new ArgumentNotAvailableException(I18n.trans(
      "module-rest-unknown-user-type",
      user.getClass().getSimpleName()));
  }

  @Parser(name = "restUserScope")
  public @NonNull String restUserScopeParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var scope = input.remove();
    if (scope.equalsIgnoreCase("admin") || RestUser.SCOPE_NAMING_PATTERN.matcher(scope).matches()) {
      return scope;
    }

    throw new ArgumentNotAvailableException(I18n.trans(
      "argument-parse-failure-regex",
      RestUser.SCOPE_NAMING_PATTERN.pattern(),
      scope));
  }

  @CommandMethod("rest user create <id> <password>")
  public void createRestUser(
    @NonNull CommandSource source,
    @Argument("id") @NonNull String userId,
    @Argument("password") @NonNull String password
  ) {
    if (this.restUserManagement.restUser(userId) != null) {
      source.sendMessage(I18n.trans("module-rest-user-already-existing", userId));
      return;
    }

    // use the default rest user here as it provides a password setter
    var user = DefaultRestUser.builder().id(userId).password(password).build();
    this.restUserManagement.saveRestUser(user);

    source.sendMessage(I18n.trans("module-rest-user-create-successful", userId));
  }

  @CommandMethod("rest user delete <id>")
  public void deleteRestUser(@NonNull CommandSource source, @Argument("id") @NonNull DefaultRestUser restUser) {
    this.restUserManagement.deleteRestUser(restUser);
    source.sendMessage(I18n.trans("module-rest-user-delete-successful", restUser.id()));
  }

  @CommandMethod("rest user <id>")
  public void displayUser(@NonNull CommandSource source, @Argument("id") @NonNull DefaultRestUser restUser) {
    source.sendMessage("RestUser " + restUser.id());
    source.sendMessage("Scopes:");
    for (var scope : restUser.scopes()) {
      source.sendMessage(" - " + scope);
    }
  }

  @CommandMethod("rest user <id> add scope <scope>")
  public void addScope(
    @NonNull CommandSource source,
    @Argument("id") @NonNull DefaultRestUser restUser,
    @Argument(value = "scope", parserName = "restUserScope") @NonNull String scope
  ) {
    this.updateRestUser(restUser, builder -> builder.scope(scope));
    source.sendMessage(I18n.trans("module-rest-user-add-scope-successful", restUser.id(), scope));
  }

  @CommandMethod("rest user <id> clear scopes")
  public void clearScopes(@NonNull CommandSource source, @Argument("id") @NonNull DefaultRestUser restUser) {
    this.updateRestUser(restUser, builder -> builder.scopes(Set.of()));
    source.sendMessage(I18n.trans("module-rest-user-clear-scopes-successful", restUser.id()));
  }

  @CommandMethod("rest user <id> remove scope <scope>")
  public void removeScope(
    @NonNull CommandSource source,
    @Argument("id") @NonNull DefaultRestUser restUser,
    @Argument("scope") @NonNull String scope
  ) {
    this.updateRestUser(restUser, builder -> builder.modifyScopes(scopes -> scopes.remove(scope)));
    source.sendMessage(I18n.trans("module-rest-user-remove-scope-successful", restUser.id(), scope));
  }

  @CommandMethod("rest user <id> set password <password>")
  public void setPassword(
    @NonNull CommandSource source,
    @Argument("id") @NonNull DefaultRestUser restUser,
    @Argument("password") @NonNull String password
  ) {
    this.updateRestUser(restUser, builder -> builder.password(password));
    source.sendMessage(I18n.trans("module-rest-user-password-changed", restUser.id()));
  }

  @CommandMethod("rest user <id> verifyPassword <password>")
  public void verifyPassword(
    @NonNull CommandSource source,
    @Argument("id") @NonNull DefaultRestUser restUser,
    @Argument("password") @NonNull String password
  ) {
    if (this.authProvider instanceof BasicAuthProvider basicAuthProvider) {
      var valid = basicAuthProvider.validatePassword(restUser, password.getBytes(StandardCharsets.UTF_8));
      if (valid) {
        source.sendMessage(I18n.trans("module-rest-user-password-match", restUser.id()));
      } else {
        source.sendMessage(I18n.trans("module-rest-user-password-mismatch", restUser.id()));
      }
    } else {
      source.sendMessage(I18n.trans("module-rest-user-verify-basic-auth-provider-missing"));
    }
  }

  private void updateRestUser(@NonNull DefaultRestUser user, @NonNull Consumer<DefaultRestUser.Builder> consumer) {
    var builder = DefaultRestUser.builder(user);
    consumer.accept(builder);
    this.restUserManagement.saveRestUser(builder.build());
  }
}
