package com.killercraft.jimy.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.killercraft.jimy.CustomShop.costMap;
import static com.killercraft.jimy.CustomShop.langMap;
import static com.killercraft.jimy.Utils.CSCostUtil.giveCost;


public class CSItemUseListener implements Listener {
    @EventHandler
    public void onUse(PlayerInteractEvent event){
        Action ac = event.getAction();
        if(ac == Action.RIGHT_CLICK_AIR || ac == Action.RIGHT_CLICK_BLOCK){
            Player player = event.getPlayer();
            ItemStack stack = player.getItemInHand();
            if(stack != null && stack.hasItemMeta()){
                ItemMeta meta = stack.getItemMeta();
                if(meta.hasLore()){
                    String costLore = langMap.get("CostLore");
                    for(String lore:meta.getLore()){
                        if(lore.contains(costLore)){
                            lore = lore.replace(costLore,"");
                            String[] xSplit = lore.split("x");
                            int num = Integer.parseInt(ChatColor.stripColor(xSplit[1]));
                            String costId = null;
                            for(String code:costMap.keySet()){
                                if(lore.contains(costMap.get(code))) costId = code;
                                //if(costMap.get(code).equals(xSplit[0])) costId = code;
                            }
                            if(costId != null){
                                player.sendMessage(giveCost(player.getName(),costId,num));
                            }
                            int amout = stack.getAmount();
                            if(amout > 1){
                                stack.setAmount(amout-1);
                                player.setItemInHand(stack);
                            }else{
                                player.setItemInHand(null);
                            }
                        }
                    }
                }
            }
        }
    }
}
