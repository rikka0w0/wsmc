package wsmc.client;

import java.net.URI;
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
import wsmc.WebSocketConnectionInfo;
import wsmc.WebSocketHandler;

public class WebSocketClientHandler extends WebSocketHandler {
	private final WebSocketClientHandshaker handshaker;
	private ChannelPromise handshakeFuture;

	/**
	 * This will set your maximum allowable frame payload length.
	 * Setting this value for big modpack.
	 */
	public final static String maxFramePayloadLength = System.getProperty("wsmc.maxFramePayloadLength", "65536");

	public WebSocketClientHandler(URI uri, String httpHostname) {
		super("S->C", "C->S");

		int maxFramePayloadLength = 65536;

		try {
			maxFramePayloadLength = Integer.parseInt(WebSocketClientHandler.maxFramePayloadLength);
		} catch (Exception e){
			WSMC.debug("Unable to parse maxFramePayloadLength, value: " + WebSocketClientHandler.maxFramePayloadLength);
		}

		DefaultHttpHeaders headers = new DefaultHttpHeaders();
		headers.set("Host", httpHostname);
		headers.set("User-Agent", buildUserAgent());

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
		this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri,
				WebSocketVersion.V13, null, true, headers, maxFramePayloadLength);
	}

	public static void hookPipeline(ChannelPipeline pipeline, IWebSocketServerAddress wsInfo) {
		// Do not perform WebSocket handshake for vanilla TCP Minecraft
		if (wsInfo != null && !wsInfo.isVanilla()) {
			WebSocketConnectionInfo connInfo = wsInfo.getWsConnectionInfo();
			final WebSocketClientHandler handler = new WebSocketClientHandler(connInfo.uri, connInfo.httpHostname);
			WSMC.info("Connecting to WebSocket Server:\n" + connInfo.toString());

			pipeline.addAfter("timeout", "WsmcHttpClient", new HttpClientCodec());
			pipeline.addAfter("WsmcHttpClient", "WsmcHttpAggregator", new HttpObjectAggregator(8192*4));
			pipeline.addAfter("WsmcHttpAggregator", "WsmcCompressionHandler", WebSocketClientCompressionHandler.INSTANCE);
			pipeline.addAfter("WsmcCompressionHandler", "WsmcWebSocketClientHandler", handler);

			if ("wss".equalsIgnoreCase(connInfo.uri.getScheme())) {
				try {
					SslContext sslCtx = SslContextBuilder.forClient()
							.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

					// SSL Parameters to set SNI TLS Extension
					SSLParameters sslParameters = new SSLParameters();
					sslParameters.setServerNames(Collections.singletonList(new SNIHostName(connInfo.sni)));

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

	// === User-Agent 构建逻辑与加载器检测合并于此 ===
	private static String buildUserAgent() {
		String javaVersion = System.getProperty("java.version");

		String modName = WSMC.modName != null ? WSMC.modName : "UnknownMod";
		String modVersion = WSMC.modVersion != null ? WSMC.modVersion : "0.0.0";

		String loaderName = "Unknown";
		String mcVersion = "unknown";

		try {
			Class.forName("net.neoforged.fml.common.Mod");
			loaderName = "NeoForge";
			mcVersion = net.neoforged.fml.loading.FMLLoader.versionInfo().mcVersion();
		} catch (ClassNotFoundException ignored1) {
			try {
				Class.forName("net.minecraftforge.fml.common.Mod");
				loaderName = "Forge";
				mcVersion = net.minecraftforge.fml.loading.FMLLoader.versionInfo().mcVersion();
			} catch (ClassNotFoundException ignored2) {
				try {
					Class.forName("net.fabricmc.loader.api.FabricLoader");
					loaderName = "Fabric";
					mcVersion = net.fabricmc.loader.api.FabricLoader.getInstance()
						.getModContainer("minecraft")
						.map(m -> m.getMetadata().getVersion().getFriendlyString())
						.orElse("unknown");
				} catch (ClassNotFoundException ignored3) {
					try {
						Class.forName("org.quiltmc.loader.api.QuiltLoader");
						loaderName = "Quilt";
						mcVersion = org.quiltmc.loader.api.QuiltLoader.getModContainer("minecraft")
							.map(m -> m.metadata().version().raw())
							.orElse("unknown");
					} catch (ClassNotFoundException ignored4) {
						// unknown loader
					}
				}
			}
		}

		return String.format("Java/%s Minecraft/%s %s/%s %s/%s",
			javaVersion,
			mcVersion,
			modName,
			modVersion,
			loaderName,
			mcVersion // loader version 和 MC 版本相同或不可区分时使用
		);
	}

}
