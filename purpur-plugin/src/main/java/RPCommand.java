import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.nio.charset.StandardCharsets;

public class RPCommand extends JavaPlugin implements Listener, PluginMessageListener {
    private final Set<UUID> justJoined = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createConfigIfNotExists();
        Bukkit.getPluginManager().registerEvents(this, this);
        // 註冊 Plugin Messaging Channel
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "rpcommand:main", this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "rpcommand:main");
    }

    private void createConfigIfNotExists() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource("config.yml");
                 FileOutputStream out = new FileOutputStream(configFile)) {
                if (in != null) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 延遲5秒後才加入 justJoined
        new BukkitRunnable() {
            @Override
            public void run() {
                justJoined.add(player.getUniqueId());
            }
        }.runTaskLater(this, 20 * 3); // 3秒（20 tick = 1秒）
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (justJoined.contains(uuid)) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                justJoined.remove(uuid);
                FileConfiguration config = getConfig();
                List<String> commands = config.getStringList("commands");
                for (String cmd : commands) {
                    String command = cmd.replace("%player%", player.getName());
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rpcreload")) {
            reloadConfig();
            sender.sendMessage("§a[RPCommand] 設定已重新載入！");
            return true;
        }
        return false;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("rpcommand:main")) return;
        // 只允許來自代理的訊息（通常 player 會是任一在線玩家）
        String command = new String(message, StandardCharsets.UTF_8);
        // 以 Console 身份執行
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }
} 