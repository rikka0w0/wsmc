package wsmc;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
	WebSocketServerHandshaker handshaker;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			HttpRequest httpRequest = (HttpRequest) msg;

			WSMC.debug("Http Request Received: " + httpRequest.uri());

			HttpHeaders headers = httpRequest.headers();

			if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))
					&& "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {

				String url = "ws://" + httpRequest.headers().get("Host") + httpRequest.uri();

				WSMC.debug("Upgrade to: " + headers.get("Upgrade"));
				WSMC.debug("Constructed URL : " + url);

				// Adding new handler to the existing pipeline to handle WebSocket Messages
				ctx.pipeline().replace(this, "websocketHandler", new WebSocketHandler());

				WSMC.debug("WebSocketHandler added to the pipeline");
				WSMC.debug("Opened Channel : " + ctx.channel());
				WSMC.debug("Handshaking....");

				// Do the Handshake to upgrade connection from HTTP to WebSocket protocol
				WebSocketServerHandshakerFactory wsFactory =
						new WebSocketServerHandshakerFactory(url, null, true);
				handshaker = wsFactory.newHandshaker(httpRequest);
				if (handshaker == null) {
					WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
				} else {
					handshaker.handshake(ctx.channel(), httpRequest);
				}

				WSMC.debug("Handshake is done");
			} else {
				// Not a WebSocket upgrade request, send a default HTTP response
				DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.OK, Unpooled.copiedBuffer("HTTP default response", CharsetUtil.UTF_8));
				response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
				response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
				response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
			}
		} else if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {
			WSMC.debug("EMPTY_LAST_CONTENT");
		} else {
			WSMC.debug("HttpServerHandler got unknown incoming request: " + msg.getClass().getName());
		}
	}
}