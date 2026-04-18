package dev.frost.frostcore.utils;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyUtil {

    private static Economy economy = null;
    private static boolean enabled = false;

    public static void init() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            FrostLogger.warn("Vault is not enabled! Economy features will be disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            FrostLogger.warn("No Vault Economy provider found! Economy features will be disabled.");
            return;
        }
        
        economy = rsp.getProvider();
        enabled = true;
        FrostLogger.info("Hooked into Vault Economy (" + economy.getName() + ").");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static boolean has(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        return economy.has(player, amount);
    }

    public static boolean withdraw(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public static boolean deposit(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public static double getBalance(OfflinePlayer player) {
        if (!enabled) return 0.0;
        return economy.getBalance(player);
    }

    public static String format(double amount) {
        if (!enabled) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    public static boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        if (!enabled || amount <= 0) return false;
        if (!has(from, amount)) return false;
        
        if (withdraw(from, amount)) {
            if (deposit(to, amount)) {
                return true;
            } else {
                deposit(from, amount);
            }
        }
        return false;
    }

    public static boolean setBalance(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        double current = getBalance(player);
        if (current > amount) {
            return withdraw(player, current - amount);
        } else if (current < amount) {
            return deposit(player, amount - current);
        }
        return true;
    }

    public static boolean reset(OfflinePlayer player) {
        return setBalance(player, 0.0);
    }

    private static final String[] SUFFIXES = {"", "k", "m", "b", "t", "q", "Q"};

    public static String formatCompact(double amount) {
        boolean negative = amount < 0;
        amount = Math.abs(amount);
        
        if (amount < 1000) {
            String result = String.format("%.2f", amount).replaceAll("\\.00$", "");
            return negative ? "-" + result : result;
        }

        int exp = (int) (Math.log(amount) / Math.log(1000));
        if (exp >= SUFFIXES.length) exp = SUFFIXES.length - 1;

        String formatted = String.format("%.2f", amount / Math.pow(1000, exp));
        formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");

        String result = formatted + SUFFIXES[exp];
        return negative ? "-" + result : result;
    }

    public static double parseCompact(String value) throws NumberFormatException {
        if (value == null || value.trim().isEmpty()) return 0.0;
        value = value.toLowerCase().replace(",", "").trim();
        
        double multiplier = 1.0;
        if (value.endsWith("k")) {
            multiplier = 1_000.0;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("m")) {
            multiplier = 1_000_000.0;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("q")) {
            multiplier = 1_000_000_000_000_000.0;
            value = value.substring(0, value.length() - 1);
        }
        
        return Double.parseDouble(value) * multiplier;
    }
}
