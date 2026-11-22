# MyEconomy 1.0 (Minecraft 26.3, Paper, Java 25)

A simple economy that plugs into Vault, so the AuctionHouse plugin (and any other Vault plugin) can use it.

Commands
- /bal [player]  (aliases: /balance, /money)
- /pay <player> <amount>   (player must be online, min $1, accepts 500, 1.5k, 2m, 3b, 1t)
- /baltop   top 10 richest
- /eco <give|take|set> <player> <amount>   (needs myeconomy.admin, ops by default)

Install: Vault + MyEconomy + AuctionHouse in your plugins folder.
Do NOT also run another economy plugin (like EssentialsX's economy), or they will fight.

Config: starting-balance, currency-symbol, currency-singular, currency-plural
Balances are stored in plugins/MyEconomy/balances.yml
