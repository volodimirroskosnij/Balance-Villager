package balance.villager.client;

import balance.villager.BalanceVillager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.item.ItemProperties;

public class BalanceVillagerClient implements ClientModInitializer {
	private static volatile boolean clientNoVillagers = false;

	@Override
	public void onInitializeClient() {
		// Реєструємо предикат моделі предмета: 0.0 = звичайний житель, 1.0 = червоний житель (NO VILLAGER ON)
		ItemProperties.register(
			BalanceVillager.VILLAGER_TOTEM,
			BalanceVillager.id("active"),
			(stack, level, entity, seed) -> clientNoVillagers ? 1.0F : 0.0F
		);

		// Отримуємо пакет синхронізації стану з сервера (при вході у світ та при перемиканні)
		ClientPlayNetworking.registerGlobalReceiver(
			BalanceVillager.SYNC_STATE_PACKET,
			(client, handler, buf, responseSender) -> {
				clientNoVillagers = buf.readBoolean();
			}
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			clientNoVillagers = false;
		});
	}
}
