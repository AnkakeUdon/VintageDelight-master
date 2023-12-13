package net.ribs.vintagedelight.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import vectorwing.farmersdelight.common.registry.ModBlocks;

import java.util.List;
import java.util.Optional;

public class LushGrassBlock extends GrassBlock {
    public LushGrassBlock(Properties properties) {
        super(properties);
    }
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean isClient) {
        return world.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        BlockPos blockpos = pPos.above();
        ChunkGenerator chunkGenerator = pLevel.getChunkSource().getGenerator();
        Registry<ConfiguredFeature<?, ?>> registry = pLevel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        this.placeFeature(registry, VegetationFeatures.FLOWER_FLOWER_FOREST, pLevel, chunkGenerator, pRandom, blockpos);
        this.placeFeature(registry, VegetationFeatures.FLOWER_MEADOW, pLevel, chunkGenerator, pRandom, blockpos);
        this.placeFeature(registry, VegetationFeatures.PATCH_GRASS, pLevel, chunkGenerator, pRandom, blockpos);
        this.placeFeature(registry, VegetationFeatures.PATCH_TALL_GRASS, pLevel, chunkGenerator, pRandom, blockpos);
        this.placeFeature(registry, VegetationFeatures.FLOWER_DEFAULT, pLevel, chunkGenerator, pRandom, blockpos);
    }

    private void placeFeature(Registry<ConfiguredFeature<?, ?>> featureRegistry, ResourceKey<ConfiguredFeature<?, ?>> featureKey, ServerLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos pos) {
        featureRegistry.getHolder(featureKey).ifPresent(featureHolder -> {
            featureHolder.value().place(level, chunkGenerator, random, pos);
        });
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack itemInHand = player.getItemInHand(hand);
        Item usedItem = itemInHand.getItem();

        if (usedItem instanceof HoeItem) {
            if (!world.isClientSide && world.getBlockState(pos.above()).isAir()) {
                world.setBlock(pos, ModBlocks.RICH_SOIL_FARMLAND.get().defaultBlockState(), 11);
                world.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                itemInHand.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                player.swing(hand, true);
                return InteractionResult.sidedSuccess(false);
            }
        }

        return super.use(state, world, pos, player, hand, hit);
    }
    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!world.isAreaLoaded(pos, 3)) return;
        if (world.getMaxLocalRawBrightness(pos.above()) >= 9) {
            for (int i = 0; i < 4; ++i) {
                BlockPos targetPos = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                if (world.getBlockState(targetPos).is(ModBlocks.RICH_SOIL.get()) && canPropagate(state, world, targetPos)) {
                    world.setBlockAndUpdate(targetPos, this.defaultBlockState());
                }
            }
        }
    }
    private static boolean canPropagate(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockpos = pos.above();
        return canBeGrass(state, world, pos) && !world.getFluidState(blockpos).is(FluidTags.WATER);
    }
    private static boolean canBeGrass(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockpos = pos.above();
        BlockState blockstate = world.getBlockState(blockpos);
        if (blockstate.is(Blocks.SNOW) && blockstate.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        } else if (blockstate.getFluidState().getAmount() == 8) {
            return false;
        } else {
            int lightLevel = LightEngine.getLightBlockInto(world, state, pos, blockstate, blockpos, Direction.UP, blockstate.getLightBlock(world, blockpos));
            return lightLevel < world.getMaxLightLevel();
        }
    }
}
