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

package eu.cloudnetservice.ext.rest.api.registry;

import eu.cloudnetservice.ext.rest.api.HttpContext;
import eu.cloudnetservice.ext.rest.api.HttpHandler;
import eu.cloudnetservice.ext.rest.api.HttpMethod;
import eu.cloudnetservice.ext.rest.api.HttpRequest;
import eu.cloudnetservice.ext.rest.api.config.ComponentConfig;
import eu.cloudnetservice.ext.rest.api.config.HttpHandlerConfig;
import eu.cloudnetservice.ext.rest.api.tree.DynamicHttpPathNode;
import eu.cloudnetservice.ext.rest.api.tree.StaticHttpPathNode;
import eu.cloudnetservice.ext.rest.api.tree.WildcardPathNode;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public final class HttpHandlerRegistryTest {

  @SuppressWarnings("DataFlowIssue")
  private static final HttpHandler EMPTY_HTTP_HANDLER = context -> null;

  private HttpContext context;
  private HttpHandlerRegistry registry;
  private final ComponentConfig componentConfig = ComponentConfig.builder().build();

  @BeforeEach
  public void setupEach() {
    this.context = Mockito.mock(HttpContext.class);
    var request = Mockito.mock(HttpRequest.class);
    Mockito.when(this.context.request()).thenReturn(request);
    Mockito.when(request.pathParameters()).thenReturn(new HashMap<>());

    this.registry = new DefaultHttpHandlerRegistry(this.componentConfig);
  }

  @Test
  public void testDynamic() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    Assertions.assertDoesNotThrow(() -> this.registry.registerHandler("api/test/{name}", EMPTY_HTTP_HANDLER, config));
    Assertions.assertThrows(
      HttpHandlerRegisterException.class,
      () -> this.registry.registerHandler("api/test/{other}", EMPTY_HTTP_HANDLER, config));

    Assertions.assertNotNull(this.registry.findHandler("api/test/playo", this.context));
    Assertions.assertNull(this.registry.findHandler("api/test/playo/other", this.context));

    Assertions.assertEquals(1, this.registry.registeredHandlers().size());
  }

  @Test
  public void testStatic() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/test/static", EMPTY_HTTP_HANDLER, config);

    Assertions.assertNotNull(this.registry.findHandler("api/test/static", this.context));
    Assertions.assertNull(this.registry.findHandler("api/test/static/other", this.context));

    Assertions.assertEquals(1, this.registry.registeredHandlers().size());
  }

  @Test
  public void testWildcard() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/test/wildcard/*", EMPTY_HTTP_HANDLER, config);

    Assertions.assertNotNull(this.registry.findHandler("api/test/wildcard", this.context));
    Assertions.assertNotNull(this.registry.findHandler("api/test/wildcard/other/more", this.context));

    Assertions.assertEquals(1, this.registry.registeredHandlers().size());
  }

  @Test
  public void testPriority() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/test/static", EMPTY_HTTP_HANDLER, config);
    this.registry.registerHandler("api/test/{static}", EMPTY_HTTP_HANDLER, config);
    this.registry.registerHandler("api/test/*", EMPTY_HTTP_HANDLER, config);

    var staticResult = this.registry.findHandler("api/test/static", this.context);
    Assertions.assertNotNull(staticResult);
    Assertions.assertInstanceOf(StaticHttpPathNode.class, staticResult.pathNode());

    var dynamicResult = this.registry.findHandler("api/test/other", this.context);
    Assertions.assertNotNull(dynamicResult);
    Assertions.assertInstanceOf(DynamicHttpPathNode.class, dynamicResult.pathNode());

    var wildcardResult = this.registry.findHandler("api/test/static/more", this.context);
    Assertions.assertNotNull(wildcardResult);
    Assertions.assertInstanceOf(WildcardPathNode.class, wildcardResult.pathNode());
  }

  @Test
  public void testUnregister() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/hello/world", EMPTY_HTTP_HANDLER, config);

    var tree = this.registry.findHandler("api/hello/world", this.context);
    Assertions.assertNotNull(tree);
    Assertions.assertEquals(1, this.registry.registeredHandlers().size());

    var handler = tree.pathNode().findHandlerForMethod(HttpMethod.GET.name());
    Assertions.assertNotNull(handler);

    this.registry.unregisterHandler(handler.httpHandler());
    Assertions.assertNull(this.registry.findHandler("api/hello/world", this.context));
    Assertions.assertEquals(0, this.registry.registeredHandlers().size());
  }

  @Test
  public void testClassLoaderUnregister() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/hello/world", EMPTY_HTTP_HANDLER, config);
    Assertions.assertNotNull(this.registry.findHandler("api/hello/world", this.context));
    Assertions.assertEquals(1, this.registry.registeredHandlers().size());

    this.registry.unregisterHandlers(this.getClass().getClassLoader());
    Assertions.assertNull(this.registry.findHandler("api/hello/world", this.context));
    Assertions.assertEquals(0, this.registry.registeredHandlers().size());
  }

  @Test
  public void testClear() {
    var config = HttpHandlerConfig.builder().httpMethod(HttpMethod.GET).build();

    this.registry.registerHandler("api/hello/world", EMPTY_HTTP_HANDLER, config);
    Assertions.assertNotNull(this.registry.findHandler("api/hello/world", this.context));
    Assertions.assertEquals(1, this.registry.registeredHandlers().size());

    this.registry.clearHandlers();
    Assertions.assertNull(this.registry.findHandler("api/hello/world", this.context));
    Assertions.assertEquals(0, this.registry.registeredHandlers().size());
  }
}
