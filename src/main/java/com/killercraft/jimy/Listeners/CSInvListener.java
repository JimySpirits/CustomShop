package com.killercraft.jimy.Listeners;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import static com.killercraft.jimy.CustomShop.*;
import static com.killercraft.jimy.Utils.CSUtil.clickShop;
import static com.killercraft.jimy.Utils.CSUtil.closeShop;

public class CSInvListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event){
        Inventory inv = event.getInventory();
        String title = inv.getTitle();
        if(title != null && title.startsWith("[E]")){
            HumanEntity he = event.getWhoClicked();
            if(he instanceof Player) {
                Player player = (Player) he;
                ClickType ct = event.getClick();
                if (ct == ClickType.SHIFT_RIGHT || ct == ClickType.NUMBER_KEY || ct == ClickType.SHIFT_LEFT) {
                    event.setCancelled(true);
                    player.sendMessage(langMap.get("NoShift"));
                    return;
                }
                if(event.getRawSlot() >= inv.getSize()) return;
                ItemStack stack = event.getCursor();
                if(stack == null || stack.getType() == Material.AIR) return;
                if(checkShopItem(stack)){
                    event.setCancelled(true);
                    player.sendMessage(langMap.get("NoShopItem"));
                }
            }
        }else if(title != null && cancelSet.contains(title)){
            HumanEntity he = event.getWhoClicked();
            if(he instanceof Player) {
                Player player = (Player) he;
                event.setCancelled(true);
                if(invClickCooldownMap.containsKey(player)) {
                    player.sendMessage(langMap.get("NoClick"));
                    return;
                }else{
                    invClickCooldownMap.put(player,3);
                }
                int slot = event.getRawSlot();
                ItemStack checkS = event.getCurrentItem();
                if(checkS == null || !checkS.hasItemMeta()) return;
                clickShop(title,player,slot);
            }
        }
    }


    @EventHandler
    public void onClose(InventoryCloseEvent event){
        Inventory inv = event.getInventory();
        String title = inv.getTitle();
        if(title != null && title.startsWith("[E]")){
            String shopName = title.replace("[E]","");
            closeShop(shopName,inv);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event){
        Inventory inv = event.getInventory();
        String title = inv.getTitle();
        if(title != null && title.startsWith("[E]")){
            HumanEntity he = event.getWhoClicked();
            if(he instanceof Player) {
                Player player = (Player) he;
                ItemStack stack = event.getOldCursor();
                if(stack == null || stack.getType() == Material.AIR) return;
                for(int i:event.getRawSlots()){
                    if(i < inv.getSize()) {
                        if(checkShopItem(stack)){
                            event.setCancelled(true);
                            player.sendMessage(langMap.get("NoShopItem"));
                        }
                    }
                }
            }
        }else if(title != null && cancelSet.contains(title)){
            event.setCancelled(true);
        }
    }

    private boolean checkShopItem(ItemStack stack){
        if(stack != null && stack.hasItemMeta()){
            ItemMeta meta = stack.getItemMeta();
            if(meta.hasLore()){
                for(String lore:meta.getLore()){
                    if(lore.startsWith("b")){
                        if(lore.contains("~")){
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
