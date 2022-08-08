package io.github.tanguygab.nestedplaceholdersexpansion;


import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public final class NestedPlaceholdersExpansion extends PlaceholderExpansion implements Relational {

    @Override
    public List<String> getPlaceholders() {
        return Arrays.asList("%nested_<placeholder>%",
                "%nested_<num>_<placeholder>%",
                "%rel_nested_<placeholder>%",
                "%rel_nested_<num>_<placeholder>%");
    }

    @Override
    public String getIdentifier() {
        return "nested";
    }

    @Override
    public String getAuthor() {
        return "Tanguygab";
    }

    @Override
    public String getVersion() {
        return "1.3.1";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String num = params.split("_")[0];
        int number = 1;
        try {number = Integer.parseInt(num);}
        catch (Exception e) {num = "";}

        if (!num.equals(""))
            params = params.replace(num+"_","");

        params = "%"+params+"%";


        for (int i = 0; i < number; i++) {
            params = parseBracketPlaceholders(player,params.replace("\\",""),findBracketPlaceholders(params));
            params = PlaceholderAPI.setPlaceholders(player,params);
        }
        return params;
    }


    @Override
    public String onPlaceholderRequest(Player player, Player player1, String params) {
        String num = params.split("_")[0];
        int number = 2;
        try {number = Integer.parseInt(num);}
        catch (Exception e) {num = "";}

        if (!num.equals(""))
            params = params.replace(num+"_","");

        params = "%"+params+"%";

        for (int i = 0; i < number; i++) {
            params = PlaceholderAPI.setRelationalPlaceholders(player,player1,params);
            params = PlaceholderAPI.setBracketPlaceholders(player1,params);
            params = PlaceholderAPI.setPlaceholders(player1,params);
        }
        return params;
    }

    public Map<Integer,Integer> findBracketPlaceholders(String params) {
        char[] chars = params.toCharArray();
        int newPos = 0;
        Map<Integer,Integer> innerPlaceholders = new HashMap<>();
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
        return innerPlaceholders;
    }

    public String parseBracketPlaceholders(OfflinePlayer player, String params, Map<Integer,Integer> innerPlaceholders) {
        StringBuilder str = new StringBuilder(params);
        Map<Integer,Integer> newPositions = new HashMap<>();
        for (int pos1 : innerPlaceholders.keySet()) {
            int pos2 = innerPlaceholders.get(pos1);

            for (int p : newPositions.keySet()) {
                int l = newPositions.get(p);
                if (p < pos1) pos1-=l;
                if (p < pos2) pos2-=l;
            }
            String sub = str.substring(pos1,pos2+1);
            String parsed = PlaceholderAPI.setBracketPlaceholders(player,sub);

            str.replace(pos1, pos2 + 1, parsed);

            newPositions.put(pos1,sub.length()-parsed.length());
        }
        return str.toString();
    }
}
