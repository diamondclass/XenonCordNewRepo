package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;

public class WhitelistUtils {
    public static boolean isWhitelisted(String ip, String username) {
        Configuration.ConfigData config = XenonCore.instance.getConfigData();
        if (config == null || config.getGeneral() == null || config.getGeneral().getWhitelist() == null) {
            return false;
        }
        Configuration.Whitelist whitelist = config.getGeneral().getWhitelist();
        if ((whitelist.getIps() == null || whitelist.getIps().length == 0) && 
            (whitelist.getUsers() == null || whitelist.getUsers().length == 0)) {
            return false;
        }
        if (ip != null && whitelist.getIps() != null) {
            String cleanIp = ip.trim();
            for (String whitelistedIp : whitelist.getIps()) {
                if (cleanIp.equalsIgnoreCase(whitelistedIp.trim())) {
                    return true;
                }
            }
        }
        if (username != null && whitelist.getUsers() != null) {
            String cleanUser = username.trim();
            for (String whitelistedUser : whitelist.getUsers()) {
                if (cleanUser.equalsIgnoreCase(whitelistedUser.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}