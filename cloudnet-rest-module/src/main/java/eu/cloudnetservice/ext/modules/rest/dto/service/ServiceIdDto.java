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

package eu.cloudnetservice.ext.modules.rest.dto.service;

import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.ext.modules.rest.dto.Dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;

public class ServiceIdDto implements Dto<ServiceId.Builder>  {

  @NotNull
  protected final String taskName;
  @NotNull
  @Pattern(regexp = ServiceTask.NAMING_REGEX)
  protected final String nameSplitter;
  @NotNull
  protected final Set<String> allowedNodes;

  @NotNull
  protected final UUID uniqueId;
  @PositiveOrZero
  protected final int taskServiceId;
  protected final String nodeUniqueId;

  @NotNull
  protected final String environmentName;
  protected final ServiceEnvironmentType environment;

  public ServiceIdDto(
    String taskName,
    String nameSplitter,
    Set<String> allowedNodes,
    UUID uniqueId,
    int taskServiceId,
    String nodeUniqueId,
    String environmentName,
    ServiceEnvironmentType environment
  ) {
    this.taskName = taskName;
    this.nameSplitter = nameSplitter;
    this.allowedNodes = allowedNodes;
    this.uniqueId = uniqueId;
    this.taskServiceId = taskServiceId;
    this.nodeUniqueId = nodeUniqueId;
    this.environmentName = environmentName;
    this.environment = environment;
  }

  public @NonNull ServiceId.Builder original() {
    return ServiceId.builder()
      .taskName(this.taskName)
      .nameSplitter(this.nameSplitter)
      .allowedNodes(this.allowedNodes)
      .uniqueId(this.uniqueId)
      .taskServiceId(this.taskServiceId)
      .nodeUniqueId(this.nodeUniqueId)
      .environment(this.environmentName)
      .environment(this.environment);
  }
}
