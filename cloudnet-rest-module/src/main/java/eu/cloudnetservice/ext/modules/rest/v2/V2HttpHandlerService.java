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

import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.Authentication;
import eu.cloudnetservice.ext.rest.api.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.ext.rest.api.annotation.Optional;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPathParam;
import eu.cloudnetservice.ext.rest.api.annotation.RequestTypedBody;
import eu.cloudnetservice.ext.rest.api.auth.RestUser;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.JsonResponse;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketChannel;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketFrameType;
import eu.cloudnetservice.ext.rest.api.websocket.WebSocketListener;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.ServiceConsoleLineHandler;
import eu.cloudnetservice.node.service.ServiceConsoleLogCache;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;

@Singleton
public final class V2HttpHandlerService {

  private final CloudServiceFactory serviceFactory;
  private final CloudServiceManager serviceManager;
  private final ServiceTaskProvider serviceTaskProvider;

  @Inject
  public V2HttpHandlerService(
    @NonNull CloudServiceFactory serviceFactory,
    @NonNull CloudServiceManager serviceManager,
    @NonNull ServiceTaskProvider serviceTaskProvider
  ) {
    this.serviceFactory = serviceFactory;
    this.serviceManager = serviceManager;
    this.serviceTaskProvider = serviceTaskProvider;
  }

  @RequestHandler(path = "/api/v2/service")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_list"})
  public @NonNull IntoResponse<?> handleServiceListRequest() {
    return JsonResponse.builder().body(Map.of("services", this.serviceManager.services()));
  }

