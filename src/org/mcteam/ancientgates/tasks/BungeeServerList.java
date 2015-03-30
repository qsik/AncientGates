package org.mcteam.ancientgates.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import org.mcteam.ancientgates.Plugin;
import org.mcteam.ancientgates.util.types.PluginMessage;

public class BungeeServerList extends BukkitRunnable {

	private final Plugin plugin;

	public BungeeServerList(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		if (plugin.getServer().getOnlinePlayers().size() == 0) {
			return;
		}

		// Send BungeeCord "GetServers" command
		final PluginMessage msg = new PluginMessage("GetServers");
		plugin.getServer().getOnlinePlayers().iterator().next().sendPluginMessage(plugin, "BungeeCord", msg.toByteArray());
	}

}