package wsmc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpGetSniffer extends ByteToMessageDecoder {
	public final static boolean disableVanillaTCP =
			System.getProperty("wsmc.disableVanillaTCP", "false").equalsIgnoreCase("true");

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() > 3) {
			byte[] byteBuffer = new byte[3];
			in.markReaderIndex();
			in.readBytes(byteBuffer, 0, 3);
			in.resetReaderIndex();
			String methodString = new String(byteBuffer, StandardCharsets.US_ASCII);

			if (methodString.equalsIgnoreCase("GET")) {
				WSMC.debug("Websocket Minecraft");
				ctx.pipeline().replace(this, "WsmcHttpCodec", new HttpServerCodec());
				ctx.pipeline().addAfter("WsmcHttpCodec", "WsmcHttpHandler", new HttpServerHandler());
			} else {
				if (HttpGetSniffer.disableVanillaTCP) {
					WSMC.info(ctx.channel().remoteAddress().toString() +
							" attemps to establish a Vanilla TCP connection which has been disabled by WSMC.");
					throw new RuntimeException("Vanilla TCP connection has been disabled by WSMC.");
				}

				WSMC.debug("TCP Minecraft");
				ctx.pipeline().remove(this);
			}
		}
	}
}
