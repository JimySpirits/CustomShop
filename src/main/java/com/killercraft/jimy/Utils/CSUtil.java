package com.killercraft.jimy.Utils;

import com.killercraft.jimy.CustomShop;
import com.killercraft.jimy.Manager.GuiShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.killercraft.jimy.ConfigManager.CSConfig.update;
import static com.killercraft.jimy.ConfigManager.CSDataUtil.*;
import static com.killercraft.jimy.CustomShop.*;
import static com.killercraft.jimy.CustomShopAPI.deleteCost;
import static com.killercraft.jimy.MySQL.CustomShopDatabase.enableMySQL;
import static com.killercraft.jimy.Utils.CSItemSaveUtil.getItemStack;
import static com.killercraft.jimy.Utils.CSItemUtil.*;
import static com.killercraft.jimy.Utils.CSItemUtil.takeIdNeed;

public class CSUtil {

    private static BukkitScheduler bk = Bukkit.getScheduler();

    public static HashMap<String, GuiShop> getShops() {
        if (enableMySQL) {
            return csb.getShops();
        } else {
            return customShops;
        }
    }

    public static void sendList(Player player) {
        if (!enableMySQL) {
            if (!player.isOp()) return;
            player.sendMessage(langMap.get("ShopList"));
            String title = ChatColor.WHITE + ">> ";
            for (String name : customShops.keySet()) {
                player.sendMessage(title + name);
            }
        } else {
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String, GuiShop> cshops = getShops();
                bk.runTask(plugin, () -> {
                    player.sendMessage(langMap.get("ShopList"));
                    String title = ChatColor.WHITE + ">> ";
                    for (String name : cshops.keySet()) {
                        player.sendMessage(title + name);
                    }
                });
            });
        }
    }

    public static void sendCostList(Player player) {
        String costTitle = langMap.get("CostTitle");
        String costName = langMap.get("CostName");
        if (!enableMySQL) {
            player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
            for(String costId:costMap.keySet()){
                player.sendMessage(costTitle+costId+" "+ costName+costMap.get(costId));
            }
            player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
        } else {
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String, String> costs = csb.getCosts();
                bk.runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
                    for(String costId:costs.keySet()){
                        player.sendMessage(costTitle+costId+" "+costName+costs.get(costId));
                    }
                    player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
                });
            });
        }
    }

    public static void openShop(Player player, String shopName) {
        if (!enableMySQL) {
            if (player.hasPermission("customshop.open." + shopName)) {
                shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
                if (customShops.containsKey(shopName)) {
                    GuiShop gs = customShops.get(shopName);
                    boolean refresh = false;
                    if (refreshShops.containsKey(shopName)) {
                        int rday = refreshShops.get(shopName);
                        if (day != rday) {
                            refreshShops.put(shopName, day);
                            refresh = true;
                        }
                    } else {
                        refreshShops.put(shopName, day);
                        refresh = true;
                    }
                    if(refresh) {
                        gs.refresh();
                    }
                    gs.refreshItems();
                    gs.openShop(player);
                } else {
                    player.sendMessage(langMap.get("NoShop"));
                }
            } else player.sendMessage(langMap.get("NoPermisson").replace("<perm>", "customshop.open." + shopName));
        } else {
            shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
            String finalShopName = shopName;
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String, GuiShop> cshops = getShops();
                boolean refresh = false;
                int shopDay = csb.selectRefresh(finalShopName);
                if(day != shopDay) {
                    refresh = true;
                    if(shopDay == -1) csb.deleteRefresh(finalShopName);
                    csb.insertRefresh(finalShopName,day);
                }
                boolean finalRefresh = refresh;
                bk.runTask(plugin, () -> {
                    if (cshops.containsKey(finalShopName)) {
                        GuiShop gs = cshops.get(finalShopName);
                        if(finalRefresh) {
                            gs.refresh();
                        }
                        gs.refreshItems();
                        gs.openShop(player);
                    } else {
                        player.sendMessage(langMap.get("NoShop"));
                    }
                });
            });
        }
    }

    public static void openEditInv(Player player, String shopName) {
        if (!enableMySQL) {
            if (!player.isOp()) return;
            shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
            if (customShops.containsKey(shopName)) {
                GuiShop gs = customShops.get(shopName);
                gs.openEditInv(player);
            } else {
                player.sendMessage(langMap.get("NoShop"));
            }
        } else {
            shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
            String finalShopName = shopName;
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String, GuiShop> cshops = getShops();
                bk.runTask(plugin, () -> {
                    if (cshops.containsKey(finalShopName)) {
                        GuiShop gs = cshops.get(finalShopName);
                        gs.openEditInv(player);
                    } else {
                        player.sendMessage(langMap.get("NoShop"));
                    }
                });
            });
        }
    }

    public static void refreshShop(Player player, String shopName) {
        if (!enableMySQL) {
            if (!player.isOp()) return;
            shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
            if (customShops.containsKey(shopName)) {
                GuiShop gs = customShops.get(shopName);
                boolean refresh = false;
                if (refreshShops.containsKey(shopName)) {
                    int rday = refreshShops.get(shopName);
                    if (day != rday) {
                        refreshShops.put(shopName, day);
                        refresh = true;
                    }
                } else {
                    refreshShops.put(shopName, day);
                    refresh = true;
                }
                if(refresh) {
                    gs.refresh();
                }
                player.sendMessage(langMap.get("ShopRefresh"));
            } else {
                player.sendMessage(langMap.get("NoShop"));
            }
        } else {
            shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
            String finalShopName = shopName;
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String, GuiShop> cshops = getShops();
                boolean refresh = false;
                int shopDay = csb.selectRefresh(finalShopName);
                if(day != shopDay) {
                    refresh = true;
                    if(shopDay == -1) csb.deleteRefresh(finalShopName);
                    csb.insertRefresh(finalShopName,day);
                }
                boolean finalRefresh = refresh;
                bk.runTask(plugin, () -> {
                    if (cshops.containsKey(finalShopName)) {
                        GuiShop gs = cshops.get(finalShopName);
                        if(finalRefresh) {
                            gs.refresh();
                        }
                        player.sendMessage(langMap.get("ShopRefresh"));
                    } else {
                        player.sendMessage(langMap.get("NoShop"));
                    }
                });
            });
        }
    }

    public static void createShop(Player player, String[] args) {
        if (!enableMySQL) {
            if (!player.isOp()) return;
            args[1] = args[1].replace('&', ChatColor.COLOR_CHAR);
            int line = Integer.parseInt(args[2]);
            if (line >= 1 && line <= 6) {
                if (!customShops.containsKey(args[1])) {
                    GuiShop gs = new GuiShop(line, args[1]);
                    customShops.put(args[1], gs);
                    player.sendMessage(langMap.get("ShopCreate"));
                } else player.sendMessage(langMap.get("AlreadyCreate"));
            } else player.sendMessage(langMap.get("LineNull"));
        } else {
            args[1] = args[1].replace('&', ChatColor.COLOR_CHAR);
            int line = Integer.parseInt(args[2]);
            bk.runTaskAsynchronously(plugin, () -> {
                String msg;
                HashMap<String, GuiShop> cshops = getShops();
                if (line >= 1 && line <= 6) {
                    if (!cshops.containsKey(args[1])) {
                        GuiShop gs = new GuiShop(line, args[1]);
                        csb.insertShop(gs);
                        msg = langMap.get("ShopCreate");
                    } else msg = langMap.get("AlreadyCreate");
                } else msg = langMap.get("LineNull");
                bk.runTask(plugin, () -> {
                    player.sendMessage(msg);
                });
            });
        }
    }

    public static void deleteShop(Player player, String shopName) {
        if (!enableMySQL) {
            if (!player.isOp()) return;
            shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
            if (customShops.containsKey(shopName)) {
                GuiShop gs = customShops.get(shopName);
                gs.closeAllInv(langMap.get("DeleteClose"));
                customShops.remove(shopName);
                refreshShops.remove(shopName);
                cancelSet.remove(shopName);
                player.sendMessage(langMap.get("ShopDelete"));
            } else player.sendMessage(langMap.get("NoShop"));
        } else {
            shopName = shopName.replace('&', ChatColor.COLOR_CHAR);
            String finalShopName = shopName;
            bk.runTaskAsynchronously(plugin, () -> {
                String msg;
                HashMap<String, GuiShop> cshops = getShops();
                if (cshops.containsKey(finalShopName)) {
                    csb.deleteShopData(finalShopName);
                    csb.deleteRefresh(finalShopName);
                    cancelSet.remove(finalShopName);
                    msg = langMap.get("ShopDelete");
                } else msg = langMap.get("NoShop");
                bk.runTask(plugin, () -> {
                    if (msg != null) player.sendMessage(msg);
                    closeAllPlayerCustomShop(finalShopName, "delete");
                });
            });
        }
    }

    public static void sendCosts(Player player) {
        player.sendMessage(langMap.get("LeftCost"));
        if (!enableMySQL) {
            String pName = player.getName();
            if (playerData.containsKey(pName)) {
                HashMap<String, Integer> pCosts = playerData.get(pName);
                for (String id : pCosts.keySet()) {
                    if (costMap.containsKey(id)) {
                        player.sendMessage(costMap.get(id) + ChatColor.GOLD + ">>: " + ChatColor.BLUE + pCosts.get(id));
                    }
                }
            } else {
                player.sendMessage(langMap.get("NoCost"));
            }
        } else {
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String, Integer> costs = csb.selectPlayerData(player.getName());
                HashMap<String, String> nowCosts = csb.getCosts();
                bk.runTask(plugin, () -> {
                    boolean send = false;
                    for (String id : costs.keySet()) {
                        if (nowCosts.containsKey(id)) {
                            send = true;
                            player.sendMessage(nowCosts.get(id) + ChatColor.GOLD + ">>: " + ChatColor.BLUE + costs.get(id));
                        }
                    }
                    if(!send) player.sendMessage(langMap.get("NoCost"));
                });
            });
        }
    }



    public static void reloadConfig(Player player) {
        if (!enableMySQL) {
            if (!player.isOp()) return;
            for (GuiShop gs : customShops.values()) {
                gs.closeAllInv(langMap.get("ReloadClose"));
            }
            saveData();
            saveShops();
            saveRefresh();
            update();
            player.sendMessage("[CustomShop]Reload Config!");
        } else {
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String, GuiShop> cshops = getShops();
                bk.runTask(plugin, () -> {
                    for (String shopName : cshops.keySet()) {
                        closeAllPlayerCustomShop(shopName, "reload");
                    }
                    saveShops();
                    saveRefresh();
                    update();
                    player.sendMessage("[CustomShop]Reload Config!");
                });
            });
        }
    }

    public static void upLoadShopCostData(){
        if(customShops.size() >= 1){
            csb.deleteAllShop();
            csb.deleteAllRefresh();
            for(GuiShop gs:customShops.values()){
                csb.insertShop(gs);
            }
        }
        File file = new File(CustomShop.root, "config.yml");
        FileConfiguration config = load(file);
        ConfigurationSection costs = config.getConfigurationSection("Costs");
        HashMap<String,String> thisCosts = new HashMap<>();
        for(String temp:costs.getKeys(false)){
            thisCosts.put(temp,costs.getString(temp).replace('&', ChatColor.COLOR_CHAR));
        }
        HashMap<String,String> sqlCosts = csb.getCosts();
        for(String costId:thisCosts.keySet()) {
            if(sqlCosts.containsKey(costId)) deleteCost(costId,false);
            csb.insertNewCost(costId,thisCosts.get(costId));
        }
    }

    public static void upLoadPlayerData(){
        File file = new File(CustomShop.root, "playerdata.yml");
        FileConfiguration data = load(file);
        HashMap<String,HashMap<String,Integer>> playerCostsMap = new HashMap<>();
        for(String temp:data.getKeys(false)){
            HashMap<String,Integer> playerCost = new HashMap<>();
            ConfigurationSection sec = data.getConfigurationSection(temp);
            for(String temp2:sec.getKeys(false)){
                playerCost.put(temp2, sec.getInt(temp2));
            }
            playerCostsMap.put(temp,playerCost);
        }
        if(playerCostsMap.size() >= 1){
            csb.deleteAllPlayerData();
            for(String name:playerCostsMap.keySet()){
                HashMap<String,Integer> nowCosts = playerCostsMap.get(name);
                for(String costId:nowCosts.keySet()){
                    csb.insertCost(name,costId,nowCosts.get(costId));
                }
            }
        }
    }


    public static void closeAllPlayerCustomShop(String shopName, String type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView invv = player.getOpenInventory();
            if (invv == null) continue;
            Inventory inv = invv.getTopInventory();
            if (inv == null) continue;
            String title = inv.getTitle();
            if (null == title) continue;
            if (title.contains(shopName)) {
                player.closeInventory();
                if (type.equalsIgnoreCase("delete")) {
                    player.sendMessage(langMap.get("ShopDelete"));
                } else if (type.equalsIgnoreCase("reload")) {
                    player.sendMessage(langMap.get("ReloadClose"));
                }
            }
        }
    }

    public static void closeShop(String shopName,Inventory inv){
        if(!enableMySQL) {
            if (customShops.containsKey(shopName)) {
                GuiShop gs = customShops.get(shopName);
                HashMap<Integer, ItemStack> items = new HashMap<>();
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if(stack != null && stack.hasItemMeta()) {
                        items.put(i, inv.getItem(i));
                    }
                }
                gs.setItems(items);
                customShops.put(shopName, gs);
            }
        }else{
            bk.runTaskAsynchronously(plugin,()->{
                GuiShop gs = csb.selectShop(shopName);
                if(gs != null){
                    bk.runTask(plugin,()->{
                        HashMap<Integer, ItemStack> items = new HashMap<>();
                        for (int i = 0; i < inv.getSize(); i++) {
                            ItemStack stack = inv.getItem(i);
                            if(stack != null && stack.hasItemMeta()) {
                                items.put(i, inv.getItem(i));
                            }
                        }
                        gs.setItems(items);
                        bk.runTaskAsynchronously(plugin,()-> csb.updateShop(gs));
                    });
                }
            });
        }
    }

    public static void clickShop(String title,Player player,int slot){
        if(!enableMySQL) {
            GuiShop gs = customShops.get(title);
            HashMap<Integer, ItemStack> items = gs.getItems();
            if (items.containsKey(slot)) {
                ItemStack stack = items.get(slot);
                if (stack != null && stack.hasItemMeta()) {
                    ItemMeta oMeta = stack.getItemMeta();
                    List<String> lores = oMeta.getLore();
                    boolean isBuy = false;
                    List<String> takeList = new ArrayList<>();
                    for (String lore : lores) {
                        if (lore.startsWith("b")) {
                            String[] buy = lore.split("~");
                            if (buy[1].equals("eco")) {
                                isBuy = checkEcoNeed(Integer.parseInt(buy[2]),player);
                                takeList.add(lore);
                            } else if (buy[1].equals("point")) {
                                isBuy = checkPointNeed(Integer.parseInt(buy[2]),player);
                                takeList.add(lore);
                            } else if (buy[1].equals("item")) {
                                isBuy = checkItemNeed(buy[2],Integer.parseInt(buy[3]),player);
                                takeList.add(lore);
                            } else if (buy[1].equals("cost")) {
                                isBuy = checkCostNeed(buy[2],Integer.parseInt(buy[3]),player);
                                takeList.add(lore);
                            } else if (buy[1].equals("id")) {
                                isBuy = checkIdNeed(Integer.parseInt(buy[2]),Integer.parseInt(buy[4]),player);
                                takeList.add(lore);
                            } else if (buy[1].equals("perm")){
                                isBuy = player.hasPermission(buy[2]);
                            } else if (buy[1].equals("none")){
                                isBuy = true;
                            }
                            if(!isBuy){
                                break;
                            }
                        }
                    }
                    if(isBuy){
                        for(String take:takeList){
                            String[] buy = take.split("~");
                            if (buy[1].equals("eco")) {
                                takeEcoNeed(Integer.parseInt(buy[2]),player);
                            } else if (buy[1].equals("point")) {
                                takePointNeed(Integer.parseInt(buy[2]),player);
                            } else if (buy[1].equals("item")) {
                                takeItemNeed(buy[2],Integer.parseInt(buy[3]),player);
                            } else if (buy[1].equals("cost")) {
                                takeCostNeed(buy[2],Integer.parseInt(buy[3]),player);
                            } else if (buy[1].equals("id")) {
                                takeIdNeed(Integer.parseInt(buy[2]),Integer.parseInt(buy[4]),player);
                            }
                        }
                        ItemStack aStack = stack.clone();
                        ItemMeta meta = aStack.getItemMeta();
                        List<String> aLores = meta.getLore();
                        for(int i = 0;i<aLores.size();i++){
                            String aLore = aLores.get(i);
                            if(aLore.contains("<player>")){
                                aLores.set(i,aLore.replace("<player>",player.getName()));
                            }
                        }
                        aLores.removeIf(aLore -> aLore.startsWith("b~") || aLore.startsWith("m~") || aLore.startsWith("s~"));
                        if(aLores.isEmpty()){
                            meta.setLore(null);
                        }else{
                            meta.setLore(aLores);
                        }
                        aStack.setItemMeta(meta);
                        boolean take = false;
                        HashMap<Integer,String> changeMap = new HashMap<>();
                        int i = 0;
                        for (String lore : lores) {
                            if (lore.startsWith("s")) {
                                String[] setting = lore.split("~");
                                if (setting[1].equals("close")) {
                                    player.closeInventory();
                                } else if (setting[1].equals("open")) {
                                    openShop(player,setting[2]);
                                } else if (setting[1].equals("limit")) {
                                    player.closeInventory();
                                    int limit = Integer.parseInt(setting[2]);
                                    if(limit <= 0) {
                                        player.sendMessage(langMap.get("NoStock"));
                                        take = true;
                                    }else{
                                        limit-=1;
                                        changeMap.put(i,"s~limit~"+limit);
                                    }
                                } else if (setting[1].equals("daylimit")) {
                                    player.closeInventory();
                                    int limit = Integer.parseInt(setting[3]);
                                    if(limit <= 0) {
                                        player.sendMessage(langMap.get("NoStock"));
                                        take = true;
                                    }else{
                                        limit-=1;
                                        changeMap.put(i,"s~daylimit~"+setting[2]+"~"+limit);
                                    }
                                } else if (setting[1].equals("cmd")) {
                                    CommandSender cs = player;
                                    if(setting[2].equals("c")){
                                        cs = Bukkit.getConsoleSender();
                                    }
                                    Bukkit.dispatchCommand(cs,setting[3].replace("<player>",player.getName()));
                                } else if (setting[1].equals("take")){
                                    take = true;
                                }
                            }
                            i++;
                        }
                        if(changeMap.size() >= 1) {
                            for (int ci : changeMap.keySet()) {
                                lores.set(ci,changeMap.get(ci));
                            }
                            oMeta.setLore(lores);
                            stack.setItemMeta(oMeta);
                            items.put(slot,stack);
                            gs.setItems(items);
                        }
                        if(!take) {
                            HashMap<Integer, ItemStack> loseItems = player.getInventory().addItem(aStack);
                            player.sendMessage(langMap.get("BuyOK"));
                            if (loseItems.size() >= 1) {
                                for (ItemStack stack1 : loseItems.values()) {
                                    player.getWorld().dropItem(player.getLocation(), stack1);
                                }
                                player.sendMessage(langMap.get("MaxItem"));
                            }
                        }
                    }else{
                        player.sendMessage(langMap.get("NoNeed"));
                    }
                }
            }
        }else{
            bk.runTaskAsynchronously(plugin,()->{
                GuiShop gs = csb.selectShop(title);
                bk.runTask(plugin,()->{
                    HashMap<Integer, ItemStack> items = gs.getItems();
                    if (items.containsKey(slot)) {
                        ItemStack stack = items.get(slot);
                        if (stack != null && stack.hasItemMeta()) {
                            ItemMeta oMeta = stack.getItemMeta();
                            List<String> lores = oMeta.getLore();
                            boolean isBuy = false;
                            List<String> takeList = new ArrayList<>();
                            for (String lore : lores) {
                                if (lore.startsWith("b")) {
                                    String[] buy = lore.split("~");
                                    if (buy[1].equals("eco")) {
                                        isBuy = checkEcoNeed(Integer.parseInt(buy[2]),player);
                                        takeList.add(lore);
                                    } else if (buy[1].equals("point")) {
                                        isBuy = checkPointNeed(Integer.parseInt(buy[2]),player);
                                        takeList.add(lore);
                                    } else if (buy[1].equals("item")) {
                                        isBuy = checkItemNeed(buy[2],Integer.parseInt(buy[3]),player);
                                        takeList.add(lore);
                                    } else if (buy[1].equals("cost")) {
                                        isBuy = checkCostNeed(buy[2],Integer.parseInt(buy[3]),player);
                                        takeList.add(lore);
                                    } else if (buy[1].equals("id")) {
                                        isBuy = checkIdNeed(Integer.parseInt(buy[2]),Integer.parseInt(buy[4]),player);
                                        takeList.add(lore);
                                    } else if (buy[1].equals("perm")){
                                        isBuy = player.hasPermission(buy[2]);
                                    } else if (buy[1].equals("none")){
                                        isBuy = true;
                                    }
                                    if(!isBuy){
                                        break;
                                    }
                                }
                            }
                            if(isBuy){
                                for(String take:takeList){
                                    String[] buy = take.split("~");
                                    if (buy[1].equals("eco")) {
                                        takeEcoNeed(Integer.parseInt(buy[2]),player);
                                    } else if (buy[1].equals("point")) {
                                        takePointNeed(Integer.parseInt(buy[2]),player);
                                    } else if (buy[1].equals("item")) {
                                        takeItemNeed(buy[2],Integer.parseInt(buy[3]),player);
                                    } else if (buy[1].equals("cost")) {
                                        takeCostNeed(buy[2],Integer.parseInt(buy[3]),player);
                                    } else if (buy[1].equals("id")) {
                                        takeIdNeed(Integer.parseInt(buy[2]),Integer.parseInt(buy[4]),player);
                                    }
                                }
                                ItemStack aStack = stack.clone();
                                ItemMeta meta = aStack.getItemMeta();
                                List<String> aLores = meta.getLore();
                                for(int i = 0;i<aLores.size();i++){
                                    String aLore = aLores.get(i);
                                    if(aLore.contains("<player>")){
                                        aLores.set(i,aLore.replace("<player>",player.getName()));
                                    }
                                }
                                aLores.removeIf(aLore -> aLore.startsWith("b~") || aLore.startsWith("m~") || aLore.startsWith("s~"));
                                if(aLores.isEmpty()){
                                    meta.setLore(null);
                                }else{
                                    meta.setLore(aLores);
                                }
                                aStack.setItemMeta(meta);
                                boolean take = false;
                                HashMap<Integer,String> changeMap = new HashMap<>();
                                int i = 0;
                                for (String lore : lores) {
                                    if (lore.startsWith("s")) {
                                        String[] setting = lore.split("~");
                                        if (setting[1].equals("close")) {
                                            player.closeInventory();
                                        } else if (setting[1].equals("open")) {
                                            openShop(player,setting[2]);
                                        } else if (setting[1].equals("limit")) {
                                            player.closeInventory();
                                            int limit = Integer.parseInt(setting[2]);
                                            if(limit <= 0) {
                                                player.sendMessage(langMap.get("NoStock"));
                                                take = true;
                                            }else{
                                                limit-=1;
                                                changeMap.put(i,"s~limit~"+limit);
                                            }
                                        } else if (setting[1].equals("daylimit")) {
                                            player.closeInventory();
                                            int limit = Integer.parseInt(setting[3]);
                                            if(limit <= 0) {
                                                player.sendMessage(langMap.get("NoStock"));
                                                take = true;
                                            }else{
                                                limit-=1;
                                                changeMap.put(i,"s~daylimit~"+setting[2]+"~"+limit);
                                            }
                                        } else if (setting[1].equals("cmd")) {
                                            CommandSender cs = player;
                                            if(setting[2].equals("c")){
                                                cs = Bukkit.getConsoleSender();
                                            }
                                            Bukkit.dispatchCommand(cs,setting[3].replace("<player>",player.getName()));
                                        } else if (setting[1].equals("take")){
                                            take = true;
                                        }
                                    }
                                    i++;
                                }
                                if(changeMap.size() >= 1) {
                                    for (int ci : changeMap.keySet()) {
                                        lores.set(ci,changeMap.get(ci));
                                    }
                                    oMeta.setLore(lores);
                                    stack.setItemMeta(oMeta);
                                    items.put(slot,stack);
                                    gs.setItems(items);
                                }
                                if(!take) {
                                    HashMap<Integer, ItemStack> loseItems = player.getInventory().addItem(aStack);
                                    player.sendMessage(langMap.get("BuyOK"));
                                    if (loseItems.size() >= 1) {
                                        for (ItemStack stack1 : loseItems.values()) {
                                            player.getWorld().dropItem(player.getLocation(), stack1);
                                        }
                                        player.sendMessage(langMap.get("MaxItem"));
                                    }
                                }
                            }else{
                                player.sendMessage(langMap.get("NoNeed"));
                            }
                        }
                    }
                });
            });
        }

    }
}