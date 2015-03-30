package org.mcteam.ancientgates;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.mcteam.ancientgates.util.DiscUtil;
import org.mcteam.ancientgates.util.FloodUtil;
import org.mcteam.ancientgates.util.TextUtil;
import org.mcteam.ancientgates.util.types.CommandType;
import org.mcteam.ancientgates.util.types.FloodOrientation;
import org.mcteam.ancientgates.util.types.GateMaterial;
import org.mcteam.ancientgates.util.types.InvBoolean;
import org.mcteam.ancientgates.util.types.TeleportType;
import org.mcteam.ancientgates.util.types.WorldCoord;

import com.google.gson.reflect.TypeToken;

public class Gate {

	private static transient TreeMap<String, Gate> instances = new TreeMap<String, Gate>(String.CASE_INSENSITIVE_ORDER);
	private static transient File file = new File(Plugin.instance.getDataFolder(), "gates.json");
	private static transient String SERVER = "server";
	private static transient String WORLD = "world";
	private static transient String X = "x";
	private static transient String Y = "y";
	private static transient String Z = "z";
	private static transient String YAW = "yaw";
	private static transient String PITCH = "pitch";

	// Gates
	private transient String id;
	private List<Location> froms;
	private List<Location> tos;
	private List<Map<String, String>> bungeetos;
	private TeleportType bungeetype;
	private Boolean entities = Conf.teleportEntitiesDefault;
	private Boolean vehicles = Conf.teleportVehiclesDefault;
	private InvBoolean inventory = Conf.teleportInventoryDefault;
	private GateMaterial material = Conf.gateMaterialDefault;
	private String command;
	private CommandType commandtype;
	private String msg;
	private double cost = 0.0;

	// Legacy entries
	private Location to;
	private Location from;
	private Map<String, String> bungeeto;

	private transient Set<WorldCoord> frameBlockCoords;
	private transient Set<WorldCoord> surroundingFrameBlockCoords;
	private transient Map<WorldCoord, FloodOrientation> portalBlockCoords;
	private transient Set<WorldCoord> surroundingPortalBlockCoords;

	public Gate() {
	}

	public void addBungeeTo(String server, String to) {
		if (this.bungeetos == null) {
			this.bungeetos = new ArrayList<Map<String, String>>();
		}
		if (to == null) {
			this.bungeetos = null;
			this.bungeetype = null;
		} else {
			String[] parts = to.split(",");
			Map<String, String> bungeeto = new HashMap<String, String>();
			bungeeto.put(SERVER, server);
			bungeeto.put(WORLD, parts[0]);
			bungeeto.put(X, parts[1]);
			bungeeto.put(Y, parts[2]);
			bungeeto.put(Z, parts[3]);
			bungeeto.put(YAW, parts[4]);
			bungeeto.put(PITCH, parts[5]);
			this.bungeetos.add(bungeeto);
			if (this.bungeetype==null) {
				this.bungeetype = Conf.bungeeTeleportDefault;
			}
		}
	}

	public void addFrom(Location from) {
		if (this.froms == null) {
			this.froms = new ArrayList<Location>();
		}
		if (from == null) {
			this.froms = null;
		} else {
			this.froms.add(from);
		}
	}

	public void addTo(Location to) {
		if (this.tos == null) {
			this.tos = new ArrayList<Location>();
		}
		if (to == null) {
			this.tos = null;
		} else {
			this.tos.add(to);
		}
	}

	public void dataClear() {
		portalBlockCoords = new HashMap<WorldCoord, FloodOrientation>();
		frameBlockCoords = new HashSet<WorldCoord>();
		surroundingPortalBlockCoords = new HashSet<WorldCoord>();
		surroundingFrameBlockCoords = new HashSet<WorldCoord>();
	}

