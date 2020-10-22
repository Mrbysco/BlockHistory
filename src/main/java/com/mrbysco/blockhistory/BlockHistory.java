package com.mrbysco.blockhistory;

import com.mrbysco.blockhistory.command.HistoryCommands;
import com.mrbysco.blockhistory.config.HistoryConfig;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.sqljet.core.SqlJetException;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Mod(BlockHistory.MOD_ID)
public class BlockHistory {
    public static final String MOD_ID = "blockhistory";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final File personalFolder = new File(FMLPaths.MODSDIR.get().toFile(), "/blockhistory");

    public BlockHistory() {
        try {
            UserHistoryDatabase.init();
        } catch(SqlJetException e) {
            LOGGER.error(e.getMessage());
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, HistoryConfig.serverSpec);
        FMLJavaModLoadingContext.get().getModEventBus().register(HistoryConfig.class);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onCommandEvent);

        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    public void onCommandEvent(RegisterCommandsEvent event) {
        HistoryCommands.initializeCommands(event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockEvent.BreakEvent event) {
        if(!event.getWorld().isRemote()) {
            PlayerEntity player = event.getPlayer();
            if(player != null && !(player instanceof FakePlayer)) {
                String username = player.getName().getUnformattedComponentText();
                ChangeStorage changeData = new ChangeStorage(getDate(), username, "break", event.getState().getBlock().getRegistryName());
                UserHistoryDatabase.addHistory(event.getPos().toLong(), changeData);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
        if(!event.getWorld().isRemote()) {
            Entity entity = event.getEntity();
            if(entity instanceof PlayerEntity && !(entity instanceof FakePlayer)) {
                PlayerEntity player = (PlayerEntity)entity;

                String username = player.getName().getUnformattedComponentText();
                ChangeStorage changeData = new ChangeStorage(getDate(), username, "place", event.getPlacedBlock().getBlock().getRegistryName());
                UserHistoryDatabase.addHistory(event.getPos().toLong(), changeData);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMultiBlockPlace(final BlockEvent.EntityMultiPlaceEvent event) {
        if(!event.getWorld().isRemote()) {
            Entity entity = event.getEntity();
            if(entity instanceof PlayerEntity && !(entity instanceof FakePlayer)) {
                PlayerEntity player = (PlayerEntity)entity;

                for(BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
                    String username = player.getName().getUnformattedComponentText();
                    ChangeStorage changeData = new ChangeStorage(getDate(), username, "place", event.getPlacedBlock().getBlock().getRegistryName());
                    UserHistoryDatabase.addHistory(snapshot.getPos().toLong(), changeData);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosionEvent(final ExplosionEvent.Detonate event) {
        if(!event.getWorld().isRemote() && HistoryConfig.SERVER.storeExplosions.get()) {
            Entity entity = event.getExplosion().getDamageSource().getTrueSource();
            if(entity != null) {
                World world = event.getWorld();
                if(entity instanceof PlayerEntity && !(entity instanceof FakePlayer)) {
                    PlayerEntity player = (PlayerEntity)entity;

                    for(BlockPos position : event.getAffectedBlocks()) {
                        String username = player.getName().getUnformattedComponentText();
                        BlockState state = world.getBlockState(position);
                        ResourceLocation resourceLoc = state.getBlock().getRegistryName();
                        ChangeStorage changeData = new ChangeStorage(getDate(), username, "explosion", resourceLoc != null ? resourceLoc : new ResourceLocation("minecraft", "air"));
                        UserHistoryDatabase.addHistory(position.toLong(), changeData);
                    }
                } else {
                    if(entity.getType().getRegistryName() != null) {
                        String mobName = entity.getType().getRegistryName().toString();
                        for(BlockPos position : event.getAffectedBlocks()) {
                            BlockState state = world.getBlockState(position);
                            ResourceLocation resourceLoc = state.getBlock().getRegistryName();
                            ChangeStorage changeData = new ChangeStorage(getDate(), mobName, "explosion", resourceLoc != null ? resourceLoc : new ResourceLocation("minecraft", "air"));
                            UserHistoryDatabase.addHistory(position.toLong(), changeData);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
        if(!event.getWorld().isRemote() && HistoryConfig.SERVER.storeContainerInteractions.get()) {
            PlayerEntity player = event.getPlayer();
            if(player != null && !(player instanceof FakePlayer) && !player.isSneaking()) {
                World world = event.getWorld();
                BlockPos position = event.getPos();
                BlockState state =  world.getBlockState(position);
                if(state.getContainer(world, position) != null) {
                    String username = player.getName().getUnformattedComponentText();
                    ChangeStorage changeData = new ChangeStorage(getDate(), username, "containeropen", state.getBlock().getRegistryName());
                    UserHistoryDatabase.addHistory(position.toLong(), changeData);
                }
            }
        }
    }

    public String getDate() {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return dateFormat.format(date);
    }
}
