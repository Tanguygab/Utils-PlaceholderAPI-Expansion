package io.github.tanguygab.utilsexpansion;


import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;

public final class UtilsExpansion extends PlaceholderExpansion implements Relational {

    private final List<String> placeholders = new ArrayList<>();

    public UtilsExpansion() {
        List<String> placeholders = Arrays.asList("parse","parse:<num>","color","uncolor","uncolor:each","parseother:[name|placeholder]","escape","parserel:[name|placeholder]");
        placeholders.forEach(placeholder->{
            this.placeholders.add("%utils_"+placeholder+"_<placeholder>%");
            this.placeholders.add("%rel_utils_"+placeholder+"_<placeholder>%");
        });
    }

    @Override
    public @Nonnull List<String> getPlaceholders() {
        return placeholders;
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
        return "1.0.3";
    }

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

        if (arg.startsWith("parseother:[") && params.contains("]")) {
            String placeholder = params.substring(12,params.indexOf("]"));
            String name = processParse(placeholder,1,viewer,target).replace("%","");
            return processParse(params.substring(params.indexOf("]")+2),1, Bukkit.getServer().getOfflinePlayer(name),target);
        }
        if (arg.startsWith("parserel:[") && params.contains("]")) {
            String placeholder = params.substring(10,params.indexOf("]"));
            String name = processParse(placeholder,1,viewer,target).replace("%","");
            return processParse(params.substring(params.indexOf("]")+2),1, viewer,Bukkit.getServer().getPlayer(name));
        }
        if (arg.startsWith("parse")) {
            int number = 1;
            if (arg.startsWith("parse:"))
                try {number = Integer.parseInt(arg.substring(6));}
                catch (Exception ignored) {}
            return processParse(text,number,viewer,target);
        }
        if (arg.equalsIgnoreCase("color")) return color(processParse(text,1,viewer,target));
        if (arg.startsWith("uncolor")) return ChatColor.stripColor(color(processParse(text,1,viewer,target,arg.equalsIgnoreCase("uncolor:each"))));
        return null;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&',text);
    }


    private String processParse(String text, int number, OfflinePlayer viewer, Player target) {
        return processParse(text,number,viewer,target,false);
    }
    private String processParse(String text, int number, OfflinePlayer viewer, Player target, boolean uncolorEach) {
        text = "%"+text+"%";

        for (int i = 0; i < number; i++) {
            findBracketPlaceholders(text);
            text = parseBracketPlaceholders(text,viewer,null,uncolorEach);
            text = parsePlaceholders(text,viewer,target);
        }
        return text;
    }

    private String parsePlaceholders(String text, OfflinePlayer viewer, Player target) {
        if (target == null)
            return PlaceholderAPI.setPlaceholders(viewer,text);
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