	//----------------------------------------------//
	// The Block data management
	//----------------------------------------------//
	public boolean dataPopulate() {
		// Clear previous data
		dataClear();

		if (froms == null) {
			return false;
		}

		// Loop through all from locations
		for (Location from : froms) {
			Entry<FloodOrientation, Set<Block>> flood = FloodUtil.getBestAirFlood(from.getBlock(), FloodOrientation.values());
			if (flood == null) {
				return false;
			}

			// Force vertical PORTALs and horizontal ENDER_PORTALs
			FloodOrientation orientation = flood.getKey();

			// Now we add the portal blocks as world coords to the lookup maps.
			Set<Block> portalBlocks = FloodUtil.getPortalBlocks(from.getBlock(), orientation);
			if (portalBlocks == null) {
				return false;
			}

			for (Block portalBlock : portalBlocks) {
				portalBlockCoords.put(new WorldCoord(portalBlock), orientation);
			}

			// Now we add the frame blocks as world coords to the lookup maps.
			Set<Block> frameBlocks = FloodUtil.getFrameBlocks(portalBlocks, orientation);
			for (Block frameBlock : frameBlocks) {
				frameBlockCoords.add(new WorldCoord(frameBlock));
			}

			// Now we add the surrounding blocks as world coords to the lookup maps.
			Set<Block> surroundingBlocks = FloodUtil.getSurroundingBlocks(portalBlocks, frameBlocks, orientation);
			for (Block surroundingBlock : surroundingBlocks) {
				surroundingPortalBlockCoords.add(new WorldCoord(surroundingBlock));
			}
			surroundingBlocks = FloodUtil.getSurroundingBlocks(frameBlocks, portalBlocks, orientation);
			for (Block surroundingBlock : surroundingBlocks) {
				surroundingFrameBlockCoords.add(new WorldCoord(surroundingBlock));
			}
		}
		return true;
	}

	public void delBungeeTo(String server, String to) {
		String[] parts = to.split(",");
		Map<String, String> bungeeto = new HashMap<String, String>();
		bungeeto.put(SERVER, server);
		bungeeto.put(WORLD, parts[0]);
		bungeeto.put(X, parts[1]);
		bungeeto.put(Y, parts[2]);
		bungeeto.put(Z, parts[3]);
		bungeeto.put(YAW, parts[4]);
		bungeeto.put(PITCH, parts[5]);
		this.bungeetos.remove(bungeeto);
		if (this.bungeetos.size() == 0) {
			this.bungeetos = null;
		}
	}

	public void delFrom(Location from) {
		this.froms.remove(from);
	}

	public void delTo(Location to) {
		this.tos.remove(to);
		if (this.tos.size() == 0) {
			this.tos = null;
		}
	}

	public Map<String, String> getBungeeTo() {
		if (bungeetos == null) {
			return null;
		}
		Random randomizer = new Random();
		return bungeetos.get(randomizer.nextInt(bungeetos.size()));
	}

	public List<Map<String, String>> getBungeeTos() {
		return bungeetos;
	}

	public TeleportType getBungeeType() {
		return bungeetype;
	}

	public String getCommand() {
		return command;
	}

	public CommandType getCommandType() {
		return commandtype;
	}

	public Double getCost() {
		return cost;
	}

	public Set<WorldCoord> getFrameBlocks() {
		return frameBlockCoords;
	}

	public List<Location> getFroms() {
		return froms;
	}

	public String getId() {
		return id;
	}

	public Material getMaterial() {
		return material.getMaterial();
	}

	public String getMaterialStr() {
		return material.name();
	}

	public String getMessage() {
		return msg;
	}

	public Map<WorldCoord, FloodOrientation> getPortalBlocks() {
		return portalBlockCoords;
	}

	public Set<WorldCoord> getSurroundingFrameBlocks() {
		return surroundingFrameBlockCoords;
	}

	public Set<WorldCoord> getSurroundingPortalBlocks() {
		return surroundingPortalBlockCoords;
	}

	public Boolean getTeleportEntities() {
		return entities;
	}

	public InvBoolean getTeleportInventory() {
		return inventory;
	}

	public Boolean getTeleportVehicles() {
		return vehicles;
	}

	public Location getTo() {
		if (tos == null) {
			return null;
		}
		Random randomizer = new Random();
		return tos.get(randomizer.nextInt(tos.size()));
	}

	public List<Location> getTos() {
		return tos;
	}

	public void rename(String id, String newid) {
		Gate gate = instances.remove(id);
		instances.put(newid, gate);
		this.id = newid;
	}

	public void setBungeeType(String bungeeType) {
		this.bungeetype = TeleportType.fromName(bungeeType.toUpperCase());
	}

	public void setCommand(String command) {
		this.command = (command.isEmpty()) ? null : command;
		if (command.isEmpty()) {
			this.commandtype = null;
		}
	}

