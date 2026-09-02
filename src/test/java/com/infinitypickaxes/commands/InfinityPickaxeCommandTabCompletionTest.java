package com.infinitypickaxes.commands;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfinityPickaxeCommandTabCompletionTest {
    @Test void gearCommandSuggestsBookSubcommandToAdministrators() {
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);
        when(command.getName()).thenReturn("infinitygear");
        when(sender.hasPermission("infinitygear.admin")).thenReturn(true);

        var suggestions = new InfinityPickaxeCommand(mock(InfinityPickaxes.class))
                .onTabComplete(sender, command, "igear", new String[]{"b"});

        assertTrue(suggestions.contains("book"));
    }
}
