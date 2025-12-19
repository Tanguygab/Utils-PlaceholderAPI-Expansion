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
        List<String> placeholders = List.of(
                "parse", "parse:<num>",
                "color", "uncolor", "uncolor:each",
                "parseother:[name]", "parseplaceholder:[placeholder]",
                "parserel:[name]", "parserelplaceholder:[placeholder]",
                "escape",
                "try_<placeholder>", "trycatch:<defaultvalue>_<placeholder>",
                "default_<placeholder>", "default:<defaultvalue>_<placeholder>"
        );
        placeholders.forEach(placeholder -> {
            this.placeholders.add("%utils_" + placeholder + "_<placeholder>%");
            this.placeholders.add("%rel_utils_" + placeholder + "_<placeholder>%");
        });
        defaults.put("shortcuts", new HashMap<String, String>() {{
            put("othermath", "%utils_parseother:[{0}]_math_{1}+1%");
        }});
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
        return "1.0.13";
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
            return parsePlaceholders(shortcut,viewer,target);
        }

        if (arg.startsWith("parseother:[") && params.contains("]")) {
            String name = params.substring(12,params.indexOf("]"));
            return processParse(params.substring(params.indexOf("]")+2), name.isEmpty() ? null : getPlayer(name),target);
        }
        if (arg.startsWith("parseplaceholder:[") && params.contains("]")) {
            String placeholder = params.substring(18,params.indexOf("]"));
            String name = processParse(placeholder, viewer, target).replace("%","");
            return processParse(params.substring(params.indexOf("]")+2), name.isEmpty() ? null : getPlayer(name),target);
        }

        if (arg.startsWith("parserel:[") && params.contains("]")) {
            String name = params.substring(10,params.indexOf("]"));
            return processParse(params.substring(params.indexOf("]")+2), viewer, name.isEmpty() ? null : getPlayer(name).getPlayer());
        }
        if (arg.startsWith("parserelplaceholder:[") && params.contains("]")) {
            String placeholder = params.substring(21,params.indexOf("]"));
            String name = processParse(placeholder, viewer,target).replace("%","");
            return processParse(params.substring(params.indexOf("]")+2), viewer, name.isEmpty() ? null : getPlayer(name).getPlayer());
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
            boolean percent = args.length < 3 || args[2].equalsIgnoreCase("true");
            return processParse(text,number,viewer,target,percent);
        }
        if (arg.equalsIgnoreCase("color")) return color(processParse(text, viewer,target));
        if (arg.startsWith("uncolor")) return ChatColor.stripColor(color(processParse(text,1,viewer,target,true,arg.equalsIgnoreCase("uncolor:each"))));

        if (arg.startsWith("try")) {
            String def = arg.startsWith("trycatch:") ? arg.substring(9) : "";
            try {return processParse(text,viewer,target);}
            catch (Exception e) {return def;}
        }

        if (arg.startsWith("default")) {
            String def = arg.startsWith("default:") ? arg.substring(8) : "";
            String output = processParse(text,viewer,target);
            return output.isEmpty() || output.equals("%"+text+"%") ? def : output;
        }

        return null;
    }

    private OfflinePlayer getPlayer(String uuidOrName) {
        try {
            return Bukkit.getServer().getPlayer(UUID.fromString(uuidOrName));
        } catch (IllegalArgumentException e) {
            Player player = Bukkit.getServer().getPlayer(uuidOrName);
            if (player != null) return player;
            //noinspection deprecation
            return Bukkit.getServer().getOfflinePlayer(uuidOrName);
        }
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
            text = parseBracketPlaceholders(text,viewer,target,uncolorEach);
            text = parsePlaceholders(text,viewer,target);
        }
        return text;
    }

    private String parsePlaceholders(String text, OfflinePlayer viewer, Player target) {
        text = PlaceholderAPI.setPlaceholders(viewer,text);
        if (target != null)
            text = PlaceholderAPI.setRelationalPlaceholders(viewer.getPlayer(),target,text);
        return text;
    }

    private String parseBracketPlaceholders(String params, OfflinePlayer viewer, Player target, boolean uncolorEach) {
        StringBuilder str = new StringBuilder(params);

        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            boolean escaped = i != 0 && str.charAt(i - 1) == '\\';

            if (escaped) {
                --i;
                continue;
            }

            if (c == '{') {
                int end = -1;
                for (int j = i+2; j < str.length(); j++) {
                    if (str.charAt(j) == '}' && str.charAt(j-1) != '\\') {
                        end = j;
                        break;
                    }
                }
                if (end == -1) continue;

                String placeholder = str.substring(i, end + 1);
                String parsed = parsePlaceholders("%" + placeholder.substring(1, placeholder.length()-1) + "%", viewer, target);
                if (uncolorEach) parsed = ChatColor.stripColor(color(parsed));
                str.replace(i, end + 1, parsed);
            }
        }
        return str.toString();
    }

}
