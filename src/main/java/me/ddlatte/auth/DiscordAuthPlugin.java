package me.ddlatte.auth;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordAuthPlugin extends JavaPlugin {
    private DiscordBot discordBot;

    @Override
    public void onEnable() {
        getLogger().info("DiscordAuthPlugin has been enabled!");
        discordBot = new DiscordBot();
        discordBot.initialize("YOUR_DISCORD_BOT_TOKEN");
    }

    @Override
    public void onDisable() {
        getLogger().info("DiscordAuthPlugin has been disabled!");
        if (discordBot != null) {
            discordBot.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("verify")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length != 1) {
                player.sendMessage("Usage: /verify <code>");
                return true;
            }

            String code = args[0];
            String userId = ""; // TODO: 플레이어의 디스코드 ID를 가져오는 로직 구현
            if (discordBot.verifyCode(userId, code)) {
                player.sendMessage("Verification successful!");
                // TODO: 추가적인 인증 완료 처리
            } else {
                player.sendMessage("Invalid verification code.");
            }
            return true;
        }
        return false;
    }
}