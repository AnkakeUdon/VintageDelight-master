package net.ribs.vintagedelight.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.ribs.vintagedelight.item.ModTags;
import net.ribs.vintagedelight.recipe.FermentingRecipe;
import net.ribs.vintagedelight.screen.FermentingJarMenu;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FermentingJarBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                if (isInputSlot(slot)) {
                    if (!isRecipeValid()) {
                        resetProgress();
                    }
                }
            }
        }
        private boolean isInputSlot(int slot) {
            return slot >= FIRST_INGREDIENT_SLOT && slot <= CONTAINER_SLOT;
        }
    };
    public static final int FIRST_INGREDIENT_SLOT = 0;
    public static final int LAST_INPUT_SLOT = 5;
    public static final int CONTAINER_SLOT = 6;
    public static final int FIRST_OUTPUT_SLOT = 7;
    public static final int SECOND_OUTPUT_SLOT = 8;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;

        public FermentingJarBlockEntity(BlockPos pPos, BlockState pBlockState) {
            super(ModBlockEntities.FERMENTING_JAR_BE.get(), pPos, pBlockState);
            this.data = new ContainerData() {
                @Override
                public int get(int pIndex) {
                    return switch (pIndex) {
                        case 0 -> FermentingJarBlockEntity.this.progress;
                        case 1 -> FermentingJarBlockEntity.this.maxProgress;
                        default -> 0;
                    };
                }
                @Override
                public void set(int pIndex, int pValue) {
                    switch (pIndex) {
                        case 0 -> FermentingJarBlockEntity.this.progress = pValue;
                        case 1 -> FermentingJarBlockEntity.this.maxProgress = pValue;
                    }
                }
                @Override
                public int getCount() {
                    return 2;
                }
            };
        }
        public List<ItemStack> getRenderStacks() {
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i <= LAST_INPUT_SLOT; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            if (FIRST_OUTPUT_SLOT < itemHandler.getSlots() && !itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT).isEmpty()) {
                stacks.add(itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT));
            }

            return stacks;
        }
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) {
                return LazyOptional.of(() -> new OutputItemHandler(itemHandler)).cast();
            } else {
                // Use the ConditionalItemHandler for sides other than down
                return LazyOptional.of(() -> new ConditionalItemHandler(itemHandler)).cast();
            }
        }
        return super.getCapability(cap, side);
    }


    private class OutputItemHandler implements IItemHandlerModifiable {
            private final ItemStackHandler itemHandler;
            public OutputItemHandler(ItemStackHandler itemHandler) {
                this.itemHandler = itemHandler;
            }
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return itemHandler.extractItem(slot + FIRST_OUTPUT_SLOT, amount, simulate);
            }
            @Override
            public int getSlots() {
                return 2;
            }
            @Override
            public ItemStack getStackInSlot(int slot) {
                return itemHandler.getStackInSlot(slot + FIRST_OUTPUT_SLOT);
            }

            @Override
            public int getSlotLimit(int slot) {
                return itemHandler.getSlotLimit(slot + FIRST_OUTPUT_SLOT);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        }
    private class ConditionalItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler wrappedHandler;
        public ConditionalItemHandler(ItemStackHandler wrappedHandler) {
            this.wrappedHandler = wrappedHandler;
        }
        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            wrappedHandler.setStackInSlot(slot, stack);
        }
        @Override
        public int getSlots() {
            return wrappedHandler.getSlots();
        }
        @Override
        public ItemStack getStackInSlot(int slot) {
            return wrappedHandler.getStackInSlot(slot);
        }
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == FIRST_OUTPUT_SLOT || slot == SECOND_OUTPUT_SLOT) {
                return stack;
            }
            if (slot == CONTAINER_SLOT && !stack.is(ModTags.CONTAINER_ITEMS)) {
                return stack;
            }
            return wrappedHandler.insertItem(slot, stack, simulate);
        }
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return wrappedHandler.extractItem(slot, amount, simulate);
        }
        @Override
        public int getSlotLimit(int slot) {
            return wrappedHandler.getSlotLimit(slot);
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == FIRST_OUTPUT_SLOT || slot == SECOND_OUTPUT_SLOT) {
                return false;
            }
            if (slot == CONTAINER_SLOT) {
                return stack.is(ModTags.CONTAINER_ITEMS);
            }
            return wrappedHandler.isItemValid(slot, stack);
        }
    }
    @Override
        public void onLoad() {
            super.onLoad();
            lazyItemHandler = LazyOptional.of(() -> itemHandler);
        }
        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            lazyItemHandler.invalidate();
        }
        public void drops() {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for(int i = 0; i < itemHandler.getSlots(); i++) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
        @Override
        public Component getDisplayName() {
            return Component.translatable("block.vintagedelight.fermenting_jar");
        }
        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
            return new FermentingJarMenu(pContainerId, pPlayerInventory, this, this.data);
        }
        @Override
        protected void saveAdditional(CompoundTag pTag) {
            super.saveAdditional(pTag);
            pTag.put("inventory", itemHandler.serializeNBT());
            pTag.putInt("fermenting_jar.progress", progress);
        }
        @Override
        public void load(CompoundTag pTag) {
            super.load(pTag);
            itemHandler.deserializeNBT(pTag.getCompound("inventory"));
            progress = pTag.getInt("fermenting_jar.progress");
        }
        public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
            if (!pLevel.isClientSide && hasRecipe()) {
                Optional<FermentingRecipe> recipeOpt = getCurrentRecipe();
                if (recipeOpt.isPresent()) {
                    FermentingRecipe recipe = recipeOpt.get();
                    ItemStack result = recipe.getResultItem(pLevel.registryAccess());
                    ItemStack secondaryResult = recipe.getSecondaryResultItem();
                    maxProgress = recipe.getProcessingTime();
                    increaseCraftingProgress();
                    if (hasProgressFinished() && canOutput(result, secondaryResult)) {
                        craftItem();
                        resetProgress();
                    }
                } else {
                    resetProgress();
                }
            }
        }

        private boolean canOutput(ItemStack result, ItemStack secondaryResult) {
            boolean canOutputPrimary = result.isEmpty() ||
                    (canInsertItemIntoOutputSlot(result.getItem(), FIRST_OUTPUT_SLOT) &&
                            canInsertAmountIntoOutputSlot(result.getCount(), FIRST_OUTPUT_SLOT));
            boolean canOutputSecondary = secondaryResult.isEmpty() ||
                    (canInsertItemIntoOutputSlot(secondaryResult.getItem(), SECOND_OUTPUT_SLOT) &&
                            canInsertAmountIntoOutputSlot(secondaryResult.getCount(), SECOND_OUTPUT_SLOT));

            return canOutputPrimary && canOutputSecondary;
        }
    private boolean isRecipeValid() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Optional<FermentingRecipe> currentRecipe = getCurrentRecipe();
        if (currentRecipe.isPresent()) {
            FermentingRecipe recipe = currentRecipe.get();
            return recipe.matches(inventory, level);
        }
        return false;
    }


    private void craftItem() {
        Optional<FermentingRecipe> recipeOpt = getCurrentRecipe();
        if (!recipeOpt.isPresent()) {
            return;
        }

        FermentingRecipe recipe = recipeOpt.get();
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack secondaryResult = recipe.getSecondaryResultItem();

        if (canOutput(result, secondaryResult)) {
            if (!result.isEmpty()) {
                itemHandler.insertItem(FIRST_OUTPUT_SLOT, result, false);
            }
            if (!secondaryResult.isEmpty()) {
                itemHandler.insertItem(SECOND_OUTPUT_SLOT, secondaryResult, false);
            }
            for (int i = FIRST_INGREDIENT_SLOT; i <= CONTAINER_SLOT; i++) {
                itemHandler.extractItem(i, 1, false);
            }
        }
    }
        private boolean hasRecipe() {
            return getCurrentRecipe().isPresent();
        }
        private Optional<FermentingRecipe> getCurrentRecipe() {
            if (level == null) return Optional.empty();
            RecipeManager recipeManager = level.getRecipeManager();
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }
            return recipeManager.getAllRecipesFor(FermentingRecipe.Type.INSTANCE).stream()
                    .filter(recipe -> recipe.matches(inventory, level))
                    .findFirst();
        }
        private boolean canInsertItemIntoOutputSlot(Item item, int slot) {
            ItemStack existingStack = itemHandler.getStackInSlot(slot);
            if (existingStack.isEmpty()) {
                return true;
            }
            if (!existingStack.is(item)) {
                return false;
            }
            int itemCount = existingStack.getCount();
            int maxStackSize = existingStack.getMaxStackSize();
            return itemCount < maxStackSize;
        }
        private boolean canInsertAmountIntoOutputSlot(int count, int slot) {
            ItemStack existingStack = itemHandler.getStackInSlot(slot);
            if (existingStack.isEmpty()) {
                return true;
            }
            int existingCount = existingStack.getCount();
            int maxStackSize = existingStack.getMaxStackSize();
            return existingCount + count <= maxStackSize;
        }
        private boolean hasProgressFinished() {
            return progress >= maxProgress;
        }
        private void increaseCraftingProgress() {
            progress++;
        }
        private void resetProgress() {
            progress = 0;
        }
        @Nullable
        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }
    }