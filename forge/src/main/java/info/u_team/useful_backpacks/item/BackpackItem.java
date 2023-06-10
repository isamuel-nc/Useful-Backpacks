package info.u_team.useful_backpacks.item;

import java.util.List;

import info.u_team.u_team_core.api.dye.DyeableItem;
import info.u_team.u_team_core.item.UItem;
import info.u_team.u_team_core.util.MenuUtil;
import info.u_team.useful_backpacks.config.ServerConfig;
import info.u_team.useful_backpacks.inventory.BackpackInventory;
import info.u_team.useful_backpacks.menu.BackpackMenu;
import info.u_team.useful_backpacks.type.BackpackType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class BackpackItem extends UItem implements AutoPickupBackpack, DyeableItem {
	
	private final BackpackType backpack;
	
	public BackpackItem(BackpackType backpack) {
		super(new Properties().stacksTo(1).rarity(backpack.getRarity()));
		this.backpack = backpack;
		addColoredItem(this);
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide && player instanceof ServerPlayer) {
			open((ServerPlayer) player, stack, hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1);
		}
		return InteractionResultHolder.success(stack);
	}
	
	@Override
	public void open(ServerPlayer player, ItemStack backpackStack, int selectedSlot) {
		MenuUtil.openMenu(player, new SimpleMenuProvider((id, playerInventory, unused) -> {
			return new BackpackMenu(id, playerInventory, getInventory(player, backpackStack), backpack, selectedSlot);
		}, backpackStack.getHoverName()), buffer -> {
			buffer.writeEnum(backpack);
			buffer.writeVarInt(selectedSlot);
		}, false);
	}
	
	@Override
	public Container getInventory(ServerPlayer player, ItemStack backpackStack) {
		return new BackpackInventory(backpackStack, backpack.getInventorySize());
	}
	
	@Override
	public void saveInventory(Container inventory, ItemStack backpackStack) {
		if (inventory instanceof BackpackInventory) {
			((BackpackInventory) inventory).writeItemStack();
		}
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		addTooltip(stack, level, tooltip, flag);
	}
	
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return !ItemStack.isSameItem(oldStack, newStack);
	}
	
	// Getter
	
	public BackpackType getBackpack() {
		return backpack;
	}
	
	// Default backpack color if not present
	
	@Override
	public int getDefaultColor() {
		return 0x816040;
	}
	
	// Fix bug #22 (too large packet size with certain mod items) and kind of reverted (config option) with #24
	
	@Override
	public CompoundTag getShareTag(ItemStack stack) {
		if (ServerConfig.getInstance().shareAllNBTData.get()) {
			return super.getShareTag(stack);
		}
		if (!stack.hasTag()) {
			return null;
		}
		final CompoundTag compound = stack.getTag().copy();
		compound.remove("Items");
		if (compound.isEmpty()) {
			return null;
		}
		return compound;
	}
	
	// Fix bug #30 (dupe bug when lagging server)
	
	@Override
	public boolean onDroppedByPlayer(ItemStack item, Player player) {
		return !(player.containerMenu instanceof BackpackMenu);
	}
}