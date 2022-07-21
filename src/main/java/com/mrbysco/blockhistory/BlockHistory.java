package com.mrbysco.blockhistory;

import com.mojang.logging.LogUtils;
import com.mrbysco.blockhistory.command.HistoryCommands;
import com.mrbysco.blockhistory.config.HistoryConfig;
import com.mrbysco.blockhistory.helper.InventoryHelper;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.tmatesoft.sqljet.core.SqlJetException;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(BlockHistory.MOD_ID)
public class BlockHistory {
	public static final String MOD_ID = "blockhistory";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final File personalFolder = new File(FMLPaths.MODSDIR.get().toFile(), "/blockhistory");

	public BlockHistory() {
		try {
			UserHistoryDatabase.init();
		} catch (SqlJetException e) {
			LOGGER.error(e.getMessage());
		}

		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, HistoryConfig.serverSpec);
		FMLJavaModLoadingContext.get().getModEventBus().register(HistoryConfig.class);

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(this::onCommandEvent);

		//Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remoteVersionString, networkBool) -> true));
	}

	public void onCommandEvent(RegisterCommandsEvent event) {
		HistoryCommands.initializeCommands(event.getDispatcher());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockEvent.BreakEvent event) {
		if (!event.getLevel().isClientSide()) {
			Player player = event.getPlayer();
			if (player != null && !(player instanceof FakePlayer)) {
				String username = player.getName().getString();
				ChangeStorage changeData = new ChangeStorage(getDate(), username, "break", ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()));
				UserHistoryDatabase.addHistory(event.getPos().asLong(), changeData);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
		if (!event.getLevel().isClientSide()) {
			Entity entity = event.getEntity();
			if (entity instanceof Player player && !(entity instanceof FakePlayer)) {

				String username = player.getName().getString();
				ChangeStorage changeData = new ChangeStorage(getDate(), username, "place", ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock()));
				UserHistoryDatabase.addHistory(event.getPos().asLong(), changeData);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onMultiBlockPlace(final BlockEvent.EntityMultiPlaceEvent event) {
		if (!event.getLevel().isClientSide()) {
			Entity entity = event.getEntity();
			if (entity instanceof Player player && !(entity instanceof FakePlayer)) {

				for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
					String username = player.getName().getString();
					ChangeStorage changeData = new ChangeStorage(getDate(), username, "place", ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock()));
					UserHistoryDatabase.addHistory(snapshot.getPos().asLong(), changeData);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onExplosionEvent(final ExplosionEvent.Detonate event) {
		if (!event.getLevel().isClientSide() && HistoryConfig.SERVER.storeExplosions.get()) {
			Entity entity = event.getExplosion().getDamageSource().getEntity();
			if (entity != null) {
				final Level world = event.getLevel();
				if (entity instanceof Player player && !(entity instanceof FakePlayer)) {
					for (BlockPos position : event.getAffectedBlocks()) {
						String username = player.getName().getString();
						BlockState state = world.getBlockState(position);
						ResourceLocation resourceLoc = ForgeRegistries.BLOCKS.getKey(state.getBlock());
						ChangeStorage changeData = new ChangeStorage(getDate(), username, "explosion", resourceLoc != null ? resourceLoc : new ResourceLocation("minecraft", "air"));
						UserHistoryDatabase.addHistory(position.asLong(), changeData);
					}
				} else {

					if (ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) != null) {
						String mobName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
						for (BlockPos position : event.getAffectedBlocks()) {
							BlockState state = world.getBlockState(position);
							ResourceLocation resourceLoc = ForgeRegistries.BLOCKS.getKey(state.getBlock());
							ChangeStorage changeData = new ChangeStorage(getDate(), mobName, "explosion", resourceLoc != null ? resourceLoc : new ResourceLocation("minecraft", "air"));
							UserHistoryDatabase.addHistory(position.asLong(), changeData);
						}
					}
				}
			}
		}
	}

	private static final Map<UUID, Long> CONTAINER_PLACE_MAP = new HashMap<>();
	private static final Map<UUID, NonNullList<ItemStack>> CONTAINER_MAP = new HashMap<>();

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
		if (!event.getLevel().isClientSide() && HistoryConfig.SERVER.storeContainerInteractions.get()) {
			final Player player = event.getEntity();
			if (player != null && !(player instanceof FakePlayer) && !player.isShiftKeyDown()) {
				final Level world = event.getLevel();
				final BlockPos position = event.getPos();
				BlockState state = world.getBlockState(position);
				if (state.getMenuProvider(world, position) != null) {
					if (HistoryConfig.SERVER.storeContainerInventoryChanges.get()) {
						CONTAINER_PLACE_MAP.put(player.getUUID(), position.asLong());
					}
					String username = player.getName().getString();
					ChangeStorage changeData = new ChangeStorage(getDate(), username, "containeropen", ForgeRegistries.BLOCKS.getKey((state.getBlock())));
					UserHistoryDatabase.addHistory(position.asLong(), changeData);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerContainerOpen(final PlayerContainerEvent.Open event) {
		final Player player = event.getEntity();
		if (!player.getCommandSenderWorld().isClientSide() && HistoryConfig.SERVER.storeContainerInventoryChanges.get()) {
			AbstractContainerMenu container = event.getContainer();
			if (container.getItems().size() >= 1) {
				CONTAINER_MAP.put(player.getUUID(), InventoryHelper.getContainerInventory(container));
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerContainerClose(final PlayerContainerEvent.Close event) {
		final Player player = event.getEntity();
		final Level world = player.getCommandSenderWorld();
		if (!world.isClientSide() && HistoryConfig.SERVER.storeContainerInventoryChanges.get()) {
			UUID playerUUID = player.getUUID();
			NonNullList<ItemStack> oldInventory = CONTAINER_MAP.getOrDefault(playerUUID, null);
			final AbstractContainerMenu container = event.getContainer();
			if (CONTAINER_PLACE_MAP.containsKey(playerUUID) && oldInventory != null && container != null) {
				NonNullList<ItemStack> currentInventory = InventoryHelper.getContainerInventory(container);
				int oldCount = InventoryHelper.getItemCount(oldInventory);
				int newCount = InventoryHelper.getItemCount(currentInventory);
				if (oldCount != newCount) {
					NonNullList<ItemStack> differenceList = InventoryHelper.getInventoryChange(oldInventory, currentInventory);
					String username = player.getName().getString();
					BlockPos position = BlockPos.of(CONTAINER_PLACE_MAP.get(playerUUID));
					ResourceLocation location = ForgeRegistries.BLOCKS.getKey(world.getBlockState(position).getBlock());
					ChangeStorage changeData = null;
					if (newCount < oldCount) {
						changeData = new ChangeStorage(getDate(), username, "inventory_withdrawal", location, differenceList.toString());
//                        LOGGER.debug("User withdrew the following: {}",  differenceList);
					}
					if (newCount > oldCount) {
						changeData = new ChangeStorage(getDate(), username, "inventory_insertion", location, differenceList.toString());
//                        LOGGER.debug("User inserted the following: {}", differenceList);
					}
					if (changeData != null) {
						UserHistoryDatabase.addHistory(position.asLong(), changeData);
					}
				}
			}
			CONTAINER_MAP.remove(playerUUID);
		}
	}

	public String getDate() {
		Date date = Calendar.getInstance().getTime();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return dateFormat.format(date);
	}
}