	public void setCommandType(String commandType) {
		this.commandtype = CommandType.fromName(commandType.toUpperCase());
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

	// -------------------------------------------- //
	// Getters And Setters
	// -------------------------------------------- //
	public void setId(String id) {
		this.id = id;
	}

	public void setMaterial(String material) {
		this.material = GateMaterial.fromName(material.toUpperCase());
	}

	public void setMessage(String msg) {
		this.msg = (msg.isEmpty()) ? null : msg;
	}

	public void setTeleportEntities(Boolean teleportEntities) {
		this.entities = teleportEntities;
	}

	public void setTeleportInventory(String teleportInventory) {
		this.inventory = InvBoolean.fromName(teleportInventory.toUpperCase());
	}

	public void setTeleportVehicles(Boolean teleportVehicles) {
		this.vehicles = teleportVehicles;
	}

	public static Gate create(String id) {
		Gate gate = new Gate();
		gate.id = id;
		instances.put(gate.id, gate);
		Plugin.log("created new gate "+gate.id);
		return gate;
	}

	public static void delete(String id) {
		// Remove the gate
		instances.remove(id);
	}

	public static boolean exists(String id) {
		return instances.containsKey(id);
	}

	public static void fillIds() {
		for(Entry<String, Gate> entry : instances.entrySet()) {
			entry.getValue().setId(entry.getKey());
		}
	}

	//----------------------------------------------//
	// Persistance and entity management
	//----------------------------------------------//
	public static Gate get(String id) {
		return instances.get(id);
	}

	public static Collection<Gate> getAll() {
		return instances.values();
	}

	public static boolean load() {
		Plugin.log("Loading gates from disk");
		if ( ! file.exists()) {
			Plugin.log("No gates to load from disk. Creating new file.");
			save();
			return true;
		}

		try {
			Type type = new TypeToken<Map<String, Gate>>(){}.getType();
			Map<String, Gate> instancesFromFile = Plugin.gson.fromJson(DiscUtil.read(file), type);
			instances.clear();
			instances.putAll(instancesFromFile);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		fillIds();

		// Check enum values
		for (Gate gate : Gate.getAll()) {
			if (gate.material == null) {
				gate.material = GateMaterial.PORTAL;
				Plugin.log(Level.WARNING, "Gate \"" + gate.getId() + "\" { \"material\" } is invalid. Valid materials are: " + TextUtil.implode(Arrays.asList(GateMaterial.names), ", ") + ".");
			}
			if (gate.inventory == null) {
				gate.inventory = InvBoolean.TRUE;
				Plugin.log(Level.WARNING, "Gate \"" + gate.getId() + "\" { \"inventory\" } is invalid. Valid options are: " + TextUtil.implode(Arrays.asList(InvBoolean.names), ", ") + ".");
			}
			if (gate.bungeetos != null && gate.bungeetype == null) {
				gate.bungeetype = TeleportType.LOCATION;
				Plugin.log(Level.WARNING, "Gate \"" + gate.getId() + "\" { \"bungeetype\" } is invalid. Valid types are: " + TextUtil.implode(Arrays.asList(TeleportType.names), ", ") + ".");
			}
			if (gate.command != null && gate.commandtype == null) {
				gate.commandtype = CommandType.PLAYER;
				Plugin.log(Level.WARNING, "Gate \"" + gate.getId() + "\" { \"commandtype\" } is invalid. Valid types are: " + TextUtil.implode(Arrays.asList(CommandType.names), ", ") + ".");
			}
		}

		// Migrate old format
		for (Gate gate : Gate.getAll()) {
			if (gate.from != null) {
				gate.addFrom(gate.from);
				gate.from = null;
			}

			if (gate.to != null) {
				gate.addTo(gate.to);
				gate.to = null;
			}

			if (gate.bungeeto != null) {
				gate.bungeetos = new ArrayList<Map<String, String>>();
				gate.bungeetos.add(gate.bungeeto);
				gate.bungeeto = null;
			}

			if (gate.bungeetos != null && gate.bungeetype == null) {
				gate.bungeetype = Conf.bungeeTeleportDefault;
			}
		}

		// Cleanup non-existent worlds
		for (Gate gate : Gate.getAll()) {
			if (gate.froms != null) {
				Iterator<Location> it = gate.froms.iterator();
				while(it.hasNext()) {
					Location from = it.next();
					if(from == null) {
						it.remove();
					}
				}
				if (gate.froms.isEmpty()) {
					gate.froms = null;
				}
			}

			if (gate.tos != null) {
				Iterator<Location> it = gate.tos.iterator();
				while(it.hasNext()) {
					Location from = it.next();
					if(from == null) {
						it.remove();
					}
				}
				if (gate.tos.isEmpty()) {
					gate.tos = null;
				}
			}
		}

		save();

		return true;
	}

	public static boolean save() {
		try {
			DiscUtil.write(file, Plugin.gson.toJson(instances));
		} catch (IOException e) {
			Plugin.log("Failed to save the gates to disk due to I/O exception.");
			e.printStackTrace();
			return false;
		} catch (NullPointerException e) {
			Plugin.log("Failed to save the gates to disk due to NPE.");
			e.printStackTrace();
			return false;
		}

		return true;
	}

}
