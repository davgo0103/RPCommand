package tw.yourname.velocityexec;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.command.SimpleCommand;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(
        id = "velocityexec",
        name = "VelocityExec",
        version = "1.0",
        description = "Send commands to backend servers via plugin messaging",
        authors = {"shi0103"}
)
public class VelocityExecPlugin {

    private final ProxyServer server;
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("rpcommand:main");

    @Inject
    public VelocityExecPlugin(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL);
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("vexec").build(),
            new VExecCommand()
        );
    }

    public class VExecCommand implements SimpleCommand {
        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            if (args.length < 2) {
                source.sendMessage(Component.text("用法: /vexec <玩家名稱> <指令>", NamedTextColor.RED));
                return;
            }
            String playerName = args[0];
            String command = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            Optional<Player> optPlayer = server.getPlayer(playerName);
            if (!optPlayer.isPresent()) {
                source.sendMessage(Component.text("找不到玩家: " + playerName, NamedTextColor.RED));
                return;
            }
            Player player = optPlayer.get();
            player.getCurrentServer().ifPresent(conn -> conn.sendPluginMessage(CHANNEL, command.getBytes(StandardCharsets.UTF_8)));
            if (source instanceof Player) {
                source.sendMessage(Component.text("已發送指令到 " + playerName + " 所在伺服器。", NamedTextColor.GREEN));
            }
        }
    }
} 