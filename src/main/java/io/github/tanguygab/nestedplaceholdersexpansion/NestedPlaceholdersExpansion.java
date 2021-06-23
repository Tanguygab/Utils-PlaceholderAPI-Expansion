package io.github.tanguygab.nestedplaceholdersexpansion;


import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

public final class NestedPlaceholdersExpansion extends PlaceholderExpansion {

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("%nested_<num>_<placeholder>%");
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
        return "1.0";
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
            params = PlaceholderAPI.setPlaceholders(player,params);
        }
        return params;
    }
}
