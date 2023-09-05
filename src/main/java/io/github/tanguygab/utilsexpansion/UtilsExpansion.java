package io.github.tanguygab.utilsexpansion;


import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import me.clip.placeholderapi.expansion.Taskable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;

public final class UtilsExpansion extends PlaceholderExpansion implements Relational, Taskable, Configurable {

    private final List<String> placeholders = new ArrayList<>();
    private final Map<String,Object> defaults = new HashMap<>();
    private final Map<String,String> shortcuts = new HashMap<>();

    public UtilsExpansion() {
        List<String> placeholders = Arrays.asList("parse","parse:<num>","color","uncolor","uncolor:each","parseother:[name|placeholder]","escape","parserel:[name|placeholder]");
        placeholders.forEach(placeholder->{
            this.placeholders.add("%utils_"+placeholder+"_<placeholder>%");
            this.placeholders.add("%rel_utils_"+placeholder+"_<placeholder>%");
        });
        Map<String,String> defaultShortCuts = new HashMap<>();
        defaultShortCuts.put("othermath","%utils_parseother:[{0}]_math_{1}+1%");
        defaults.put("shortcuts",defaultShortCuts);
    }

    @Override
    public @Nonnull String getIdentifier() {
        return "utils";
    }
    @Override
    public @Nonnull String getAuthor() {
        return "Tanguygab";
    }
    @Override
    public @Nonnull String getVersion() {
        return "1.0.5";
    }
    @Override
    public @Nonnull List<String> getPlaceholders() {
        return placeholders;
    }
    @Override
    public Map<String, Object> getDefaults() {
        return defaults;
    }

    @Override
    public void start() {
        ConfigurationSection shortcuts = getConfigSection("shortcuts");
        assert shortcuts != null;
        shortcuts.getValues(false).forEach((name, shortcut)->{
            this.shortcuts.put(name,shortcut.toString());
            placeholders.add("%utils_shortcut_"+name+":arg0:arg1:...%");
            placeholders.add("%rel_utils_shortcut_"+name+":arg0:arg1:...%");
        });
    }
    @Override
    public void stop() {}

    @Override
    public String onRequest(OfflinePlayer player, @Nonnull String params) {
        return process(params,player,null);
    }
    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String params) {
        return process(params,viewer,target);
    }

    @SuppressWarnings("deprecation")
    private String process(String params, OfflinePlayer viewer, Player target) {
        String arg = params.split("_")[0];
        String text = params.substring(arg.length()+1);
        if (arg.equalsIgnoreCase("escape")) return "%"+text+"%";

        if (arg.equalsIgnoreCase("shortcut")) {
            String[] args = text.split(":");
            String shortcut = args[0];
            if (!shortcuts.containsKey(shortcut)) return "";
            shortcut = shortcuts.get(shortcut);
            for (int i = 1; i < args.length; i++) shortcut = shortcut.replace("{"+(i-1)+"}",args[i]);
            return processParse(shortcut,1,viewer,target,false);
        }

        if (arg.startsWith("parseother:[") && params.contains("]")) {
            String placeholder = params.substring(12,params.indexOf("]"));
            String name = processParse(placeholder, viewer,target).replace("%","");
            return processParse(params.substring(params.indexOf("]")+2), Bukkit.getServer().getOfflinePlayer(name),target);
        }
        if (arg.startsWith("parserel:[") && params.contains("]")) {
            String placeholder = params.substring(10,params.indexOf("]"));
            String name = processParse(placeholder, viewer,target).replace("%","");
            return processParse(params.substring(params.indexOf("]")+2), viewer,Bukkit.getServer().getPlayer(name));
        }
        if (arg.equalsIgnoreCase("parsesync")) {
            if (Bukkit.isPrimaryThread()) return processParse(text, viewer,target);
            try {
                return Bukkit.getServer().getScheduler().callSyncMethod(getPlaceholderAPI(),()->processParse(text, viewer,target)).get();
            } catch (Exception e) {
                return "<Error parsing placeholders synchronously>";
            }
        }
        if (arg.startsWith("parse")) {
            String[] args = arg.split(":");
            int number = 1;
            if (args.length > 1)
                try {number = Integer.parseInt(args[1]);}
                catch (Exception ignored) {}
            boolean percent = args.length < 2 || args[2].equalsIgnoreCase("true");
            return processParse(text,number,viewer,target,percent);
        }
        if (arg.equalsIgnoreCase("color")) return color(processParse(text, viewer,target));
        if (arg.startsWith("uncolor")) return ChatColor.stripColor(color(processParse(text,1,viewer,target,true,arg.equalsIgnoreCase("uncolor:each"))));
        return null;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&',text);
    }


    private String processParse(String text, OfflinePlayer viewer, Player target) {
        return processParse(text, 1,viewer,target,true,false);
    }
    private String processParse(String text, int number, OfflinePlayer viewer, Player target, boolean percent) {
        return processParse(text,number,viewer,target,percent,false);
    }
    private String processParse(String text, int number, OfflinePlayer viewer, Player target, boolean percent, boolean uncolorEach) {
        if (percent) text = "%"+text+"%";

        for (int i = 0; i < number; i++) {
            findBracketPlaceholders(text);
            text = parseBracketPlaceholders(text,viewer,null,uncolorEach);
            text = parsePlaceholders(text,viewer,target);
        }
        return text;
    }

    private String parsePlaceholders(String text, OfflinePlayer viewer, Player target) {
        if (target == null) return PlaceholderAPI.setPlaceholders(viewer,text);
        return PlaceholderAPI.setRelationalPlaceholders(viewer.getPlayer(),target,text);
    }

    private final Map<String,Map<Integer,Integer>> innerPlaceholders = new HashMap<>();
    private void findBracketPlaceholders(String params) {
        if (innerPlaceholders.containsKey(params)) return;
        char[] chars = params.toCharArray();
        int newPos = 0;
        Map<Integer,Integer> innerPlaceholders = new LinkedHashMap<>();
        List<Integer> brackets = new ArrayList<>();
        for (int i=0; i < chars.length; i++) {
            char c = chars[i];
            boolean escaped = i != 0 && chars[i-1]=='\\';
            if (escaped) {
                newPos++;
                continue;
            }
            if (c == '{')
                brackets.add(i-newPos);
            if (c == '}' && !brackets.isEmpty()) {
                innerPlaceholders.put(brackets.get(brackets.size()-1),i-newPos);
                brackets.remove(brackets.size()-1);
            }
        }
        this.innerPlaceholders.put(params,innerPlaceholders);
    }

    public String parseBracketPlaceholders(String params, OfflinePlayer viewer, Player target, boolean uncolorEach) {
        Map<Integer,Integer> innerPlaceholders = this.innerPlaceholders.get(params);
        StringBuilder str = new StringBuilder(params.replace("\\",""));
        Map<Integer,Integer> newPositions = new LinkedHashMap<>();
        for (int pos1 : innerPlaceholders.keySet()) {
            int pos2 = innerPlaceholders.get(pos1);

            for (int p : newPositions.keySet()) {
                int l = newPositions.get(p);
                if (p < pos1) pos1-=l;
                if (p < pos2) pos2-=l;
            }
            String sub = str.substring(pos1,pos2+1);
            String parsed = parsePlaceholders("%"+sub.substring(1,sub.length()-1)+"%",viewer,target);
            if (uncolorEach) parsed = ChatColor.stripColor(color(parsed));
            str.replace(pos1, pos2 + 1, parsed);

            newPositions.put(pos1,sub.length()-parsed.length());
        }
        return str.toString();
    }

}
