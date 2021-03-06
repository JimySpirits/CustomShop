package com.killercraft.jimy;

import com.killercraft.jimy.Listeners.CSItemUseListener;
import com.killercraft.jimy.Listeners.CSInvListener;
import com.killercraft.jimy.Manager.CSPAPIHooker;
import com.killercraft.jimy.Manager.GuiShop;
import com.killercraft.jimy.MySQL.CustomShopDatabase;
import com.killercraft.jimy.MySQL.CustomShopSQLUpdate;
import com.killercraft.jimy.Runnables.CSInvCooldown;
import com.killercraft.jimy.Runnables.CSRefreshRunnable;
import com.killercraft.jimy.Runnables.CSSaveDataRunnable;
import com.killercraft.jimy.Utils.CSUtil;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;

import static com.killercraft.jimy.ConfigManager.CSConfig.update;
import static com.killercraft.jimy.ConfigManager.CSDataUtil.*;
import static com.killercraft.jimy.CustomShopAPI.*;
import static com.killercraft.jimy.MySQL.CustomShopDatabase.enableMySQL;
import static com.killercraft.jimy.Utils.CSCostUtil.delCost;
import static com.killercraft.jimy.Utils.CSCostUtil.giveCost;
import static com.killercraft.jimy.Utils.CSUtil.*;

public final class CustomShop extends JavaPlugin {

    public static String root;

    public static CustomShopDatabase csb;

    public static Economy economy;
    public static PlayerPointsAPI poi;
    public static boolean poiLoad;
    public static Plugin plugin;

    public static HashSet<String> cancelSet = new HashSet<>();
    public static HashMap<Player,Integer> invClickCooldownMap = new HashMap<>();

    public static HashMap<String,String> langMap = new HashMap<>();
    public static HashMap<String,String> costMap = new HashMap<>();
    public static HashMap<String,HashMap<String,Integer>> playerData = new HashMap<>();
    public static HashMap<String,HashMap<String,Integer>> hookPlayerData = new HashMap<>();
    public static HashMap<String,GuiShop> customShops = new HashMap<>();
    public static HashMap<String,Integer> refreshShops = new HashMap<>();

    private static HashSet<String> safetyLock = new HashSet<>();
    private static HashSet<String> safetyLock2 = new HashSet<>();

    public static int day;


