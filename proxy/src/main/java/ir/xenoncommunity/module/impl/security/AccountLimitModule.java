package ir.xenoncommunity.module.impl.security;

import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.WhitelistUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.event.EventHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ModuleInfo(name = "AccountLimit", version = 1.0, description = "Limits the number of accounts per IP address.")
public class AccountLimitModule extends ModuleBase {

    private final ConcurrentHashMap<InetAddress, AtomicInteger> connectionCount = new ConcurrentHashMap<>();

    @Override
    public void onInit() {
        if (!getConfig().getModules().getAccount_limit_module().isEnabled()) {
            return;
        }
        getServer().getPluginManager().registerListener(null, this);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (event.isCancelled()) return;

        final InetAddress address = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress();

        if (WhitelistUtils.isWhitelisted(address.getHostAddress(), event.getConnection().getName())) return;

        final int limit = getConfig().getModules().getAccount_limit_module().getMax_accounts();
        final int count = connectionCount.getOrDefault(address, new AtomicInteger(0)).get();

        if (count >= limit) {
            String kickMsg = getConfig().getModules().getAccount_limit_module().getKick_message()
                    .replace("%limit%", String.valueOf(limit))
                    .replace("%count%", String.valueOf(count));
            event.setCancelReason(TextComponent.fromLegacyText(Message.translateColor(kickMsg)));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        final InetAddress address = event.getPlayer().getAddress().getAddress();
        connectionCount.computeIfAbsent(address, k -> new AtomicInteger(0)).incrementAndGet();
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        final InetAddress address = event.getPlayer().getAddress().getAddress();
        connectionCount.computeIfPresent(address, (k, counter) -> {
            if (counter.decrementAndGet() <= 0) return null;
            return counter;
        });
    }
}