package wsmc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpGetSniffer extends ByteToMessageDecoder {
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
				ctx.pipeline().replace(this, "HttpCodec", new HttpServerCodec());
				ctx.pipeline().addAfter("HttpCodec", "HttpHandler", new HttpServerHandler());
			} else {
				WSMC.debug("TCP Minecraft");
				ctx.pipeline().remove(this);
			}
		}
	}
}