    @Override
    public void onEnable() {
        plugin = Bukkit.getPluginManager().getPlugin("CustomShop");
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if(papi != null){
            boolean isLoadPAPI = new CSPAPIHooker().register();
            if(isLoadPAPI){
                System.out.println("[CustomShop]Placeholder API Loaded!");
            }else{
                System.out.println("[CustomShop]Placeholder API Unloaded!");
            }
        }
        root = getDataFolder().getAbsolutePath();
        setupEconomy();
        PlayerPoints points = (PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if(points != null){
            poi = points.getAPI();
            poiLoad = true;
            System.out.println("[CustomShop]Player Points Loaded!");
        }else{
            poiLoad = false;
            System.out.println("[CustomShop]Player Points Unloaded!");
        }
        saveDefaultConfig();
        update();
        Bukkit.getScheduler().runTaskTimer(this,new CSRefreshRunnable(),1200,1200);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,new CSSaveDataRunnable(),6000,6000);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,new CustomShopSQLUpdate(),30,30);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,new CSInvCooldown(),2,2);
        Bukkit.getPluginManager().registerEvents(new CSInvListener(),this);
        Bukkit.getPluginManager().registerEvents(new CSItemUseListener(),this);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equalsIgnoreCase("cshop")){
            if(sender instanceof Player){
                Player player = (Player) sender;
                if(args.length < 1){
                    if(player.isOp()){
                        player.sendMessage(ChatColor.GREEN+"=============CustomShop=============");
                        player.sendMessage(ChatColor.BLUE+"/cshop reload "+ChatColor.GRAY+"- 重载插件配置");
                        player.sendMessage(ChatColor.BLUE+"/cshop list "+ChatColor.GRAY+"- 查看当前所有的商店");
                        player.sendMessage(ChatColor.BLUE+"/cshop open 商店名"+ChatColor.GRAY+"- 打开指定商店");
                        player.sendMessage(ChatColor.BLUE+"/cshop open 商店名 玩家名"+ChatColor.GRAY+"- 为指定玩家打开指定商店");
                        player.sendMessage(ChatColor.BLUE+"/cshop edit 商店名"+ChatColor.GRAY+"- 编辑指定商店");
                        player.sendMessage(ChatColor.BLUE+"/cshop refresh 商店名"+ChatColor.GRAY+"- 刷新该商店");
                        player.sendMessage(ChatColor.BLUE+"/cshop create 商店名 行数"+ChatColor.GRAY+"- 创建一个界面有指定行数的商店，注意商店名过长会报错");
                        player.sendMessage(ChatColor.BLUE+"/cshop delete 商店名"+ChatColor.GRAY+"- 删除指定商店");
                        player.sendMessage(ChatColor.BLUE+"/cshop costs"+ChatColor.GRAY+"- 查看自己已拥有的所有货币");
                        player.sendMessage(ChatColor.BLUE+"/cshop givecost 玩家名 货币id 数量"+ChatColor.GRAY+"- 为指定玩家添加指定货币id的货币");
                        player.sendMessage(ChatColor.BLUE+"/cshop takecost 玩家名 货币id 数量"+ChatColor.GRAY+"- 为指定玩家减少指定货币id的货币");
                        player.sendMessage(ChatColor.BLUE+"/cshop cost list"+ChatColor.GRAY+"- 查看现有的货币id与对应的货币名");
                        player.sendMessage(ChatColor.BLUE+"/cshop cost create 货币id 货币名"+ChatColor.GRAY+"- 创建一种新的货币");
                        player.sendMessage(ChatColor.BLUE+"/cshop cost rename 货币id 货币名"+ChatColor.GRAY+"- 修改一种货币的名字");
                        player.sendMessage(ChatColor.BLUE+"/cshop cost delete 货币id true/false[是否清空余额]"+ChatColor.GRAY+"- 删除一种已存在的货币");
                        player.sendMessage(ChatColor.BLUE+"/cshop cost clear 货币id"+ChatColor.GRAY+"- 清空所有的玩家数据中的指定货币余额");
                        player.sendMessage(ChatColor.GREEN+"=============CustomShop=============");
                        player.sendMessage(ChatColor.RED+"作者QQ:2506678176 - 性能与便利至上[此行消息与管理员命令仅OP可见]");
                    }else{
                        player.sendMessage(ChatColor.GREEN+"=============CustomShop=============");
                        player.sendMessage(ChatColor.BLUE+"/cshop open 商店名"+ChatColor.GRAY+"- 打开指定商店");
                        player.sendMessage(ChatColor.BLUE+"/cshop costs"+ChatColor.GRAY+"- 查看自己已拥有的所有货币");
                        player.sendMessage(ChatColor.GREEN+"=============CustomShop=============");
                    }
                }
                if(args.length == 1 && args[0].equalsIgnoreCase("reload")){
                    if(!player.isOp()) return true;
                    CSUtil.reloadConfig(player);
                }else if(args.length == 1 && args[0].equalsIgnoreCase("list")){
                    if(!player.isOp()) return true;
                    sendList(player);
                }else if(args.length == 2 && args[0].equalsIgnoreCase("open")){
                    if(player.hasPermission("customshop.open."+args[1])) {
                        openShop(player,args[1]);
                    }else player.sendMessage(langMap.get("NoPermisson").replace("<perm>","customshop.open."+args[1]));
                }else if(args.length == 3 && args[0].equalsIgnoreCase("open")){
                    if(!player.isOp()) return true;
                    Player players = Bukkit.getPlayer(args[2]);
                    if(players == null) return true;
                    openShop(players,args[1]);
                }else if(args.length == 2 && args[0].equalsIgnoreCase("edit")){
                    if(!player.isOp()) return true;
                    openEditInv(player,args[1]);
                }else if(args.length == 2 && args[0].equalsIgnoreCase("refresh")){
                    if(!player.isOp()) return true;
                    refreshShop(player,args[1]);
                }else if(args.length == 3 && args[0].equalsIgnoreCase("create")){
                    if(!player.isOp()) return true;
                    createShop(player,args);
                }else if(args.length == 2 && args[0].equalsIgnoreCase("delete")){
                    if(!player.isOp()) return true;
                    deleteShop(player,args[1]);
                }else if(args.length == 1 && args[0].equalsIgnoreCase("costs")){
                    sendCosts(player);
                }else if(args.length >= 2 && args[0].equalsIgnoreCase("cost")){
                    if(player.isOp()) {
                        if(args[1].equalsIgnoreCase("create") && args.length == 4){
                            if(createCost(args[2],args[3])){
                                player.sendMessage(langMap.get("CostCreate"));
                            }else player.sendMessage(langMap.get("CostCreateNull"));
                        }else if(args[1].equalsIgnoreCase("delete") && args.length == 4){
                            if(deleteCost(args[2],Boolean.getBoolean(args[3]))){
                                player.sendMessage(langMap.get("CostDelete"));
                            }else player.sendMessage(langMap.get("CostDeleteNull"));
                        }else if(args[1].equalsIgnoreCase("clear") && args.length == 3){
                            clearCost(args[2]);
                            player.sendMessage(langMap.get("CostClear"));
                        }else if(args[1].equalsIgnoreCase("rename") && args.length == 4){
                            if(renameCost(args[2],args[3])){
                                player.sendMessage(langMap.get("CostRename"));
                            }else player.sendMessage(langMap.get("CostRenameNull"));
                        }else if(args[1].equalsIgnoreCase("list")){
                            sendCostList(player);
                        }
                    }
                }else if(args.length == 4 && args[0].equalsIgnoreCase("givecost")){
                    if(!player.isOp()) return true;
                    String msg = giveCost(args[1],args[2],Integer.parseInt(args[3]));
                    if(msg == null) return true;
                    player.sendMessage(msg);
                }else if(args.length == 4 && args[0].equalsIgnoreCase("takecost")){
                    if(!player.isOp()) return true;
                    String msg = delCost(args[1],args[2],Integer.parseInt(args[3]));
                    if(msg == null) return true;
                    player.sendMessage(msg);
                }else if(args.length == 2 && args[0].equalsIgnoreCase("upload")){
                    if(!player.isOp()) return true;
                    String pName = player.getName();
                    if(args[1].equalsIgnoreCase("shops")) {
                        if (!safetyLock.contains(pName)) {
                            player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
                            player.sendMessage(ChatColor.RED + "警告:该功能的作用是将你该服务端的商店与货币配置");
                            player.sendMessage(ChatColor.RED + "(仅商店与货币配置)上传至数据库中并删除原有数据");
                            player.sendMessage(ChatColor.BLUE + "该功能只是提供给从单端转为数据库的用户们使用的！");
                            player.sendMessage(ChatColor.RED + "如果服务端无配置任何商店以及货币那请不要使用该功能");
                            player.sendMessage(ChatColor.RED + "否则可能会导致数据库内商店及货币被清空或者被修改！");
                            player.sendMessage(ChatColor.GREEN + "如果确定开启数据库启用选项之前服务端有商店及货币配置");
                            player.sendMessage(ChatColor.GREEN + "并且这些配置是您想要上传至数据库的商店及货币配置的话");
                            player.sendMessage(ChatColor.GREEN + "那么请您重新输入" + ChatColor.AQUA + " /cshop upload shops" + ChatColor.GREEN + "来执行上传");
                            player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
                            safetyLock.add(pName);
                        } else {
                            safetyLock.remove(pName);
                            if (enableMySQL) {
                                upLoadShopCostData();
                                player.sendMessage(ChatColor.GREEN + "上传完毕！");
                            } else player.sendMessage(ChatColor.RED + "您并没有开启数据库启用选项！");
                        }
                    }else if(args[1].equalsIgnoreCase("playerdata")){
                        if (!safetyLock2.contains(pName)) {
                            player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
                            player.sendMessage(ChatColor.RED + "警告:该功能的作用是将你该服务端的玩家的货币配置");
                            player.sendMessage(ChatColor.RED + "(仅玩家的货币配置)上传至数据库中并删除原有数据");
                            player.sendMessage(ChatColor.BLUE + "该功能只是提供给从单端转为数据库的用户们使用的！");
                            player.sendMessage(ChatColor.RED + "如果服务端无配置任何玩家货币数据那请不要使用该功能");
                            player.sendMessage(ChatColor.RED + "否则可能会导致数据库内玩家的货币被清空或者被修改！");
                            player.sendMessage(ChatColor.GREEN + "如果确定开启数据库启用选项之前服务端有玩家的货币配置");
                            player.sendMessage(ChatColor.GREEN + "并且这些配置是您想要上传至数据库的玩家的货币配置的话");
                            player.sendMessage(ChatColor.RED + "注意:在上传玩家数据之前请先上传商店与货币数据,否则可能导致部分货币不会显示[但实际存在]");
                            player.sendMessage(ChatColor.GREEN + "那么请您重新输入" + ChatColor.AQUA + " /cshop upload playerdata" + ChatColor.GREEN + "来执行上传");
                            player.sendMessage(ChatColor.GREEN + "=============CustomShop=============");
                            safetyLock2.add(pName);
                        } else {
                            safetyLock2.remove(pName);
                            if (enableMySQL) {
                                upLoadPlayerData();
                                player.sendMessage(ChatColor.GREEN + "上传完毕！");
                            } else player.sendMessage(ChatColor.RED + "您并没有开启数据库启用选项！");
                        }
                    }
                }
            }else{
                if(args.length == 3 && args[0].equalsIgnoreCase("open")){
                    Player player = Bukkit.getPlayer(args[2]);
                    if(player == null) return true;
                    openShop(player,args[1]);
                }else if(args.length == 4 && args[0].equalsIgnoreCase("givecost")){
                    String msg = giveCost(args[1],args[2],Integer.parseInt(args[3]));
                    if(msg == null) return true;
                    sender.sendMessage(msg);
                }else if(args.length == 4 && args[0].equalsIgnoreCase("takecost")){
                    String msg = delCost(args[1],args[2],Integer.parseInt(args[3]));
                    if(msg == null) return true;
                    sender.sendMessage(msg);
                }
            }
        }
        return true;
    }

    @Override
    public void onDisable() {
        saveData();
        saveShops();
        saveRefresh();
    }
    private void setupEconomy(){
        RegisteredServiceProvider economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = ((Economy)economyProvider.getProvider());
        }
    }
}
