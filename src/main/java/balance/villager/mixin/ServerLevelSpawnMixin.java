package balance.villager.mixin;

import balance.villager.state.NoVillagerState;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSpawnMixin {

	@Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
	private void balanceVillager$preventVillagerSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Villager && NoVillagerState.isNoVillagers((ServerLevel) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "addWorldGenChunkEntities", at = @At("HEAD"), argsOnly = true)
	private Stream<Entity> balanceVillager$filterWorldGenVillagers(Stream<Entity> stream) {
		if (NoVillagerState.isNoVillagers((ServerLevel) (Object) this)) {
			return stream.filter(entity -> !(entity instanceof Villager));
		}
		return stream;
	}
}
