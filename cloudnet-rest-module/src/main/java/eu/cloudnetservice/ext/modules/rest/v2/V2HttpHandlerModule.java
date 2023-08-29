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

package eu.cloudnetservice.ext.modules.rest.v2;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.RequestBody;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;

@Singleton
public final class V2HttpHandlerModule {

  private final ModuleProvider moduleProvider;

  public V2HttpHandlerModule(@NonNull ModuleProvider moduleProvider) {
    this.moduleProvider = moduleProvider;
  }

  // TODO docs: request method changed
  @RequestHandler(path = "/api/v2/module/reload", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_reload_all"})
  public @NonNull IntoResponse<?> handleModuleReloadRequest() {
    this.moduleProvider.reloadAll();
    return JsonResponse.builder().noContent();
  }

  @RequestHandler(path = "/api/v2/module")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_list"})
  public @NonNull IntoResponse<?> handleModuleListRequest() {
    var modules = this.moduleProvider.modules().stream().map(this::constructModuleInformation).toList();
    return JsonResponse.builder().body(Map.of("modules", modules));
  }

  @RequestHandler(path = "/api/v2/module/{module}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_get"})
  public @NonNull IntoResponse<?> handleModuleGetRequest(@NonNull @RequestPathParam("module") String name) {
    return this.handleModuleContext(
      name,
      module -> JsonResponse.builder().body(this.constructModuleInformation(module)));
  }

  // TODO: reconsider if we should merge this into one route like service has

  // TODO docs: request method changed
  @RequestHandler(path = "/api/v2/module/{module}/reload", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_reload"})
  public @NonNull IntoResponse<?> handleModuleReloadRequest(@NonNull @RequestPathParam("module") String name) {
    return this.handleModuleContext(name, module -> {
      module.reloadModule();
      return JsonResponse.builder().noContent();
    });
  }

  // TODO docs: request method changed
  @RequestHandler(path = "/api/v2/module/{module}/unload", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_unload"})
  public @NonNull IntoResponse<?> handleModuleUnloadRequest(@NonNull @RequestPathParam("module") String name) {
    return this.handleModuleContext(name, module -> {
      module.unloadModule();
      return JsonResponse.builder().noContent();
    });
  }

  // TODO docs: this route is completely new
  @RequestHandler(path = "/api/v2/module/{module}/start", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_start"})
  public @NonNull IntoResponse<?> handleModuleStartRequest(@NonNull @RequestPathParam("module") String name) {
    return this.handleModuleContext(name, module -> {
      module.startModule();
      return JsonResponse.builder().noContent();
    });
  }

  // TODO docs: this route is completely new
  @RequestHandler(path = "/api/v2/module/{module}/stop", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_stop"})
  public @NonNull IntoResponse<?> handleModuleStopRequest(@NonNull @RequestPathParam("module") String name) {
    return this.handleModuleContext(name, module -> {
      module.stopModule();
      return JsonResponse.builder().noContent();
    });
  }

  // TODO docs: this route is completely new
  @RequestHandler(path = "/api/v2/module/{module}/uninstall", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_uninstall"})
  public @NonNull IntoResponse<?> handleModuleUninstallRequest(@NonNull @RequestPathParam("module") String name) {
    return this.handleModuleContext(name, module -> {
      module.stopModule();
      module.unloadModule();

      try {
        Files.delete(Path.of(module.uri()));
      } catch (IOException exception) {
        return ProblemDetail.builder()
          .type("module-uninstall-failed")
          .title("Module Uninstall Failed")
          .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
          .detail(String.format("Uninstalling module %s failed due to internal I/O error.", name));
      }

      return JsonResponse.builder().noContent();
    });
  }

  @RequestHandler(path = "/api/v2/module/{module}/load", method = HttpMethod.PUT)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_load"})
  public @NonNull IntoResponse<?> handleModuleLoadRequest(
    @NonNull @RequestPathParam("module") String name,
    @NonNull @RequestBody InputStream body
  ) {
    var moduleTarget = this.moduleProvider.moduleDirectoryPath().resolve(name);
    FileUtil.ensureChild(this.moduleProvider.moduleDirectoryPath(), moduleTarget);

    try (var outputStream = Files.newOutputStream(moduleTarget)) {
      FileUtil.copy(body, outputStream);
    } catch (IOException exception) {
      FileUtil.delete(moduleTarget);
      return ProblemDetail.builder()
        .type("module-load-failed-io")
        .title("Module Load Failed I/O")
        .status(HttpResponseCode.INTERNAL_SERVER_ERROR)
        .detail(String.format("Loading module %s failed due to internal I/O error.", name));
    }

    var module = this.moduleProvider.loadModule(moduleTarget);
    if (module == null) {
      return ProblemDetail.builder()
        .type("module-load-failed")
        .title("Module Load Failed")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail(String.format("Loading module %s failed. Check the console for more information.", name));
    }

    return JsonResponse.builder().responseCode(HttpResponseCode.CREATED).body(this.constructModuleInformation(module));
  }

  @RequestHandler(path = "/api/v2/module/{module}/config")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_read", "cloudnet_rest:module_config_get"})
  public @NonNull IntoResponse<?> handleModuleConfigRequest(@NonNull @RequestPathParam("module") String name) {
    return this.handleModuleContext(name, module -> {
      if (module.module() instanceof DriverModule driverModule) {
        var config = driverModule.readConfig(DocumentFactory.json());
        return JsonResponse.builder().body(Map.of("config", config));
      } else {
        return ProblemDetail.builder()
          .status(HttpResponseCode.BAD_REQUEST)
          .type("module-misses-config-read-support")
          .title("Module Misses Config Read Support")
          .detail(String.format("The requested module %s does not support reading its configuration.", name));
      }
    });
  }

  @RequestHandler(path = "/api/v2/module/{module}/config", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:module_write", "cloudnet_rest:module_config_update"})
  public @NonNull IntoResponse<?> handleModuleConfigRequest(
    @NonNull @RequestPathParam("module") String name,
    @NonNull @RequestBody InputStream body
  ) {
    return this.handleModuleContext(name, module -> {
      if (module.module() instanceof DriverModule driverModule) {
        driverModule.writeConfig(DocumentFactory.json().parse(body));
        return JsonResponse.builder().noContent();
      } else {
        return ProblemDetail.builder()
          .status(HttpResponseCode.BAD_REQUEST)
          .type("module-misses-config-write-support")
          .title("Module Misses Config Write Support")
          .detail(String.format("The requested module %s does not support writing its configuration.", name));
      }
    });
  }

  private @NonNull Map<String, Object> constructModuleInformation(@NonNull ModuleWrapper module) {
    return Map.of("lifecycle", module.moduleLifeCycle(), "configuration", module.moduleConfiguration());
  }

  private @NonNull IntoResponse<?> handleModuleContext(
    @NonNull String name,
    @NonNull Function<ModuleWrapper, IntoResponse<?>> mapper
  ) {
    var module = this.moduleProvider.module(name);
    if (module == null) {
      return ProblemDetail.builder()
        .type("module-not-found")
        .title("Module Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested module %s was not found.", name));
    }

    return mapper.apply(module);
  }
}
