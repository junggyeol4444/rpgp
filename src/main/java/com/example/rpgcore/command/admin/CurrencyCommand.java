package com.example.rpgcore.command.admin;

import com.example.rpgcore.config.schema.CurrencyDefinition;
import com.example.rpgcore.economy.CurrencyService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rpg admin currency &lt;player&gt; &lt;give|take&gt; &lt;currencyId&gt; &lt;amount&gt; */
public final class CurrencyCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final CurrencyService currencies;
    private final Messages messages;

    public CurrencyCommand(PlayerManager players, CurrencyService currencies, Messages messages) {
        this.players = players;
        this.currencies = currencies;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "currency";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.currency.desc";
    }

    @Override
    public String argHint() {
        return "<player> <give|take> <currencyId> <amount>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        Player target = CommandUtil.onlinePlayer(args[0]);
        RpgPlayer rpgPlayer = target == null ? null : players.get(target);
        if (rpgPlayer == null) {
            messages.send(sender, "common.unknown-player", "name", args[0]);
            return;
        }
        String currencyId = args[2];
        if (currencies.definition(currencyId) == null) {
            messages.send(sender, "admin.currency.unknown", "currency", currencyId);
            return;
        }
        Integer amount = CommandUtil.parseInt(args[3]);
        if (amount == null || amount < 0) {
            messages.send(sender, "common.invalid-number", "input", args[3]);
            return;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        if (!mode.equals("give") && !mode.equals("take")) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        currencies.adjust(rpgPlayer, currencyId, mode.equals("give") ? amount : -amount);
        messages.send(sender, "admin.currency." + mode,
                "player", target.getName(),
                "currency", currencies.display(currencyId),
                "amount", amount,
                "owned", currencies.balance(rpgPlayer.data(), currencyId));
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
        }
        if (args.length == 2) {
            return List.of("give", "take");
        }
        if (args.length == 3) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            for (CurrencyDefinition definition : currencies.all()) {
                if (definition.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    ids.add(definition.id());
                }
            }
            return ids;
        }
        return List.of();
    }
}
