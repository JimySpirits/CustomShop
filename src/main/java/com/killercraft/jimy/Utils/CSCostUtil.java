package com.killercraft.jimy.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;

import static com.killercraft.jimy.CustomShop.*;
import static com.killercraft.jimy.MySQL.CustomShopDatabase.enableMySQL;

public class CSCostUtil {

    private static BukkitScheduler bk = Bukkit.getScheduler();

    public static String giveCost(String name,String costId,int i){
        if(enableMySQL){
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String,Integer> costs = csb.selectPlayerData(name);
                HashMap<String,String> nowCosts = csb.getCosts();
                String msg;
                if(nowCosts.containsKey(costId)) {
                    if (costs.containsKey(costId)) {
                        int a = costs.get(costId)+i;
                        csb.updatePlayerCost(name, costId, a);
                    } else {
                        csb.insertCost(name, costId, i);
                    }
                    msg = langMap.get("AddCost").replace("<cost>", nowCosts.get(costId)).replace("<value>", i + "");
                }else msg = langMap.get("CostLose");
                bk.runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(name);
                    if(player != null) player.sendMessage(msg);
                });
            });
            return null;
        }else {
            if (playerData.containsKey(name)) {
                HashMap<String, Integer> pCosts = playerData.get(name);
                if (costMap.containsKey(costId)) {
                    if (pCosts.containsKey(costId)) {
                        pCosts.put(costId, pCosts.get(costId) + i);
                    } else {
                        pCosts.put(costId, i);
                    }
                    playerData.put(name, pCosts);
                    return langMap.get("AddCost").replace("<cost>", costMap.get(costId)).replace("<value>", pCosts.getOrDefault(costId, 0) + "");
                } else return langMap.get("CostLose");
            } else {
                if (costMap.containsKey(costId)) {
                    HashMap<String, Integer> pCosts = new HashMap<>();
                    pCosts.put(costId, i);
                    playerData.put(name, pCosts);
                    return langMap.get("AddCost").replace("<cost>", costMap.get(costId)).replace("<value>", pCosts.getOrDefault(costId, 0) + "");
                } else return langMap.get("CostLose");
            }
        }
    }

    public static boolean takeCost(String name,String costId,int i){
        if(!enableMySQL) {
            if (playerData.containsKey(name)) {
                HashMap<String, Integer> pCosts = playerData.get(name);
                if (costMap.containsKey(costId)) {
                    if (pCosts.containsKey(costId)) {
                        int left = pCosts.get(costId) - i;
                        if (left >= 0) {
                            pCosts.put(costId, left);
                            playerData.put(name, pCosts);
                            return true;
                        }else return false;
                    } else return false;
                } else return false;
            } else return false;
        }else{
            HashMap<String,Integer> costs = csb.selectPlayerData(name);
            if(costs.containsKey(costId)){
                int left = costs.get(costId);
                if(left >= 0){
                    csb.updatePlayerCost(name,costId,left);
                    return true;
                }else return false;
            } else return false;
        }
    }


    public static String delCost(String name,String costId,int i){
        if(enableMySQL){
            bk.runTaskAsynchronously(plugin, () -> {
                HashMap<String,Integer> costs = csb.selectPlayerData(name);
                HashMap<String,String> nowCosts = csb.getCosts();
                String msg;
                if(nowCosts.containsKey(costId)) {
                    if (costs.containsKey(costId)) {
                        int a = costs.get(costId)-i;
                        if(a < 0) a = 0;
                        csb.updatePlayerCost(name, costId, a);
                    }
                    msg = langMap.get("DelCost").replace("<cost>", nowCosts.get(costId)).replace("<value>", i + "");
                }else msg = langMap.get("CostLose");
                bk.runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(name);
                    if(player != null) player.sendMessage(msg);
                });
            });
            return null;
        }else {
            if (playerData.containsKey(name)) {
                HashMap<String, Integer> pCosts = playerData.get(name);
                if (costMap.containsKey(costId)) {
                    if (pCosts.containsKey(costId)) {
                        int left = pCosts.get(costId) - i;
                        if (left < 0) left = 0;
                        pCosts.put(costId, left);
                        playerData.put(name, pCosts);
                        return langMap.get("DelCost").replace("<cost>", costMap.get(costId)).replace("<value>", pCosts.getOrDefault(costId, 0) + "");
                    } else return langMap.get("DelCost").replace("<cost>", costMap.get(costId)).replace("<value>", 0 + "");
                } else return langMap.get("CostLose");
            } else return langMap.get("DelCost").replace("<cost>", costMap.get(costId)).replace("<value>", 0 + "");
        }
    }

    public static int checkCost(String name,String costId){
        if(enableMySQL) {
            if (playerData.containsKey(name)) {
                HashMap<String, Integer> pCosts = playerData.get(name);
                if (pCosts.containsKey(costId)) {
                    return pCosts.get(costId);
                }
            }
        }else return csb.selectPlayerData(name).getOrDefault(costId,0);
        return 0;
    }
}
