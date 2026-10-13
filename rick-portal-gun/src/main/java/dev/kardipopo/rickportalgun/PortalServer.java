package dev.kardipopo.rickportalgun;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class PortalServer {
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, PlayerConfig> CONFIGS = new HashMap<>();

    public record PlayerConfig(String dimension, int x, int y, int z, boolean destinationMode) {
        public static PlayerConfig defaults() {
            return new PlayerConfig("minecraft:overworld", 0, 80, 0, false);
        }
    }

    private PortalServer() {}

    public static void initialize() {
        ServerTickEvents.END_WORLD_TICK.register(PortalServer::tick);
        RickChallenge.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("rick")
                        .executes(context -> {
                            context.getSource().sendFeedback(() -> Text.literal(
                                    "§a[Рик C-137] §f/rick challenge — испытание и получение пушки; "
                                            + "/rick config <измерение> <x> <y> <z> <true|false> — настройка портала."
                            ), false);
                            return 1;
                        })
                        .then(literal("challenge").executes(context ->
                                RickChallenge.start(context.getSource().getPlayerOrThrow())))
                        .then(literal("config")
                                .then(argument("dimension", StringArgumentType.word())
                                .then(argument("x", IntegerArgumentType.integer(-30000000, 30000000))
                                .then(argument("y", IntegerArgumentType.integer(-64, 320))
                                .then(argument("z", IntegerArgumentType.integer(-30000000, 30000000))
                                .then(argument("destination", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    String rawDimension = StringArgumentType.getString(context, "dimension");
                                    Identifier identifier;
                                    try {
                                        identifier = Identifier.of(rawDimension);
                                    } catch (IllegalArgumentException exception) {
                                        player.sendMessage(Text.literal("§c[Рик C-137] Такого измерения нет."));
                                        return 0;
                                    }

                                    RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, identifier);
                                    if (context.getSource().getServer().getWorld(key) == null) {
                                        player.sendMessage(Text.literal("§c[Рик C-137] Это измерение не загружено на сервере."));
                                        return 0;
                                    }

                                    PlayerConfig config = new PlayerConfig(
                                            identifier.toString(),
                                            IntegerArgumentType.getInteger(context, "x"),
                                            IntegerArgumentType.getInteger(context, "y"),
                                            IntegerArgumentType.getInteger(context, "z"),
                                            BoolArgumentType.getBool(context, "destination")
                                    );
                                    CONFIGS.put(player.getUuid(), config);
                                    player.sendMessage(Text.literal(config.destinationMode()
                                            ? "§a[Рик C-137] Маршрут задан: " + config.dimension() + " "
                                                + config.x() + " " + config.y() + " " + config.z()
                                            : "§a[Рик C-137] Режим парных порталов включён."));
                                    return 1;
                                })))))))));
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            server.execute(() -> {
                if (!RickChallenge.hasPortalGun(player)) {
                    player.sendMessage(Text.literal("§a[Рик C-137] §fХочешь штуку, которая ломает пространство?"));
                    player.sendMessage(Text.literal("§a[Рик C-137] §fДокажи, что не полный Морти: §e/rick challenge"));
                }
            });
        });
    }

    public static PlayerConfig getConfig(UUID playerId) {
        return CONFIGS.getOrDefault(playerId, PlayerConfig.defaults());
    }

    public static ServerWorld resolveWorld(net.minecraft.server.MinecraftServer server, String dimension) {
        try {
            Identifier id = Identifier.of(dimension);
            return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void tick(ServerWorld world) {
        COOLDOWNS.replaceAll((uuid, value) -> value - 1);
        COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= 0);
        RickChallenge.tick(world);

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (COOLDOWNS.containsKey(player.getUuid())) continue;

            Box area = player.getBoundingBox().expand(1.65);
            for (ArmorStandEntity stand : world.getEntitiesByType(EntityType.ARMOR_STAND, area, PortalMarker::is)) {
                PortalMarker marker = PortalMarker.read(stand);
                if (marker == null || marker.target() == null) continue;
                if (player.squaredDistanceTo(stand) > 2.25 * 2.25) continue;

                teleport(player, marker, world);
                break;
            }
        }
    }

    private static void teleport(ServerPlayerEntity player, PortalMarker source, ServerWorld sourceWorld) {
        String dimension = source.targetDimension() == null
                ? sourceWorld.getRegistryKey().getValue().toString()
                : source.targetDimension();
        ServerWorld targetWorld = resolveWorld(sourceWorld.getServer(), dimension);
        if (targetWorld == null) {
            player.sendMessage(Text.literal("§c[Рик C-137] Портал не может найти измерение назначения."));
            COOLDOWNS.put(player.getUuid(), 20);
            return;
        }

        Vec3d target = source.target();
        double x = target.x;
        double y = target.y;
        double z = target.z;

        player.teleport(targetWorld, x, y, z, Set.of(), player.getYaw(), player.getPitch(), true);
        player.setVelocity(Vec3d.ZERO);
        COOLDOWNS.put(player.getUuid(), 16);

        targetWorld.playSound(null, x, y, z,
                ModSounds.PORTAL_CREATE, SoundCategory.PLAYERS, 0.65f, 0.72f);
    }
}
