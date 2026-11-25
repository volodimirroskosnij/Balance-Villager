package balance.villager.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class NoVillagerState extends SavedData {
	private static final String KEY = "balance_villager_no_villagers";

	// O(1) кеш у пам'яті, щоб не звертатися до DimensionDataStorage при кожному спавні/генерації чанка
	private static volatile boolean cachedActive = false;
	private static volatile boolean cacheInitialized = false;

	public boolean noVillagers = false;

	public void setNoVillagers(boolean enabled) {
		this.noVillagers = enabled;
		cachedActive = enabled;
		cacheInitialized = true;
		this.setDirty();
	}

	public static boolean isNoVillagers(ServerLevel level) {
		if (cacheInitialized) {
			return cachedActive;
		}
		return getServerState(level).noVillagers;
	}

	public static void resetCache() {
		cacheInitialized = false;
		cachedActive = false;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		tag.putBoolean("noVillagers", this.noVillagers);
		return tag;
	}

	public static NoVillagerState createFromNbt(CompoundTag tag) {
		NoVillagerState state = new NoVillagerState();
		state.noVillagers = tag.getBoolean("noVillagers");
		cachedActive = state.noVillagers;
		cacheInitialized = true;
		return state;
	}

	public static NoVillagerState getServerState(ServerLevel level) {
		ServerLevel overworld = level.getServer().overworld();
		NoVillagerState state = overworld.getDataStorage().computeIfAbsent(
			NoVillagerState::createFromNbt,
			NoVillagerState::new,
			KEY
		);
		cachedActive = state.noVillagers;
		cacheInitialized = true;
		return state;
	}
}
