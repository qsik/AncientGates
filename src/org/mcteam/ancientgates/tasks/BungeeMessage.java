package org.mcteam.ancientgates.tasks;

import java.util.Iterator;
import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;
import org.mcteam.ancientgates.Plugin;
import org.mcteam.ancientgates.util.types.PluginMessage;

public class BungeeMessage extends BukkitRunnable {

	private final Plugin plugin;

	public BungeeMessage(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		if (plugin.getServer().getOnlinePlayers().size() == 0) {
			return;
		}

		List<PluginMessage> msgQueue = Plugin.bungeeMsgQueue;
		Iterator<PluginMessage> it = msgQueue.iterator();

		while (it.hasNext()) {
			PluginMessage msg = it.next();
			plugin.getServer().getOnlinePlayers().iterator().next().sendPluginMessage(plugin, "BungeeCord", msg.toByteArray());

			// Remove from queue
			it.remove();
		}
	}

}