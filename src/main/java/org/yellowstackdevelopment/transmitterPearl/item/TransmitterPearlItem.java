package org.yellowstackdevelopment.transmitterPearl.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.yellowstackdevelopment.transmitterPearl.TransmitterPearl;
import org.yellowstackdevelopment.transmitterPearl.command.LinkCommand;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Objects;

public class TransmitterPearlItem extends Item {
    public TransmitterPearlItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.REDSTONE_BLOCK) || super.canRepair(stack, ingredient);
    }

    private static final WeakHashMap<PlayerEntity, LinkData> PLAYER_SELECTIONS = new WeakHashMap<>();

    public record LinkData(Optional<UUID> id1, Optional<String> name1, Optional<UUID> id2, Optional<String> name2) {
        public static final LinkData EMPTY = new LinkData(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<LinkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Uuids.CODEC.optionalFieldOf("id1").forGetter(LinkData::id1),
                Codec.STRING.optionalFieldOf("name1").forGetter(LinkData::name1),
                Uuids.CODEC.optionalFieldOf("id2").forGetter(LinkData::id2),
                Codec.STRING.optionalFieldOf("name2").forGetter(LinkData::name2)
        ).apply(instance, LinkData::new));

        public static final PacketCodec<net.minecraft.network.RegistryByteBuf, LinkData> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.optional(Uuids.PACKET_CODEC), LinkData::id1,
                PacketCodecs.optional(PacketCodecs.STRING), LinkData::name1,
                PacketCodecs.optional(Uuids.PACKET_CODEC), LinkData::id2,
                PacketCodecs.optional(PacketCodecs.STRING), LinkData::name2,
                LinkData::new
        );

        public LinkData with1(UUID id, String name) {
            return new LinkData(Optional.of(id), Optional.of(name), this.id2, this.name2);
        }

        public LinkData with2(UUID id, String name) {
            return new LinkData(this.id1, this.name1, Optional.of(id), Optional.of(name));
        }

        public boolean contains(UUID uuid) {
            return (id1.isPresent() && id1.get().equals(uuid)) || (id2.isPresent() && id2.get().equals(uuid));
        }
        
        public boolean isEmpty() {
            return id1.isEmpty() && id2.isEmpty();
        }
    }

    public static void clearAllLinksToPlayer(ServerPlayerEntity player) {
        PLAYER_SELECTIONS.values().removeIf(data -> data.contains(player.getUuid()));
    }

    private void updateLink(ItemStack stack, PlayerEntity user, Entity target) {
        if (user.getWorld().isClient) return;
        if (target instanceof ServerPlayerEntity targetPlayer && user instanceof ServerPlayerEntity requester) {
            boolean trustEnabled = user.getWorld().getGameRules().getBoolean(TransmitterPearl.LINK_TRUST);
            if (trustEnabled && !LinkCommand.isTrusted(targetPlayer, requester)) {
                user.sendMessage(Text.literal("§c" + targetPlayer.getName().getString() + " does not trust you!"), true);
                return;
            }
        }

        LinkData current = stack.getOrDefault(TransmitterPearl.TRANSMITTER_LINK, LinkData.EMPTY);
        if (current.isEmpty()) {
            LinkData stored = PLAYER_SELECTIONS.get(user);
            if (stored != null) current = stored;
        }

        UUID uuid = target.getUuid();
        String name = target.getName().getString();
        LinkData next;

        if (current.id1().isEmpty()) {
            next = current.with1(uuid, name);
            user.sendMessage(Text.literal("§bLinked Target 1: " + name), true);
        } else if (current.id2().isEmpty()) {
            if (uuid.equals(current.id1().get())) {
                user.sendMessage(Text.literal("§cAlready linked!"), true);
                return;
            }
            next = current.with2(uuid, name);
            user.sendMessage(Text.literal("§dLinked Target 2: " + name), true);
        } else {
            if (uuid.equals(current.id1().get()) || uuid.equals(current.id2().get())) {
                user.sendMessage(Text.literal("§cAlready in link!"), true);
                return;
            }
            next = new LinkData(current.id2(), current.name2(), Optional.of(uuid), Optional.of(name));
            user.sendMessage(Text.literal("§5Link Updated: " + name), true);
        }

        stack.set(TransmitterPearl.TRANSMITTER_LINK, next);
        PLAYER_SELECTIONS.put(user, next);

        user.getWorld().playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.5f);
        if (target instanceof LivingEntity) {
            ((ServerWorld)user.getWorld()).spawnParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1, target.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
        }
        user.getItemCooldownManager().set(this, 10);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getItemCooldownManager().isCoolingDown(this)) return ActionResult.FAIL;
        updateLink(stack, user, entity);
        return user.isSneaking() ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity user = context.getPlayer();
        if (user != null && user.isSneaking()) {
            if (user.getItemCooldownManager().isCoolingDown(this)) return ActionResult.FAIL;
            updateLink(context.getStack(), user, user);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.isSneaking()) {
            if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
            updateLink(stack, user, user);
            return TypedActionResult.success(stack, !world.isClient);
        }
        return TypedActionResult.pass(stack);
    }

    private boolean tryDamage(ItemStack stack, PlayerEntity player, int amount) {
        if (player.getAbilities().creativeMode) return true;
        if (stack.getDamage() + amount >= stack.getMaxDamage()) {
            return false;
        }
        stack.setDamage(stack.getDamage() + amount);
        return true;
    }

    public void processTransmission(PlayerEntity player, ItemStack stack) {
        if (player.getWorld().isClient) return;

        LinkData data = stack.getOrDefault(TransmitterPearl.TRANSMITTER_LINK, PLAYER_SELECTIONS.getOrDefault(player, LinkData.EMPTY));
        if (data.id1().isPresent() && data.id2().isPresent()) {
            Entity e1 = findEntity(player.getWorld(), data.id1().get());
            Entity e2 = findEntity(player.getWorld(), data.id2().get());

            if (e1 != null && e2 != null && e1.isAlive() && e2.isAlive()) {
                transmit(e1, e2);
                player.sendMessage(Text.literal("§aTransmission Successful!"), true);
                
                if (tryDamage(stack, player, 1)) {
                    player.getItemCooldownManager().set(this, 10);
                } else {
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 0.5f);
                    ItemStack replacement = new ItemStack(Items.ENDER_PEARL);
                    Hand hand = player.getMainHandStack() == stack ? Hand.MAIN_HAND : Hand.OFF_HAND;
                    player.setStackInHand(hand, net.minecraft.item.ItemUsage.exchangeStack(stack, player, replacement));
                }
            } else {
                player.sendMessage(Text.literal("§cLink broken: Target lost."), true);
            }
        }
    }

    public void clearLinks(PlayerEntity player, ItemStack stack) {
        if (player.getWorld().isClient) return;
        stack.set(TransmitterPearl.TRANSMITTER_LINK, LinkData.EMPTY);
        PLAYER_SELECTIONS.put(player, LinkData.EMPTY);
        player.sendMessage(Text.literal("§7Links Terminated"), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        player.getItemCooldownManager().set(this, 10);
    }

    private Entity findEntity(World world, UUID uuid) {
        if (world.getServer() == null) return null;
        for (ServerWorld sw : world.getServer().getWorlds()) {
            Entity e = sw.getEntity(uuid);
            if (e != null) return e;
        }
        return world.getServer().getPlayerManager().getPlayer(uuid);
    }

    private void transmit(Entity a, Entity b) {
        Vec3d posA = a.getPos();
        Vec3d posB = b.getPos();
        World worldA = a.getWorld();
        World worldB = b.getWorld();

        if (worldA == worldB) {
            a.requestTeleport(posB.x, posB.y, posB.z);
            b.requestTeleport(posA.x, posA.y, posA.z);
        } else {
            a.teleport((ServerWorld)worldB, posB.x, posB.y, posB.z, EnumSet.noneOf(PositionFlag.class), a.getYaw(), a.getPitch());
            b.teleport((ServerWorld)worldA, posA.x, posA.y, posA.z, EnumSet.noneOf(PositionFlag.class), b.getYaw(), b.getPitch());
        }

        playTransmissionEffects(worldA, posA);
        playTransmissionEffects(worldB, posB);
    }

    private void playTransmissionEffects(World world, Vec3d pos) {
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1, pos.z, 30, 0.3, 0.5, 0.3, 0.1);
            world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !selected || !(entity instanceof PlayerEntity player)) return;
        
        LinkData playerLink = PLAYER_SELECTIONS.get(player);
        if (playerLink != null && !Objects.equals(stack.get(TransmitterPearl.TRANSMITTER_LINK), playerLink)) {
             stack.set(TransmitterPearl.TRANSMITTER_LINK, playerLink);
        }

        LinkData data = stack.getOrDefault(TransmitterPearl.TRANSMITTER_LINK, LinkData.EMPTY);
        if (data.id1().isPresent()) {
            String msg = "§bL1: " + data.name1().get();
            if (data.id2().isPresent()) {
                msg += " §f<-> §dL2: " + data.name2().get() + " §7(L-Click to Transmit)";
            } else {
                msg += " §7(R-Click for Link 2)";
            }
            player.sendMessage(Text.literal(msg), true);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        LinkData data = stack.get(TransmitterPearl.TRANSMITTER_LINK);
        if (data != null) {
            if (data.id1().isPresent()) {
                tooltip.add(Text.literal("§bLinked 1: " + data.name1().get()));
                tooltip.add(Text.literal("§8> " + data.id1().get().toString()));
            }
            if (data.id2().isPresent()) {
                tooltip.add(Text.literal("§dLinked 2: " + data.name2().get()));
                tooltip.add(Text.literal("§8> " + data.id2().get().toString()));
            }
        }
        tooltip.add(Text.literal("§7Right-click entities to establish a link."));
        tooltip.add(Text.literal("§7Shift + Right-click air to link yourself."));
        tooltip.add(Text.literal("§7Left-click to transmit positions."));
        tooltip.add(Text.literal("§7Shift + Left-click to terminate links."));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        LinkData data = stack.get(TransmitterPearl.TRANSMITTER_LINK);
        return data != null && data.id1().isPresent();
    }
}
