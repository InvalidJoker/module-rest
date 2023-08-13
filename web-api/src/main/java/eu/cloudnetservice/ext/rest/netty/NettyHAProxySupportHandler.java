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

import eu.cloudnetservice.ext.rest.http.config.ProxyMode;
import io.netty.contrib.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelPipeline;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.NonNull;

final class NettyHAProxySupportHandler extends ByteToMessageDecoder {

  private final ProxyMode mode;

  public NettyHAProxySupportHandler(@NonNull ProxyMode mode) {
    this.mode = mode;
  }

  @Override
  protected void handlerAdded0(@NonNull ChannelHandlerContext ctx) {
    switch (this.mode) {
      // there are only two ways to properly handle this:
      //  - expect the handler to not be in the pipeline in case of being disabled
      //  - remove the handler immediately in case the mode is set to 'disabled'
      // the second option is better here to guard against invalid use of this handler
      case DISABLED -> ctx.pipeline().remove(this);
      case ENABLED -> this.insertHaProtocolHandlers(ctx.pipeline());
    }
  }

  @Override
  protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull Buffer in) {
    var detectionResult = HAProxyMessageDecoder.detectProtocol(in);
    switch (detectionResult.state()) {
      case INVALID -> ctx.pipeline().remove(this);
      case DETECTED -> this.insertHaProtocolHandlers(ctx.pipeline());
    }
  }

  private void insertHaProtocolHandlers(@NonNull ChannelPipeline pipeline) {
    // insert the ha decoder and our message reader
    pipeline
      .addFirst("ha-proxy-message-handler", NettyHAProxyMessageHandler.INSTANCE)
      .addFirst("ha-proxy-message-decoder", new HAProxyMessageDecoder());

    // remove our handler from the pipeline
    pipeline.remove(this);
  }
}
