package balance.villager.mixin;

import balance.villager.state.NoVillagerState;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class EntityTypeSpawnMixin<T extends Entity> {

	@Inject(
		method = "create(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/nbt/CompoundTag;Ljava/util/function/Consumer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void balanceVillager$preventVillagerCreationAndSound(
		ServerLevel serverLevel,
		@Nullable CompoundTag compoundTag,
		@Nullable Consumer<T> consumer,
		BlockPos blockPos,
		MobSpawnType mobSpawnType,
		boolean shouldOffsetY,
		boolean shouldOffsetYMore,
		CallbackInfoReturnable<T> cir
	) {
		if ((Object) this == EntityType.VILLAGER && NoVillagerState.isNoVillagers(serverLevel)) {
			cir.setReturnValue(null);
		}
	}
}
