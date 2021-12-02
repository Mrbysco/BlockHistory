package com.mrbysco.blockhistory.helper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

import java.util.List;

public class InventoryHelper {
	public static NonNullList<ItemStack> getContainerInventory(AbstractContainerMenu container) {
		int regularSlots = 0;
		List<Slot> slots = container.slots;
		for (Slot slot : slots) {
			if (slot.container instanceof Inventory) {
				break;
			} else {
				regularSlots++;
			}
		}
		NonNullList<ItemStack> inventoryList = NonNullList.create();
		List<ItemStack> subList = container.getItems().subList(0, regularSlots);
		for(ItemStack stack : subList) {
			inventoryList.add(stack.copy());
		}
		return inventoryList;
	}

	public static int getItemCount(NonNullList<ItemStack> inventory) {
		int count = 0;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty()) {
				count++;
			}
		}
		return count;
	}

	public static NonNullList<ItemStack> getInventoryChange(NonNullList<ItemStack> originalInventory, NonNullList<ItemStack> inventory) {
		int oldCount = getItemCount(originalInventory);
		int newCount = getItemCount(inventory);
		NonNullList<ItemStack> differenceList = NonNullList.create();

		if(newCount < oldCount) {
			originalInventory.forEach(stack -> differenceList.add(stack.copy()));
			for(int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.get(i);
				if(ItemStack.matches(originalInventory.get(i), stack)) {
					differenceList.set(i, ItemStack.EMPTY);
				}
			}
			differenceList.removeIf(stack -> stack.isEmpty());
			return differenceList;
		}
		if(newCount > oldCount) {
			inventory.forEach(stack -> differenceList.add(stack.copy()));
			for(int i = 0; i < originalInventory.size(); i++) {
				ItemStack stack = originalInventory.get(i);
				if(ItemStack.matches(inventory.get(i), stack)) {
					differenceList.set(i, ItemStack.EMPTY);
				}
			}
			differenceList.removeIf(stack -> stack.isEmpty());
			return differenceList;
		}

		return differenceList;
	}
}
