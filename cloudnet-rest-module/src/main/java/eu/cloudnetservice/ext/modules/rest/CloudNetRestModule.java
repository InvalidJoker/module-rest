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
public class CloudNetRestModule extends DriverModule {

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
  public void init(@NonNull InjectionLayer<?> layer) {
    var config = ComponentConfig.builder()
      .haProxyMode(HttpProxyMode.AUTO_DETECT)
      .corsConfig(CorsConfig.builder()
        .addAllowedOrigin("*")
        .addAllowedHeader("*")
        .build()).build();

    var componentFactory = HttpComponentFactoryLoader.getFirstComponentFactory(HttpServer.class);
    var server = componentFactory.construct(config);

    server.addListener(1870);
    server.annotationParser()
      .registerHandlerContextDecorator(ValidationHandlerMethodContextDecorator.withDefaultValidator());

    this.parseAndRegister(
      layer,
      server,
      V2HttpHandlerAuthorization.class,
      V2HttpHandlerCluster.class,
      V2HttpHandlerDatabase.class,
      V2HttpHandlerDocumentation.class,
      V2HttpHandlerGroup.class,
      V2HttpHandlerModule.class,
      V2HttpHandlerNode.class,
      V2HttpHandlerService.class,
      V2HttpHandlerServiceVersion.class,
      V2HttpHandlerTask.class,
      V2HttpHandlerTemplate.class,
      V2HttpHandlerTemplateStorage.class);
  }

  private void parseAndRegister(
    @NonNull InjectionLayer<?> layer,
    @NonNull HttpServer server,
    @NonNull Class<?>... handlers
  ) {
    for (var handlerType : handlers) {
      server.annotationParser().parseAndRegister(layer.instance(handlerType));
    }
  }
}
