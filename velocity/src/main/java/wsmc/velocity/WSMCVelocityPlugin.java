package wsmc.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

import org.slf4j.Logger;

import wsmc.WSMC;
import wsmc.HttpGetSniffer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

@Plugin(
	id = "wsmc",
	name = "WSMC",
	version = "0.7.1",
	description = "Enable WebSocket support for Velocity proxy. Allows players to connect via WebSocket (ws:// or wss://) through CDN.",
	authors = {"Rikka0w0"}
)
public class WSMCVelocityPlugin {

	private final ProxyServer server;
	private final Logger logger;
	private final Path dataDirectory;

	@Inject
	public WSMCVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		this.server = server;
		this.logger = logger;
		this.dataDirectory = dataDirectory;
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		try {
			Field cmField = server.getClass().getDeclaredField("cm");
			cmField.setAccessible(true);
			Object connectionMgr = cmField.get(server);

			Field sciField = connectionMgr.getClass().getField("serverChannelInitializer");
			Object holder = sciField.get(connectionMgr);

			ChannelInitializer<Channel> original = (ChannelInitializer<Channel>)
				holder.getClass().getMethod("get").invoke(holder);

			Method initChannelMethod = ChannelInitializer.class
				.getDeclaredMethod("initChannel", Channel.class);
			initChannelMethod.setAccessible(true);

			ChannelInitializer<Channel> wrapped = new ChannelInitializer<Channel>() {
				@Override
				protected void initChannel(Channel ch) throws Exception {
					initChannelMethod.invoke(original, ch);
					ch.pipeline().addFirst("wsmc-http-sniffer", new HttpGetSniffer(null));
				}
			};

			holder.getClass().getMethod("set", ChannelInitializer.class).invoke(holder, wrapped);

			logger.info("WSMC Velocity plugin enabled. WebSocket connections will be accepted on the proxy port.");
		} catch (Exception e) {
			logger.error("WSMC Velocity failed to register channel initializer. WebSocket support will not be available.", e);
		}
	}
}
