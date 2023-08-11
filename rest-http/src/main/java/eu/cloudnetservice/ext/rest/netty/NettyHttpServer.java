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

package eu.cloudnetservice.ext.rest.netty;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.netty.NettySslServer;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.HttpServer;
import eu.cloudnetservice.ext.rest.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.ext.rest.http.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.ext.rest.http.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.http.tree.DefaultHttpHandlerTree;
import eu.cloudnetservice.ext.rest.http.tree.DynamicHttpPathNode;
import eu.cloudnetservice.ext.rest.http.tree.HttpHandlerTree;
import eu.cloudnetservice.ext.rest.http.tree.HttpPathNode;
import eu.cloudnetservice.ext.rest.http.tree.StaticHttpPathNode;
import eu.cloudnetservice.ext.rest.http.tree.WildcardPathNode;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.util.concurrent.Future;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of the web server, using netty as its backing mechanism.
 *
 * @since 4.0
 */
@Singleton
public class NettyHttpServer extends NettySslServer implements HttpServer {

  private static final Logger LOGGER = LogManager.logger(NettyHttpServer.class);
  private static final Predicate<HttpHandlerTree<HttpPathNode>> DYNAMIC_NODE_FILTER =
    node -> node.pathNode() instanceof DynamicHttpPathNode;

  protected final ComponentConfig componentConfig;
  protected final DefaultHttpHandlerTree handlerTree = DefaultHttpHandlerTree.newTree();
  protected final Map<HostAndPort, Future<Void>> channelFutures = new ConcurrentHashMap<>();

  protected final EventLoopGroup bossGroup = NettyUtil.newEventLoopGroup(1);
  protected final EventLoopGroup workerGroup = NettyUtil.newEventLoopGroup(0);

  protected final HttpAnnotationParser<HttpServer> annotationParser;

  /**
   * Constructs a new instance of a netty http server instance. Equivalent to {@code new NettyHttpServer(null)}.
   */
  public NettyHttpServer(@NonNull ComponentConfig config) {
    this(null, config);
  }

  /**
   * Constructs a new netty http server instance with the given ssl configuration.
   *
   * @param sslConfiguration the ssl configuration to use, null for no ssl.
   */
  public NettyHttpServer(@Nullable SSLConfiguration sslConfiguration, @NonNull ComponentConfig componentConfig) {
    super(sslConfiguration);

    this.componentConfig = componentConfig;
    this.annotationParser = DefaultHttpAnnotationParser.withDefaultProcessors(this);

    try {
      this.init();
    } catch (Exception exception) {
      LOGGER.severe("Exception initializing web server", exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean sslEnabled() {
    return this.sslContext != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ComponentConfig componentConfig() {
    return this.componentConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpAnnotationParser<HttpServer> annotationParser() {
    return this.annotationParser;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<Void> addListener(int port) {
    return this.addListener(new HostAndPort("0.0.0.0", port));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<Void> addListener(@NonNull HostAndPort hostAndPort) {
    Task<Void> result = new Task<>();
    new ServerBootstrap()
      .group(this.bossGroup, this.workerGroup)
      .channelFactory(NettyUtil.serverChannelFactory())
      .childHandler(new NettyHttpServerInitializer(this, hostAndPort))

      .childOption(ChannelOption.AUTO_READ, true)
      .childOption(ChannelOption.TCP_NODELAY, true)
      .childOption(ChannelOption.SO_REUSEADDR, true)

      .option(ChannelOption.SO_REUSEADDR, true)

      .bind(hostAndPort.host(), hostAndPort.port())
      .addListener(future -> {
        if (future.isSuccess()) {
          // ok, we bound successfully
          result.complete(null);
          this.channelFutures.put(hostAndPort, future.getNow().closeFuture());
        } else {
          // something went wrong
          result.completeExceptionally(future.cause());
        }
      });

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer registerHandler(
    @NonNull String path,
    @NonNull HttpHandler handler,
    @NonNull HttpHandlerConfig config
  ) {
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

    // register the http handler to the node
    lastSeenTreeNode.pathNode().registerHttpHandler(handler, config);

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer removeHandler(@NonNull HttpHandler handler) {
    var nodeToUnregister = this.handlerTree.findMatchingChildNode(node -> {
      var handlers = node.pathNode().handlers();
      return handlers.stream().anyMatch(pair -> pair.httpHandler() == handler);
    });
    if (nodeToUnregister != null) {
      this.handlerTree.unregisterChildNode(nodeToUnregister);
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer removeHandler(@NonNull ClassLoader classLoader) {
    var nodeToUnregister = this.handlerTree.findMatchingChildNode(node -> {
      var httpHandler = node.pathNode().handlers();
      return !httpHandler.isEmpty() && httpHandler.getClass().getClassLoader() == classLoader;
    });
    if (nodeToUnregister != null) {
      this.handlerTree.unregisterChildNode(nodeToUnregister);
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<HttpHandler> httpHandlers() {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer clearHandlers() {
    this.handlerTree.removeAllChildren();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    for (var entry : this.channelFutures.values()) {
      entry.cancel();
    }

    this.bossGroup.shutdownGracefully();
    this.workerGroup.shutdownGracefully();
    this.clearHandlers();
  }
}
