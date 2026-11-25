package balance.villager.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface NoVillagerToggleCallback {
	Event<NoVillagerToggleCallback> EVENT = EventFactory.createArrayBacked(
		NoVillagerToggleCallback.class,
		(listeners) -> (level, player, enabled) -> {
			for (NoVillagerToggleCallback listener : listeners) {
				listener.onToggle(level, player, enabled);
			}
		}
	);

	void onToggle(ServerLevel level, ServerPlayer player, boolean enabled);
}
