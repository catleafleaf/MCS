package org.boxutil.units.builtin.console;

import org.boxutil.config.BoxConfigs;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;

public class ShowAAStatus implements BaseCommand {
    public CommandResult runCommand(@NotNull String s, @NotNull BaseCommand.CommandContext commandContext) {
        BoxConfigs.setAAShowEdge(Boolean.parseBoolean(s.toUpperCase()));
        return CommandResult.SUCCESS;
    }
}
