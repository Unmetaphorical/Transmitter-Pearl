package org.yellowstackdevelopment.transmitterPearl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yellowstackdevelopment.transmitterPearl.command.LinkCommand;
import org.yellowstackdevelopment.transmitterPearl.item.TransmitterPearlItem;

import java.util.function.UnaryOperator;

public class TransmitterPearl implements ModInitializer {
    public static final String MOD_ID = "transmitter-pearl";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final RegistryKey<ItemGroup> ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "item_group"));

    public static final GameRules.Key<GameRules.BooleanRule> LINK_TRUST = GameRuleRegistry.register("linkTrust", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(false));
    public static final GameRules.Key<GameRules.BooleanRule> LINK_PEARL_CRAFTING = GameRuleRegistry.register("linkPearlCrafting", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static final Item TRANSMITTER_PEARL = registerItem("transmitter_pearl",
            new TransmitterPearlItem(new Item.Settings().maxCount(1).maxDamage(10)));

    public static final ComponentType<TransmitterPearlItem.LinkData> TRANSMITTER_LINK = registerComponent("transmitter_link",
            builder -> builder.codec(TransmitterPearlItem.LinkData.CODEC).packetCodec(TransmitterPearlItem.LinkData.PACKET_CODEC));

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Transmitter Pearl...");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LinkCommand.register(dispatcher);
        });

        Registry.register(Registries.ITEM_GROUP, ITEM_GROUP_KEY, FabricItemGroup.builder()
                .icon(() -> new ItemStack(TRANSMITTER_PEARL))
                .displayName(Text.translatable("itemGroup.transmitter_pearl.item_group"))
                .entries((displayContext, entries) -> {
                    entries.add(TRANSMITTER_PEARL);
                })
                .build());
    }

    private static <T> ComponentType<T> registerComponent(String name, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(MOD_ID, name), (builderOperator.apply(ComponentType.builder())).build());
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item);
    }
}
