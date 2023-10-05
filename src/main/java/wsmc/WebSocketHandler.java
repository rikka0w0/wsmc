package wsmc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.buffer.ByteBuf;

public class WebSocketHandler
		extends CombinedChannelDuplexHandler<WebSocketHandler.InboundHandler, WebSocketHandler.OutboundHandler> {

	public WebSocketHandler() {
		super(new InboundHandler(), new OutboundHandler());
	}

    public static void dumpByteArray(ByteBuf byteArray) {
        int maxBytesPerLine = 32;
        int totalBytes = byteArray.readableBytes();
        byteArray.markReaderIndex();

        for (int i = 0; i < totalBytes; i += maxBytesPerLine) {
            int remainingBytes = Math.min(maxBytesPerLine, totalBytes - i);
            StringBuilder line = new StringBuilder();

            for (int j = 0; j < remainingBytes; j++) {
                byte currentByte = byteArray.readByte();
                line.append(String.format("%02X ", currentByte));
            }

            System.out.println(line.toString().trim());
        }
        byteArray.resetReaderIndex();
    }

	static class OutboundHandler extends ChannelOutboundHandlerAdapter {
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
			if (msg instanceof ByteBuf) {
				ByteBuf byteBuf = (ByteBuf) msg;

				if (WSMC.debug()) {
					WSMC.debug("S->C (" +byteBuf.readableBytes() + "):");
					dumpByteArray(byteBuf);
				}

				ctx.write(new BinaryWebSocketFrame(byteBuf), promise);
			} else {
				// DefaultFullHttpResponse
				WSMC.debug("S->C Passthrough: " + msg.getClass().getName());
				ctx.write(msg, promise);
			}
		}
	}

	static class InboundHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			if (msg instanceof WebSocketFrame) {
				if (msg instanceof BinaryWebSocketFrame) {
					ByteBuf content = ((WebSocketFrame) msg).content();

					if (WSMC.debug()) {
						WSMC.debug("C->S (" + content.readableBytes() + "):");
						dumpByteArray(content);
					}

					ctx.fireChannelRead(content);
				} else if (msg instanceof CloseWebSocketFrame) {
					WSMC.debug("CloseWebSocketFrame (" + ((CloseWebSocketFrame) msg).statusCode()
								+ ") received : " + ((CloseWebSocketFrame) msg).reasonText());
				} else {
					WSMC.debug("Unsupported WebSocketFrame: " + msg.getClass().getName());
				}
			}
		}
	}

}
