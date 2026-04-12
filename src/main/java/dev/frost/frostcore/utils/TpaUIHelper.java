package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class TpaUIHelper {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final MessageManager mm = MessageManager.get();

    public static void sendTpaRequest(Player target, String senderName, String description, int expiry) {

        String div = "<gradient:#6B8DAE:#8BADC4><strikethrough>                                                          </strikethrough></gradient>";

        Component accept = MINI.deserialize("<click:run_command:'/tpaccept " + senderName + "'><hover:show_text:'<#7ECFA0>Click to accept!'> <#7ECFA0><bold>✔ ACCEPT</bold> </hover></click>");
        Component decline = MINI.deserialize("<click:run_command:'/tpdecline " + senderName + "'><hover:show_text:'<#D4727A>Click to deny!'> <#D4727A><bold>✖ DECLINE</bold> </hover></click>");

        Component message = Component.empty()
                .append(MINI.deserialize("\n"))
                .append(MINI.deserialize(div))
                .append(MINI.deserialize("\n"))
                .append(MINI.deserialize(" <gradient:#6B8DAE:#8BADC4><bold>⚡ TELEPORT REQUEST</bold></gradient>"))
                .append(MINI.deserialize("\n\n"))
                .append(MINI.deserialize(" <white>" + senderName + "</white> <#8FA3BF>" + description + ".</#8FA3BF>"))
                .append(MINI.deserialize("\n\n"))
                .append(MINI.deserialize(" <dark_gray>Expires in <white>" + expiry + "s</white>.</dark_gray>"))
                .append(MINI.deserialize("\n\n"))
                .append(MINI.deserialize("     "))
                .append(accept)
                .append(MINI.deserialize("      <dark_gray>│</dark_gray>   "))
                .append(decline)
                .append(MINI.deserialize("\n"))
                .append(MINI.deserialize(div))
                .append(MINI.deserialize("\n"));

        target.sendMessage(message);
    }
}

