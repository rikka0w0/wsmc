package wsmc;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.level.ServerPlayer;

public final class WsmcTransferCommand {
	private static final SimpleCommandExceptionType ERROR_INVALID_URI = new SimpleCommandExceptionType(Component.literal("Invalid WebSocket URI"));
	private static final SimpleCommandExceptionType ERROR_NO_PLAYERS = new SimpleCommandExceptionType(
		Component.translatable("commands.transfer.error.no_players")
	);

	private WsmcTransferCommand() {
	}

	public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("transfer")
				.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
				.then(
					Commands.argument("wsmc_uri", StringArgumentType.greedyString())
						.executes(WsmcTransferCommand::transferWebSocket)
				)
		);
	}

	private static int transferWebSocket(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		TransferTarget target = parseTarget(context.getSource(), StringArgumentType.getString(context, "wsmc_uri"));
		return transfer(context.getSource(), target.packetHost(), target.port(), target.players());
	}

	private static TransferTarget parseTarget(CommandSourceStack source, String input) throws CommandSyntaxException {
		StringReader reader = new StringReader(input);
		reader.skipWhitespace();

		String uriString = readToken(reader);
		WebSocketConnectionInfo connInfo = WebSocketConnectionInfo.parseUri(uriString);
		if (connInfo == null)
			throw ERROR_INVALID_URI.create();

		reader.skipWhitespace();

		int port = connInfo.uri.getPort();
		String packetHost = uriString;
		Collection<ServerPlayer> players = null;

		if (reader.canRead()) {
			int beforeOptionalPort = reader.getCursor();
			String token = readToken(reader);

			if (isIntegerLiteral(token)) {
				port = parsePort(token);
				packetHost = withPort(uriString, port);
				reader.skipWhitespace();
			} else {
				reader.setCursor(beforeOptionalPort);
			}
		}

		if (reader.canRead()) {
			players = parsePlayers(source, reader.getRemaining());
		}

		if (players == null)
			players = List.of(source.getPlayerOrException());

		return new TransferTarget(packetHost, port, players);
	}

	private static String readToken(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
			reader.skip();
		}

		if (reader.getCursor() == start)
			throw ERROR_INVALID_URI.create();

		return reader.getString().substring(start, reader.getCursor());
	}

	private static boolean isIntegerLiteral(String token) {
		if (token.isEmpty())
			return false;

		int index = token.charAt(0) == '-' ? 1 : 0;
		if (index == token.length())
			return false;

		for (; index < token.length(); index++) {
			if (!Character.isDigit(token.charAt(index)))
				return false;
		}

		return true;
	}

	private static int parsePort(String token) throws CommandSyntaxException {
		return IntegerArgumentType.integer(1, 65535).parse(new StringReader(token));
	}

	private static String withPort(String uriString, int port) throws CommandSyntaxException {
		try {
			URI uri = new URI(uriString);
			return new URI(
				uri.getScheme(),
				uri.getUserInfo(),
				uri.getHost(),
				port,
				uri.getPath(),
				uri.getQuery(),
				uri.getFragment()).toString();
		} catch (IllegalArgumentException | URISyntaxException e) {
			throw ERROR_INVALID_URI.create();
		}
	}

	private static Collection<ServerPlayer> parsePlayers(CommandSourceStack source, String input) throws CommandSyntaxException {
		StringReader reader = new StringReader(input);
		EntitySelector selector = EntityArgument.players().parse(reader, source);
		reader.skipWhitespace();
		if (reader.canRead())
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);

		List<ServerPlayer> players = selector.findPlayers(source);
		if (players.isEmpty())
			throw EntityArgument.NO_PLAYERS_FOUND.create();

		return players;
	}

	private static int transfer(CommandSourceStack source, String hostname, int port, Collection<ServerPlayer> players) throws CommandSyntaxException {
		if (players.isEmpty()) {
			throw ERROR_NO_PLAYERS.create();
		}

		for (ServerPlayer player : players) {
			player.connection.send(new ClientboundTransferPacket(hostname, port));
		}

		if (players.size() == 1) {
			source.sendSuccess(
				() -> Component.translatable("commands.transfer.success.single", players.iterator().next().getDisplayName(), hostname, port),
				true
			);
		} else {
			source.sendSuccess(() -> Component.translatable("commands.transfer.success.multiple", players.size(), hostname, port), true);
		}

		return players.size();
	}

	private record TransferTarget(String packetHost, int port, Collection<ServerPlayer> players) {
	}
}
