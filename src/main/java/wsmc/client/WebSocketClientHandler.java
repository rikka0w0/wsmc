package wsmc.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

import wsmc.IWebSocketServerAddress;
import wsmc.WSMC;
import wsmc.WebSocketHandler;

public class WebSocketClientHandler extends WebSocketHandler {
	private final WebSocketClientHandshaker handshaker;
	private ChannelPromise handshakeFuture;

	/**
	 * This will set your maximum allowable frame payload length.
	 * Setting this value for big modpack.
	 */
	public final static String maxFramePayloadLength = System.getProperty("wsmc.maxFramePayloadLength", "65536");

	public WebSocketClientHandler(URI uri) {
		super("S->C", "C->S");

		int maxFramePayloadLength = 65536;

		try {
			maxFramePayloadLength = Integer.parseInt(WebSocketClientHandler.maxFramePayloadLength);
		} catch (Exception e){
			WSMC.debug("Unable to parse maxFramePayloadLength, value: " + WebSocketClientHandler.maxFramePayloadLength);
		}

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
		this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri,
				WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), maxFramePayloadLength);
	}

	public static WebSocketClientHandler fromServerData(IWebSocketServerAddress wsInfo) {
		// Do not perform WebSocket handshake for vanilla TCP Minecraft
		if (wsInfo == null || wsInfo.isVanilla())
			return null;

		try {
			URI uri = new URI(wsInfo.getScheme(), null, wsInfo.asServerAddress().getHost(),
					wsInfo.asServerAddress().getPort(), wsInfo.getPath(), null, null);

			return new WebSocketClientHandler(uri);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public static void hookPipeline(ChannelPipeline pipeline, IWebSocketServerAddress wsInfo) {
		final WebSocketClientHandler handler = WebSocketClientHandler.fromServerData(wsInfo);

		if (handler != null) {
			pipeline.addAfter("timeout", "WsmcHttpClient", new HttpClientCodec());
			pipeline.addAfter("WsmcHttpClient", "WsmcHttpAggregator", new HttpObjectAggregator(8192*4));
			pipeline.addAfter("WsmcHttpAggregator", "WsmcCompressionHandler", WebSocketClientCompressionHandler.INSTANCE);
			pipeline.addAfter("WsmcCompressionHandler", "WsmcWebSocketClientHandler", handler);

			if ("wss".equalsIgnoreCase(wsInfo.getScheme())) {
				try {
					SslContext sslCtx = SslContextBuilder.forClient()
							.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

					// SSL Parameters to set SNI TLS Extension
					SSLParameters sslParameters = new SSLParameters();
					sslParameters.setServerNames(Collections.singletonList(new SNIHostName(wsInfo.asServerAddress().getHost())));

					// SSLEngine with SSL Parameters for SNI
					SSLEngine sslEngine = sslCtx.newEngine(ByteBufAllocator.DEFAULT);
					sslEngine.setSSLParameters(sslParameters);

					// SSL Handler
					SslHandler sslHandler = new SslHandler(sslEngine);

					pipeline.addAfter("timeout", "WsmcSslHandler", sslHandler);
				} catch (SSLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
		this.handshakeFuture = ctx.newPromise();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		handshaker.handshake(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		WSMC.debug(this.inboundPrefix + " WebSocket Client disconnected!");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);

		cause.printStackTrace();
		if (!handshakeFuture.isDone()) {
			handshakeFuture.setFailure(cause);
		}
		ctx.close();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Channel ch = ctx.channel();
		if (!handshaker.isHandshakeComplete()) {
			try {
				handshaker.finishHandshake(ch, (FullHttpResponse) msg);
				WSMC.debug(this.inboundPrefix + " WebSocket Client connected!");
				handshakeFuture.setSuccess();
			} catch (WebSocketHandshakeException e) {
				WSMC.debug(this.inboundPrefix + " WebSocket Client failed to connect");
				handshakeFuture.setFailure(e);
			}
			return;
		}

		if (msg instanceof FullHttpResponse) {
			FullHttpResponse response = (FullHttpResponse) msg;
			throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status()
					+ ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
		}

		super.channelRead(ctx, msg);
	}

	@Override
	protected void sendWsFrame(ChannelHandlerContext ctx, WebSocketFrame frame, ChannelPromise promise) throws Exception {
		if (handshakeFuture.isSuccess()) {
			ctx.write(frame, promise);
		} else {
			handshakeFuture.addListener((future) -> {
				if (handshakeFuture.isSuccess()) {
					ctx.write(frame, promise);
				}
			});
		}
	}
}
