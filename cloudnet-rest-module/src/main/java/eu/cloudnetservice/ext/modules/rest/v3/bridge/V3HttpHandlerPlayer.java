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

package eu.cloudnetservice.ext.modules.rest.v3.bridge;

import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.registry.injection.Service;
import eu.cloudnetservice.ext.modules.rest.page.PageSortingMode;
import eu.cloudnetservice.ext.modules.rest.page.Paging;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.validation.EnableValidation;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import java.util.Map;

@Singleton
@EnableValidation
public class V3HttpHandlerPlayer {

  private final PlayerManager playerManager;

  @Inject
  public V3HttpHandlerPlayer(@Service PlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  @RequestHandler(path = "/api/v3/player/onlineCount")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read", "cloudnet_bridge:player_online_count"})
  public IntoResponse<?> handleOnlineCount() {
    return JsonResponse.builder().body(Map.of("onlineCount", this.playerManager.onlineCount()));
  }

  @RequestHandler(path = "/api/v3/player/registeredCount")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read",
    "cloudnet_bridge:player_registered_count"})
  public IntoResponse<?> handleRegisteredCount() {
    return JsonResponse.builder().body(Map.of("registeredCount", this.playerManager.onlineCount()));
  }

  @RequestHandler(path = "/api/v3/player/online")
  @Authentication(providers = "jwt", scopes = {"cloudnet_bridge:player_read", "cloudnet_bridge:player_get_bulk"})
  public IntoResponse<?> handleOnlinePlayerList(
    @Optional @Valid @FirstRequestQueryParam(value = "limit", def = "10") String limit,
    @Optional @Valid @FirstRequestQueryParam(value = "offset", def = "0") String offset,
    @Optional @FirstRequestQueryParam(value = "sort", def = "asc") String sort
  ) {
    var limitInt = Ints.tryParse(limit);
    if (limitInt == null || limitInt < 1 || limitInt > 25) {
      return ProblemDetail.builder()
        .type("malformed-limit-parameter")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Limit Query Parameter")
        .detail("The provided limit query parameter is not a valid integer.");
    }

    var offsetInt = Ints.tryParse(offset);
    if (offsetInt == null || offsetInt < 0) {
      return ProblemDetail.builder()
        .type("malformed-offset-parameter")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Offset Query Parameter")
        .detail("The provided offset query parameter is not a valid integer.");
    }

    playerManager.globalPlayerExecutor().

    var sortingMode = PageSortingMode.findSortingMode(sort);
    if (sortingMode == null) {
      return ProblemDetail.builder()
        .type("malformed-sort-parameter")
        .status(HttpResponseCode.BAD_REQUEST)
        .title("Malformed Sort Query Parameter")
        .detail("The provided sort query parameter '%s' is unknown".formatted(sort));
    }

    // create the correct slice for our data based on the input
    return Paging.pagedJsonResponse(
      this.playerManager.onlinePlayers().players(),
      CloudOfflinePlayer::name,
      sortingMode,
      limitInt,
      offsetInt);
  }

}
