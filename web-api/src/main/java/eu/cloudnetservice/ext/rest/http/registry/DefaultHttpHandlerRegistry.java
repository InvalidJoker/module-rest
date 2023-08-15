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

package eu.cloudnetservice.ext.rest.http.registry;

import eu.cloudnetservice.ext.rest.http.HttpContext;
import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.http.tree.DefaultHttpHandlerTree;
import eu.cloudnetservice.ext.rest.http.tree.DynamicHttpPathNode;
import eu.cloudnetservice.ext.rest.http.tree.HttpHandlerTree;
import eu.cloudnetservice.ext.rest.http.tree.HttpPathNode;
import eu.cloudnetservice.ext.rest.http.tree.StaticHttpPathNode;
import eu.cloudnetservice.ext.rest.http.tree.WildcardPathNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public final class DefaultHttpHandlerRegistry implements HttpHandlerRegistry {

  private static final Predicate<HttpHandlerTree<HttpPathNode>> DYNAMIC_NODE_FILTER =
    node -> node.pathNode() instanceof DynamicHttpPathNode;

  private final ComponentConfig componentConfig;
  private final DefaultHttpHandlerTree handlerTree = DefaultHttpHandlerTree.newTree();

  public DefaultHttpHandlerRegistry(@NonNull ComponentConfig componentConfig) {
    this.componentConfig = componentConfig;
  }

  @Override
  public @Unmodifiable @NonNull Collection<HttpHandler> registeredHandlers() {
    List<HttpHandler> httpHandlers = new ArrayList<>();
    this.handlerTree.visitFullTree(node -> {
      var handlerPairs = node.pathNode().handlers();
      if (!handlerPairs.isEmpty()) {
        for (var handler : handlerPairs) {
          httpHandlers.add(handler.httpHandler());
        }
      }
    });
    return httpHandlers;
  }

  @Override
  public @Nullable HttpHandlerTree<HttpPathNode> findHandler(@NonNull String path, @NonNull HttpContext context) {
    var matchingTreeNode = this.handlerTree;
    if (!path.equals("/")) {
      // strip the leading slash
      if (path.startsWith("/")) {
        path = path.substring(1);
      }

      for (var pathPart : path.split("/")) {
        // find the first node that accepts the path part as input; returns null in case no node does so
        matchingTreeNode = matchingTreeNode.findMatchingChildNode(
          node -> node.pathNode().validateAndRegisterPathPart(context, pathPart));
        if (matchingTreeNode == null || matchingTreeNode.pathNode().consumesRemainingPath()) {
          break;
        }
      }
    }
    return matchingTreeNode;
  }

  @Override
  public void registerHandler(@NonNull String path, @NonNull HttpHandler handler, @NonNull HttpHandlerConfig config) {
    // ensure that the path ends with a / (if the path is not the root handler)
    var lastSeenTreeNode = this.handlerTree;
    if (!path.equals("/")) {
      // strip the leading slash
      if (path.startsWith("/")) {
        path = path.substring(1);
      }

      var pathParts = path.split("/");
      for (int i = 0; i < pathParts.length; i++) {
        var pathPart = pathParts[i];
        if (pathPart.startsWith("{") && pathPart.endsWith("}")) {
          // extract the path id from the given input
          var pathId = pathPart.substring(1, pathPart.length() - 1);
          HttpPathNode.validatePathId(pathId);

          // check if the current node already has a dynamic node
          var existingDynamicNode = lastSeenTreeNode.findMatchingChildNode(DYNAMIC_NODE_FILTER);
          if (existingDynamicNode != null) {
            if (existingDynamicNode.pathNode().pathId().equals(pathId)) {
              // there is an existing dynamic node with the same name, use that
              lastSeenTreeNode = existingDynamicNode;
            } else {
              // there can't be two different dynamic named nodes at the same point in the tree
              // todo: show node tree upwards
              throw new IllegalStateException(String.format(
                "Tried to register second dynamic node named \"%s\" alongside \"%s\" in http handler tree. Path chain: %s",
                pathId,
                existingDynamicNode.pathNode().pathId(),
                ""));
            }
          } else {
            // register the dynamic path node
            var dynamicNode = new DynamicHttpPathNode(pathId);
            lastSeenTreeNode = lastSeenTreeNode.registerChildNode(dynamicNode);
          }
        } else if (pathPart.equals("*")) {
          // ensure that a wildcard node is only used at the end of the uri
          if (i != (pathParts.length - 1)) {
            throw new IllegalStateException("Invalid use of wildcard in middle of handler uri: " + path);
          }

          // register the wildcard node
          lastSeenTreeNode = lastSeenTreeNode.registerChildNode(new WildcardPathNode());
        } else {
          HttpPathNode.validatePathId(pathPart);

          // register the static path node
          var staticNode = new StaticHttpPathNode(pathPart);
          lastSeenTreeNode = lastSeenTreeNode.registerChildNode(staticNode);
        }
      }
    }

    // ensure that there is no http handler for the current node yet
    var existingHandler = lastSeenTreeNode.pathNode().findHandlerForMethod(config.httpMethod());
    if (existingHandler != null) {
      // todo: path
      throw new IllegalStateException("Detected duplicate http handler for path \"%s\"");
    }

    // construct the final handler config & register the http handler
    var mergedCorsConfig = this.componentConfig.corsConfig().combine(config.corsConfig());
    var handlerConfig = HttpHandlerConfig.builder(config).corsConfiguration(mergedCorsConfig).build();
    lastSeenTreeNode.pathNode().registerHttpHandler(handler, handlerConfig);
  }

  @Override
  public void unregisterHandler(@NonNull HttpHandler handler) {
    var nodeToUnregister = this.handlerTree.findMatchingChildNode(node -> {
      var handlers = node.pathNode().handlers();
      return handlers.stream().anyMatch(pair -> pair.httpHandler() == handler);
    });
    if (nodeToUnregister != null) {
      this.handlerTree.unregisterChildNode(nodeToUnregister);
    }
  }

  @Override
  public void unregisterHandlers(@NonNull ClassLoader classLoader) {
    var nodeToUnregister = this.handlerTree.findMatchingChildNode(node -> {
      var httpHandler = node.pathNode().handlers();
      return !httpHandler.isEmpty() && httpHandler.getClass().getClassLoader() == classLoader;
    });
    if (nodeToUnregister != null) {
      this.handlerTree.unregisterChildNode(nodeToUnregister);
    }
  }

  @Override
  public void clearHandlers() {
    this.handlerTree.removeAllChildren();
  }
}
