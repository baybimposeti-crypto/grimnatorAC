package com.grimnatorac.platform.fabric.mixins;

import com.grimnatorac.platform.api.world.PlatformChunk;
import com.grimnatorac.platform.api.world.PlatformWorld;
import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import com.grimnatorac.platform.fabric.utils.world.LevelChunkUtil;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.UUID;

@Mixin(Level.class)
@Implements(@Interface(iface = PlatformWorld.class, prefix = "GrimnatorAC$"))
abstract class LevelMixin implements LevelAccessor {

    @Shadow
    public abstract ResourceKey<Level> dimension();

    // Route through ChunkSource (via LevelChunkUtil trampoline) so the call resolves to
    // method_12123, not Level.method_8393. Otherwise the prefix-stripped bridge ends up
    // overriding method_8393 on the target on versions where the runtime mapping aliases
    // isChunkLoaded(II)Z to it, and the body recurses through itself — see issue #2568.
    public boolean GrimnatorAC$isChunkLoaded(int chunkX, int chunkZ) {
        return LevelChunkUtil.hasChunkAt((Level) (Object) this, chunkX, chunkZ);
    }

    public WrappedBlockState GrimnatorAC$getBlockAt(int x, int y, int z) {
        return WrappedBlockState.getByGlobalId(
                Block.getId(getBlockState(new BlockPos(x, y, z)))
        );
    }

    public String GrimnatorAC$getName() {
        return this.dimension().location().toString();
    }

    public @Nullable UUID GrimnatorAC$getUID() {
        throw new UnsupportedOperationException();
    }

    public PlatformChunk GrimnatorAC$getChunkAt(int currChunkX, int currChunkZ) {
        return (PlatformChunk) getChunk(currChunkX, currChunkZ);
    }

    public boolean GrimnatorAC$isLoaded() {
        return GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.getLevel(this.dimension()) != null;
    }
}
