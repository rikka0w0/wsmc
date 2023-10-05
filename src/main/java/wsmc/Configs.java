package wsmc;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = WSMC.MODID)
@Config.Gui.Background("minecraft:textures/block/oak_planks.png")
public class Configs implements ConfigData {
    @ConfigEntry.Category("spawnsetting")
    @ConfigEntry.Gui.PrefixText
    public boolean enableWebsocket = true;
}