  @RequestHandler(path = "/api/v2/service/{id}")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_get"})
  public @NonNull IntoResponse<?> handleServiceGetRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleServiceContext(id, service -> JsonResponse.builder().body(Map.of("snapshot", service)));
  }

  @RequestHandler(path = "/api/v2/service/{id}", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_delete"})
  public @NonNull IntoResponse<?> handleServiceDeleteRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleEmptyServiceProviderContext(id, SpecificCloudServiceProvider::delete);
  }

  // TODO docs: this route is new
  @RequestHandler(path = "/api/v2/service/{id}/deleteFiles", method = HttpMethod.DELETE)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_delete_files"})
  public @NonNull IntoResponse<?> handleServiceDeleteFilesRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleEmptyServiceProviderContext(id, SpecificCloudServiceProvider::deleteFiles);
  }

  @RequestHandler(path = "/api/v2/service/{id}/lifecycle", method = HttpMethod.PATCH)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_lifecycle"})
  public @NonNull IntoResponse<?> handleServiceLifecycleRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("target") String lifecycle
  ) {
    return this.handleServiceProviderContext(id, service -> {
      switch (StringUtil.toLower(lifecycle)) {
        case "start" -> service.start();
        case "stop" -> service.stop();
        case "restart" -> service.restart();
        default -> {
          return ProblemDetail.builder()
            .type("invalid-service-lifecycle")
            .title("Invalid Service Lifecycle")
            .status(HttpResponseCode.BAD_REQUEST)
            .detail(String.format(
              "The requested service lifecycle %s does not exist. Supported values are: start, stop, restart",
              lifecycle
            ));
        }
      }

      return JsonResponse.builder().noContent();
    });
  }

  @RequestHandler(path = "/api/v2/service/{id}/command", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_send_commands"})
  public @NonNull IntoResponse<?> handleServiceCommandRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @RequestTypedBody Map<String, String> command
  ) {
    return this.handleServiceProviderContext(id, service -> {
      var commandLine = command.get("command");
      if (commandLine == null) {
        return ProblemDetail.builder()
          .title("Missing Command Line")
          .type("missing-command-line")
          .status(HttpResponseCode.BAD_REQUEST)
          .detail("The request body has no 'command' field.");
      }

      service.runCommand(commandLine);
      return JsonResponse.builder().noContent();
    });
  }

  // TODO docs: request method changed
  @RequestHandler(path = "/api/v2/service/{id}/include", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_include"})
  public @NonNull IntoResponse<?> handleServiceIncludeRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("type") String type
  ) {
    return this.handleServiceProviderContext(id, service -> {
      switch (StringUtil.toLower(type)) {
        case "templates" -> service.includeWaitingServiceTemplates();
        case "inclusions" -> service.includeWaitingServiceInclusions();
        default -> {
          return ProblemDetail.builder()
            .type("invalid-include-type")
            .title("Invalid Include Type")
            .status(HttpResponseCode.BAD_REQUEST)
            .detail(String.format(
              "The requested inclusion type %s does not exist. Supported values are: templates, inclusions",
              type
            ));
        }
      }

      return JsonResponse.builder().noContent();
    });
  }

  // TODO docs: request method changed
  @RequestHandler(path = "/api/v2/service/{id}/deployResources", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_deploy_resources"})
  public @NonNull IntoResponse<?> handleServiceDeployRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam(value = "remove", def = "true") String remove
  ) {
    return this.handleEmptyServiceProviderContext(id, service -> service.deployResources(Boolean.parseBoolean(remove)));
  }

  @RequestHandler(path = "/api/v2/service/{id}/logLines")
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_log_lines"})
  public @NonNull IntoResponse<?> handleServiceLogRequest(@NonNull @RequestPathParam("id") String id) {
    return this.handleServiceProviderContext(
      id,
      service -> JsonResponse.builder().body(Map.of("lines", service.cachedLogMessages())));
  }

  @RequestHandler(path = "/api/v2/service/{id}/liveLog")
  public @NonNull IntoResponse<?> handleServiceLiveLogRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull HttpContext context,
    @Authentication(
      providers = "jwt",
      scopes = {"cloudnet_rest:service_read", "cloudnet_rest:service_live_log"}) @NonNull RestUser restUser
  ) {
    // TODO: can we maybe support this across the cluster using SpecificServiceInfoProvider#toggleScreenEvents
    return this.handleServiceContext(id, service -> {
      var localService = this.serviceManager.localCloudService(service.serviceId().uniqueId());
      if (localService == null) {
        return ProblemDetail.builder()
          .type("service-not-on-local-node")
          .title("Service Not On Local Node")
          .status(HttpResponseCode.NOT_FOUND)
          .detail(String.format("The requested service %s is not running on the node the request was sent to.", id));
      }

      context.upgrade().thenAccept(channel -> {
        ServiceConsoleLineHandler handler = (console, line, stderr) -> channel.sendWebSocketFrame(
          WebSocketFrameType.TEXT,
          line);
        localService.serviceConsoleLogCache().addHandler(handler);

        channel.addListener(new ConsoleHandlerWebSocketListener(
          restUser,
          localService,
          localService.serviceConsoleLogCache(),
          handler));
      });

      // todo is this response correct?
      return JsonResponse.builder().responseCode(HttpResponseCode.SWITCHING_PROTOCOLS);
    });
  }

  @RequestHandler(path = "/api/v2/service/create", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_create"})
  public @NonNull IntoResponse<?> handleServiceCreateRequest(@NonNull @RequestTypedBody Document body) {
    // check for a provided service configuration
    var configuration = body.readObject("serviceConfiguration", ServiceConfiguration.class);
    if (configuration == null) {
      // check for a provided service task
      var serviceTask = body.readObject("task", ServiceTask.class);
      if (serviceTask != null) {
        configuration = ServiceConfiguration.builder(serviceTask).build();
      } else {
        // fallback to a service task name which has to exist
        var serviceTaskName = body.getString("serviceTaskName");
        if (serviceTaskName != null) {
          var task = this.serviceTaskProvider.serviceTask(serviceTaskName);
          if (task != null) {
            configuration = ServiceConfiguration.builder(task).build();
          } else {
            // we got a task but it does not exist
            return ProblemDetail.builder()
              .type("task-not-found")
              .title("Task Not Found")
              .status(HttpResponseCode.NOT_FOUND)
              .detail(String.format("The requested task %s to start the service with was not found.", serviceTaskName));
          }
        } else {
          return ProblemDetail.builder()
            .status(HttpResponseCode.BAD_REQUEST)
            .type("invalid-service-configuration")
            .title("Invalid Service Configuration")
            .detail("The provided service configuration in the body is invalid.");
        }
      }
    }

    var createResult = this.serviceFactory.createCloudService(configuration);
    var start = body.getBoolean("start", false);
    if (start && createResult.state() == ServiceCreateResult.State.CREATED) {
      createResult.serviceInfo().provider().start();
    }

    return JsonResponse.builder().responseCode(HttpResponseCode.CREATED).body(Map.of("result", createResult));
  }

  @RequestHandler(path = "/api/v2/service/{id}/add", method = HttpMethod.POST)
  @Authentication(providers = "jwt", scopes = {"cloudnet_rest:service_write", "cloudnet_rest:service_add_file"})
  public @NonNull IntoResponse<?> handleServiceAddFileRequest(
    @NonNull @RequestPathParam("id") String id,
    @NonNull @FirstRequestQueryParam("type") String type,
    @NonNull @Optional @FirstRequestQueryParam(value = "flush", def = "false") String flush,
    @NonNull @RequestTypedBody Document body
  ) {
    return this.handleServiceProviderContext(id, service -> {
      var flushAfter = Boolean.parseBoolean(flush);
      switch (StringUtil.toLower(type)) {
        case "template" -> {
          var template = body.readObject("template", ServiceTemplate.class);
          if (template == null) {
            return ProblemDetail.builder()
              .status(HttpResponseCode.BAD_REQUEST)
              .type("service-add-missing-template")
              .title("Service Add Missing Template")
              .detail("The request body does not contain a service template to add to the service.");
          } else {
            service.addServiceTemplate(template);
            if (flushAfter) {
              service.includeWaitingServiceTemplates();
            }
          }
        }

        case "deployment" -> {
          var deployment = body.readObject("deployment", ServiceDeployment.class);
          if (deployment == null) {
            return ProblemDetail.builder()
              .status(HttpResponseCode.BAD_REQUEST)
              .type("service-add-missing-deployment")
              .title("Service Add Missing Deployment")
              .detail("The request body does not contain a service deployment to add to the service.");
          } else {
            service.addServiceDeployment(deployment);
            if (flushAfter) {
              service.deployResources(body.getBoolean("removeDeployments", true));
            }
          }
        }

        case "inclusion" -> {
          var inclusion = body.readObject("inclusion", ServiceRemoteInclusion.class);
          if (inclusion == null) {
            return ProblemDetail.builder()
              .status(HttpResponseCode.BAD_REQUEST)
              .type("service-add-missing-inclusion")
              .title("Service Add Missing Inclusion")
              .detail("The request body does not contain a service inclusion to add to the service.");
          } else {
            service.addServiceRemoteInclusion(inclusion);
            if (flushAfter) {
              service.includeWaitingServiceInclusions();
            }
          }
        }

        default -> {
          return ProblemDetail.builder()
            .status(HttpResponseCode.BAD_REQUEST)
            .type("service-add-invalid-type")
            .title("Service Add Invalid Type")
            .detail(String.format(
              "The requested service add type %s is not supported. Supported values are: template, deployment, inclusion",
              type));
        }
      }

      return JsonResponse.builder().noContent();
    });
  }

  private @NonNull IntoResponse<?> handleServiceProviderContext(
    @NonNull String id,
    @NonNull Function<SpecificCloudServiceProvider, IntoResponse<?>> mapper
  ) {
    // try to find a matching service
    SpecificCloudServiceProvider serviceProvider;
    try {
      // try to parse a unique id from that
      var serviceId = UUID.fromString(id);
      serviceProvider = this.serviceManager.serviceProvider(serviceId);
    } catch (Exception exception) {
      serviceProvider = this.serviceManager.serviceProviderByName(id);
    }

    if (!serviceProvider.valid()) {
      return ProblemDetail.builder()
        .type("service-not-found")
        .title("Service Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail(String.format("The requested service %s was not found.", id));
    }

    return mapper.apply(serviceProvider);
  }

  private @NonNull IntoResponse<?> handleEmptyServiceProviderContext(
    @NonNull String id,
    @NonNull Consumer<SpecificCloudServiceProvider> mapper
  ) {
    return this.handleServiceProviderContext(id, service -> {
      mapper.accept(service);
      return JsonResponse.builder().noContent();
    });
  }

  private @NonNull IntoResponse<?> handleServiceContext(
    @NonNull String id,
    @NonNull Function<ServiceInfoSnapshot, IntoResponse<?>> mapper
  ) {
    return this.handleServiceProviderContext(id, provider -> mapper.apply(provider.serviceInfo()));
  }

  record ConsoleHandlerWebSocketListener(
    @NonNull RestUser user,
    @NonNull CloudService service,
    @NonNull ServiceConsoleLogCache logCache,
    @NonNull ServiceConsoleLineHandler watchingHandler
  ) implements WebSocketListener {

    private static final String[] REQUIRED_SCOPES = new String[]{
      "cloudnet_rest:service_write",
      "cloudnet_rest:service_send_commands"};

    @Override
    public void handle(
      @NonNull WebSocketChannel channel,
      @NonNull WebSocketFrameType type,
      byte[] bytes
    ) {
      if (type == WebSocketFrameType.TEXT && this.user.hasOneScopeOf(REQUIRED_SCOPES)) {
        var commandLine = new String(bytes, StandardCharsets.UTF_8);
        this.service.runCommand(commandLine);
      }
    }

    @Override
    public void handleClose(
      @NonNull WebSocketChannel channel,
      @NonNull AtomicInteger statusCode,
      @NonNull AtomicReference<String> reasonText
    ) {
      this.logCache.removeHandler(this.watchingHandler);
    }
  }
}
