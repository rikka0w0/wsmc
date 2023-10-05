package wsmc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
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
				System.out.println("S->C (" +byteBuf.readableBytes() + "):");
				byteBuf.markReaderIndex();
				dumpByteArray(byteBuf);
				byteBuf.resetReaderIndex();

				ctx.write(new BinaryWebSocketFrame(byteBuf), promise);
			} else {
				// DefaultFullHttpResponse
				System.out.println("Passthrough: " + msg.getClass().getName());
				ctx.write(msg, promise);
			}
		}
	}

	static class InboundHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			if (msg instanceof WebSocketFrame) {
				System.out.println("This is a WebSocket frame, Client Channel : " + ctx.channel());
				if (msg instanceof BinaryWebSocketFrame) {
					ByteBuf content = ((WebSocketFrame) msg).content();
					System.out.println("C->S (" + content.readableBytes() + "):");
					content.markReaderIndex();
					dumpByteArray(content);
					content.resetReaderIndex();
					ctx.fireChannelRead(content);
				} else if (msg instanceof TextWebSocketFrame) {
					System.out.println("TextWebSocketFrame Received : ");
					ctx.channel().writeAndFlush(
							new TextWebSocketFrame("Message recieved : " + ((TextWebSocketFrame) msg).text()));
					System.out.println(((TextWebSocketFrame) msg).text());
				} else if (msg instanceof PingWebSocketFrame) {
					System.out.println("PingWebSocketFrame Received : ");
					System.out.println(((PingWebSocketFrame) msg).content());
				} else if (msg instanceof PongWebSocketFrame) {
					System.out.println("PongWebSocketFrame Received : ");
					System.out.println(((PongWebSocketFrame) msg).content());
				} else if (msg instanceof CloseWebSocketFrame) {
					System.out.println("CloseWebSocketFrame Received : ");
					System.out.println("ReasonText :" + ((CloseWebSocketFrame) msg).reasonText());
					System.out.println("StatusCode : " + ((CloseWebSocketFrame) msg).statusCode());
				} else {
					System.out.println("Unsupported WebSocketFrame");
				}
			}
		}
	}

}
