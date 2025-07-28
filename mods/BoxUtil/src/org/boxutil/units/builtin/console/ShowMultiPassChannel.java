package org.boxutil.units.builtin.console;

import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxEnum;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class ShowMultiPassChannel implements BaseCommand {
    public CommandResult runCommand(@NotNull String s, @NotNull BaseCommand.CommandContext commandContext) {
        if (s.isEmpty()) {
            BoxConfigs.setMultiPassMode(BoxEnum.MP_BEAUTY);
        } else {
            String upCase = s.toUpperCase();
            if (upCase.contentEquals("BEAUTY")) BoxConfigs.setMultiPassMode(BoxEnum.MP_BEAUTY);
            else if (upCase.contentEquals("DATA")) BoxConfigs.setMultiPassMode(BoxEnum.MP_DATA);
            else if (upCase.contentEquals("EMISSIVE")) BoxConfigs.setMultiPassMode(BoxEnum.MP_EMISSIVE);
            else if (upCase.contentEquals("NORMAL")) BoxConfigs.setMultiPassMode(BoxEnum.MP_NORMAL);
            else if (upCase.contentEquals("BLOOM")) BoxConfigs.setMultiPassMode(BoxEnum.MP_BLOOM);
            else {
                Console.showMessage("Error: no such channel '" + s + "'! Valid channel: [ 'none' / BEAUTY / DATA / EMISSIVE / NORMAL / BLOOM ].");
                return CommandResult.ERROR;
            }
        }
        return CommandResult.SUCCESS;
    }
}
