package ir.xenoncommunity.module.impl.security;

import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;
import ir.xenoncommunity.utils.BlacklistManager;
import ir.xenoncommunity.utils.Configuration;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.event.EventHandler;

@ModuleInfo(name = "Blacklist", version = 1.0, description = "Blocks connections from blacklisted IPs.")
public class BlacklistModule extends ModuleBase {

    public static BlacklistModule instance;
    private BlacklistManager blacklistManager;

    @Override
    public void onInit() {
        final Configuration.BlacklistModuleConfig cfg = getConfig().getModules().getBlacklist_module();
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        instance = this;
        blacklistManager = new BlacklistManager();
        getServer().getPluginManager().registerListener(null, this);
    }

    public BlacklistManager getBlacklistManager() {
        return blacklistManager;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (event.isCancelled()) return;

        final String ip = event.getConnection().getAddress().getAddress().getHostAddress();
        if (blacklistManager.isBlacklisted(ip)) {
            final Configuration.BlacklistModuleConfig cfg = getConfig().getModules().getBlacklist_module();
            event.setCancelReason(TextComponent.fromLegacyText(cfg.getKick_message()));
            event.setCancelled(true);
        }
    }
}