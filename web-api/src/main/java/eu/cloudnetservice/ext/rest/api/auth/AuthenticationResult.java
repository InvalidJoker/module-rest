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

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

public final class AuthenticationResult {

  private static final AuthenticationResult EMPTY = new AuthenticationResult(null, null);
  private static final AuthenticationResult USER_NOT_FOUND = new AuthenticationResult(State.USER_NOT_FOUND, null);
  private static final AuthenticationResult INVALID_CREDENTIALS = new AuthenticationResult(
    State.INVALID_CREDENTIALS,
    null);

  private final State state;
  private final RestUser user;

  private AuthenticationResult(@UnknownNullability State state, @UnknownNullability RestUser user) {
    this.state = state;
    this.user = user;
  }

  public static @NonNull AuthenticationResult proceed() {
    return EMPTY;
  }

  public static @NonNull AuthenticationResult userNotFound() {
    return USER_NOT_FOUND;
  }

  public static @NonNull AuthenticationResult invalidCredentials() {
    return INVALID_CREDENTIALS;
  }

  public boolean empty() {
    return this == EMPTY;
  }

  public boolean ok() {
    return this != EMPTY && this.state == State.OK;
  }

  public @NonNull State state() {
    Preconditions.checkArgument(this != EMPTY, "State is only present for non-empty results.");

    return this.state;
  }

  public @NonNull RestUser user() {
    Preconditions.checkArgument(this.state == State.OK, "User is only present on OK state.");

    return this.user;
  }


  public enum State {

    OK,
    USER_NOT_FOUND,
    INVALID_CREDENTIALS

  }
}
