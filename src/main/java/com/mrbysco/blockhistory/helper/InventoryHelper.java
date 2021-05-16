package com.mrbysco.blockhistory.helper;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.List;

public class InventoryHelper {
	public static NonNullList<ItemStack> getContainerInventory(Container container) {
		int regularSlots = 0;
		List<Slot> slots = container.inventorySlots;
		for(int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(i);
			if(slot.inventory instanceof PlayerInventory) {
				break;
			} else {
				regularSlots++;
			}
		}
		NonNullList<ItemStack> inventoryList = NonNullList.create();
		List<ItemStack> subList = container.getInventory().subList(0, regularSlots);
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
				if(ItemStack.areItemStacksEqual(originalInventory.get(i), stack)) {
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
				if(ItemStack.areItemStacksEqual(inventory.get(i), stack)) {
					differenceList.set(i, ItemStack.EMPTY);
				}
			}
			differenceList.removeIf(stack -> stack.isEmpty());
			return differenceList;
		}

		return differenceList;
	}
}
