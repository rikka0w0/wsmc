package wsmc;

import java.util.function.Consumer;

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
	public final static String wsmcEndpoint = System.getProperty("wsmc.wsmcEndpoint", null);

	/**
	 * This will set your maximum allowable frame payload length.
	 * Setting this value for big modpack.
	 */
	public final static String maxFramePayloadLength = System.getProperty("wsmc.maxFramePayloadLength", "65536");

	/**
	 * This will be called when a WebSocket upgrade is received.
	 * Note that this does NOT guarantee a success WebSocket handshake.
	 */
	private final Consumer<HttpRequest> onWsmcHandshake;

	public HttpServerHandler(Consumer<HttpRequest> onWsmcHandshake) {
		this.onWsmcHandshake = onWsmcHandshake;
	}

	/**
	 * Checks if the incoming request matches the expected endpoint
	 * {@link wsmc.HttpServerHandler.wsmcEndpoint}.
	 * If {@link wsmc.HttpServerHandler.wsmcEndpoint} is null,
	 * the path of the incoming request can be any.
	 *
	 * @param endpoint
	 * @return true if match or endpoint can be any, false if not match.
	 */
	private boolean isWsmcEndpoint(String endpoint) {
		if (HttpServerHandler.wsmcEndpoint == null)
			return true;

		// This has to be case-sensitive!
		return HttpServerHandler.wsmcEndpoint.equals(endpoint);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
			HttpRequest httpRequest = (HttpRequest) msg;
			String endpoint = httpRequest.uri();

			WSMC.debug("Http Request Received: " + httpRequest.uri());

			HttpHeaders headers = httpRequest.headers();

			if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))
					&& "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))
					&& isWsmcEndpoint(endpoint)) {
				String url = "ws://" + httpRequest.headers().get("Host") + httpRequest.uri();
				WSMC.debug("Upgrade to: " + headers.get("Upgrade") + " for: " + url);

				if (this.onWsmcHandshake != null) {
					this.onWsmcHandshake.accept(httpRequest);
				}

				// Adding new handler to the existing pipeline to handle WebSocket Messages
				ctx.pipeline().replace(this, "WsmcWebSocketServerHandler", new WebSocketHandler.WebSocketServerHandler());

				WSMC.debug("Opened Channel: " + ctx.channel());

				int maxFramePayloadLength = 65536;

				try {
					maxFramePayloadLength = Integer.parseInt(HttpServerHandler.maxFramePayloadLength);
				} catch (Exception e){
					WSMC.debug("Unable to parse maxFramePayloadLength, value: " + HttpServerHandler.maxFramePayloadLength);
				}

				System.out.println("maxFramePayloadLength: " + maxFramePayloadLength);
				// Do the Handshake to upgrade connection from HTTP to WebSocket protocol
				WebSocketServerHandshakerFactory wsFactory =
							new WebSocketServerHandshakerFactory(url, null, true, maxFramePayloadLength);
				WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);

				if (handshaker == null) {
					WSMC.info("Unsupported WebSocket version");
					WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
				} else {
					WSMC.debug("Handshaking starts...");
					handshaker.handshake(ctx.channel(), httpRequest)
						.addListener((future) -> WSMC.debug("Handshake is done"));
				}

				// Here we assume that the server never actively sends anything before it receives anything from the client.
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
