package io.github.tanguygab.nestedplaceholdersexpansion;


import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

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
        return "1.2.1";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String num = params.split("_")[0];
        int number = 2;
        try {number = Integer.parseInt(num);}
        catch (Exception e) {num = "";}

        if (!num.equals(""))
            params = params.replace(num+"_","");

        params = "%"+params+"%";

        for (int i = 0; i < number; i++) {
            params = PlaceholderAPI.setBracketPlaceholders(player,params);
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
}
