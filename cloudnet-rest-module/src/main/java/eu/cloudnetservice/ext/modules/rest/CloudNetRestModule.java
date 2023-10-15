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

package eu.cloudnetservice.ext.modules.rest;

import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerAuthorization;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerCluster;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerDatabase;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerDocumentation;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerGroup;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerModule;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerNode;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerService;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerServiceVersion;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerTask;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerTemplate;
import eu.cloudnetservice.ext.modules.rest.v2.V2HttpHandlerTemplateStorage;
import eu.cloudnetservice.ext.rest.api.HttpServer;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.CorsConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpProxyMode;
import eu.cloudnetservice.ext.rest.api.factory.HttpComponentFactoryLoader;
import eu.cloudnetservice.ext.rest.validation.ValidationHandlerMethodContextDecorator;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class CloudNetRestModule extends DriverModule {

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STARTED)
  public void initHttpServer(@NonNull InjectionLayer<?> injectionLayer) {
    // todo: configuration
    var config = ComponentConfig.builder()
      .haProxyMode(HttpProxyMode.AUTO_DETECT)
      .corsConfig(CorsConfig.builder()
        .addAllowedOrigin("*")
        .addAllowedHeader("*")
        .build()).build();

    // construct the http server component
    var componentFactory = HttpComponentFactoryLoader.getFirstComponentFactory(HttpServer.class);
    var server = componentFactory.construct(config);

    // registers the validation-enabling context decorator
    var validationDecorator = ValidationHandlerMethodContextDecorator.withDefaultValidator();
    server.annotationParser().registerHandlerContextDecorator(validationDecorator);

    // bind the server and register it for injection
    server.addListener(1870);
    injectionLayer.install(BindingBuilder.create().bind(HttpServer.class).toInstance(server));
  }

  @ModuleTask(order = 107, lifecycle = ModuleLifeCycle.STARTED)
  public void registerHttpHandlers(
    @NonNull HttpServer httpServer,
    @NonNull V2HttpHandlerTask taskHandler,
    @NonNull V2HttpHandlerNode nodeHandler,
    @NonNull V2HttpHandlerGroup groupHandler,
    @NonNull V2HttpHandlerModule moduleHandler,
    @NonNull V2HttpHandlerCluster clusterHandler,
    @NonNull V2HttpHandlerService serviceHandler,
    @NonNull V2HttpHandlerDatabase databaseHandler,
    @NonNull V2HttpHandlerTemplate templateHandler,
    @NonNull V2HttpHandlerServiceVersion versionHandler,
    @NonNull V2HttpHandlerTemplateStorage storageHandler,
    @NonNull V2HttpHandlerAuthorization authorizationHandler,
    @NonNull V2HttpHandlerDocumentation documentationHandler
  ) {
    httpServer.annotationParser()
      .parseAndRegister(taskHandler)
      .parseAndRegister(nodeHandler)
      .parseAndRegister(groupHandler)
      .parseAndRegister(moduleHandler)
      .parseAndRegister(clusterHandler)
      .parseAndRegister(serviceHandler)
      .parseAndRegister(databaseHandler)
      .parseAndRegister(templateHandler)
      .parseAndRegister(versionHandler)
      .parseAndRegister(storageHandler)
      .parseAndRegister(authorizationHandler)
      .parseAndRegister(documentationHandler);
  }
}
