# Parse-Expansion
Parse colors and nested PAPI placeholders within PAPI placeholders

# Usage
`%placeholder_parse_<placeholder>%` - Parses the placeholder once with inner placeholders  
`%placeholder_parse:#_<placeholder>%` - Parses the placeholder # times with inner placeholders each time
(Also supports relational placeholders! Just add `rel_` after the first `%`)

# Example
`%javascript_test%` returns `%player_name%` which returns a player's name  
`%placeholder_parse:2_javascript_test%` will be parsed 2 times, it will first be parsed into %player_name% and then parsed again into the player's name.

`%placeholder_parse_server_online_{player_world}%` will parse `{player_world}` first and then `%server_online_` + output of the world placeholder + `%`

Since newer versions, PAPI doesn't parse colors anymore, so `%luckperms_prefix%` returns `&4Admin &c` as plain text.
`%placeholder_color_luckperms_prefix%` allows you to parse colors, thus returning `§4Admin §c`