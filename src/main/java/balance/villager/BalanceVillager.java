package balance.villager;

import balance.villager.event.NoVillagerToggleCallback;
import balance.villager.state.NoVillagerState;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalanceVillager implements ModInitializer {
	public static final String MOD_ID = "balance-villager";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceLocation SYNC_STATE_PACKET = id("sync_no_villager_state");

	public static final Item VILLAGER_TOTEM = Registry.register(
		BuiltInRegistries.ITEM,
		id("villager_totem"),
		new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(VILLAGER_TOTEM);
		});

		// Ініціалізуємо та очищаємо O(1) кеш при старті/зупинці сервера
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			NoVillagerState.getServerState(server.overworld());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			NoVillagerState.resetCache();
		});

		// При вході гравця у світ відправляємо поточний стан (щоб текстура тотема одразу відповідала режиму)
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeBoolean(NoVillagerState.isNoVillagers(server.overworld()));
			sender.sendPacket(SYNC_STATE_PACKET, buf);
		});

		// 1. Слухаємо клік ПКМ нашим кастомним предметом VILLAGER_TOTEM
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!stack.is(VILLAGER_TOTEM)) {
				return InteractionResultHolder.pass(stack);
			}

			if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				ServerLevel serverLevel = (ServerLevel) level;
				NoVillagerState state = NoVillagerState.getServerState(serverLevel);

				state.setNoVillagers(!state.noVillagers);

				NoVillagerToggleCallback.EVENT.invoker().onToggle(serverLevel, serverPlayer, state.noVillagers);
			}

			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		});

		// 2. Підписуємося на власну подію: синхронізуємо текстуру на клієнти, виводимо статус і робимо discard()
		NoVillagerToggleCallback.EVENT.register((level, player, enabled) -> {
			for (ServerPlayer onlinePlayer : level.getServer().getPlayerList().getPlayers()) {
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeBoolean(enabled);
				ServerPlayNetworking.send(onlinePlayer, SYNC_STATE_PACKET, buf);
			}

			if (enabled) {
				int removedCount = 0;
				for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
					List<? extends Villager> villagers = serverLevel.getEntities(EntityType.VILLAGER, v -> true);
					for (Villager villager : villagers) {
						villager.discard();
						removedCount++;
					}
				}

				player.displayClientMessage(
					Component.literal("Режим NO VILLAGER: УВІМКНЕНО (видалено жителів: " + removedCount + ")")
						.withStyle(ChatFormatting.RED),
					true
				);
			} else {
				player.displayClientMessage(
					Component.literal("Режим NO VILLAGER: ВИМКНЕНО (спавн жителів дозволено)")
						.withStyle(ChatFormatting.GREEN),
					true
				);
			}
		});

		// 3. Якщо завантажується старий чанк із жителем, поки режим увімкнено — одразу discard()
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Villager && NoVillagerState.isNoVillagers(level)) {
				entity.discard();
			}
		});
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
