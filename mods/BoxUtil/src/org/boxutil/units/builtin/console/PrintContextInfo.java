package org.boxutil.units.builtin.console;

import org.boxutil.define.BoxDatabase;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class PrintContextInfo implements BaseCommand {
    public CommandResult runCommand(@NotNull String s, @NotNull BaseCommand.CommandContext commandContext) {
        BoxDatabase.GLState state = BoxDatabase.getGLState();
        Console.showMessage(state.getPrintInfo());
        state.print();
        return CommandResult.SUCCESS;
    }
}
