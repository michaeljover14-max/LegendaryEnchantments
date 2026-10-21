package com.smp.legendary;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class LegendaryPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey bookKey, artifactKey;
    private final Map<UUID,Long> totemCooldown = new HashMap<>();
    @Override public void onEnable() {
        saveDefaultConfig(); bookKey = new NamespacedKey(this,"unique_book"); artifactKey = new NamespacedKey(this,"artifact");
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("legendary")).setExecutor(this);
        double border = getConfig().getDouble("world-border",3000);
        if (border > 0) for(World world : getServer().getWorlds()) if(world.getEnvironment()==World.Environment.NORMAL) world.getWorldBorder().setSize(border);
    }
    private String tag(ItemStack item, NamespacedKey key) {
        if(item==null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
    }
    private ItemStack makeBook(String id, int level) {
        Enchantment enchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(id));
        if(enchant==null) return null;
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        var meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta)item.getItemMeta();
        meta.addStoredEnchant(enchant,level,true);
        meta.getPersistentDataContainer().set(bookKey,PersistentDataType.STRING,id);
        meta.setDisplayName(ChatColor.LIGHT_PURPLE+"Libro unico: "+id);
        item.setItemMeta(meta); return item;
    }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length==0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("/legendary list | /legendary givebook <id> [jugador] | /legendary status"); return true;
        }
        if(args[0].equalsIgnoreCase("list")) {
            ConfigurationSection books=getConfig().getConfigurationSection("books");
            if(books!=null) for(String id:books.getKeys(false)) sender.sendMessage(id+" - "+getConfig().getString("books."+id+".rarity")+" - max "+getConfig().getInt("books."+id+".max"));
            return true;
        }
        if(!sender.hasPermission("legendary.admin")) { sender.sendMessage(ChatColor.RED+"Sin permiso");return true; }
        if(args[0].equalsIgnoreCase("status")) { sender.sendMessage("Libros emitidos: "+getConfig().getConfigurationSection("issued")); return true; }
        if(args[0].equalsIgnoreCase("givebook") && args.length>=2) {
            String id=args[1].toLowerCase(Locale.ROOT);
            if(!getConfig().contains("books."+id)) {sender.sendMessage("Libro no configurado");return true;}
            Player target=args.length>=3?Bukkit.getPlayerExact(args[2]):(sender instanceof Player p?p:null);
            if(target==null) {sender.sendMessage("Jugador no encontrado");return true;}
            int issued=getConfig().getInt("issued."+id,0), max=getConfig().getInt("books."+id+".max",1);
            if(issued>=max) {sender.sendMessage("Limite mundial alcanzado para "+id);return true;}
            ItemStack item=makeBook(id,getConfig().getInt("books."+id+".level",1));
            if(item==null) {sender.sendMessage("Encantamiento vanilla desconocido");return true;}
            if(!target.getInventory().addItem(item).isEmpty()) {sender.sendMessage("Inventario lleno");return true;}
            getConfig().set("issued."+id,issued+1);saveConfig();
            sender.sendMessage("Libro entregado a "+target.getName());return true;
        }
        return false;
    }
    @EventHandler(ignoreCancelled=true) public void onAnvil(PrepareAnvilEvent event) {
        if(!getConfig().getBoolean("apply-with-anvil",true)) return;
        ItemStack base=event.getInventory().getItem(0), book=event.getInventory().getItem(1);
        String id=tag(book,bookKey);
        if(base==null || book==null || id==null || base.getType().isAir() || base.getAmount()!=1 || book.getAmount()!=1) return;
        if(tag(base,artifactKey)!=null) return; // una marca única por objeto
        Enchantment enchant=Registry.ENCHANTMENT.get(NamespacedKey.minecraft(id));
        if(enchant==null) return;
        ItemStack output=base.clone(); ItemMeta meta=output.getItemMeta(); if(meta==null) return;
        meta.addEnchant(enchant,getConfig().getInt("books."+id+".level",1),true);
        meta.getPersistentDataContainer().set(artifactKey,PersistentDataType.STRING,id);
        output.setItemMeta(meta); event.setResult(output); event.getInventory().setRepairCost(1);
    }
    @EventHandler(ignoreCancelled=true) public void onTotem(EntityResurrectEvent event) {
        if(!(event.getEntity() instanceof Player player)) return;
        ItemStack main=player.getInventory().getItemInMainHand(), off=player.getInventory().getItemInOffHand();
        boolean offhand=off.getType()==Material.TOTEM_OF_UNDYING && "infinity".equals(tag(off,artifactKey));
        boolean mainhand=main.getType()==Material.TOTEM_OF_UNDYING && "infinity".equals(tag(main,artifactKey));
        if(!offhand && !mainhand) return;
        long now=System.currentTimeMillis(), last=totemCooldown.getOrDefault(player.getUniqueId(),0L);
        if(now-last < getConfig().getLong("infinite-totem-cooldown-seconds",300)*1000L) return;
        ItemStack replacement=(offhand?off:main).clone(); replacement.setAmount(1);
        totemCooldown.put(player.getUniqueId(),now);
        Bukkit.getScheduler().runTask(this,()-> {
            if(offhand && player.getInventory().getItemInOffHand().getType()==Material.AIR) player.getInventory().setItemInOffHand(replacement);
            else if(mainhand && player.getInventory().getItemInMainHand().getType()==Material.AIR) player.getInventory().setItemInMainHand(replacement);
            else player.getInventory().addItem(replacement).values().forEach(i->player.getWorld().dropItemNaturally(player.getLocation(),i));
        });
    }
    @EventHandler public void onDeath(PlayerDeathEvent event) {
        // Mantiene las reglas de muerte vanilla: los objetos caen si keepInventory está desactivado.
    }
}
