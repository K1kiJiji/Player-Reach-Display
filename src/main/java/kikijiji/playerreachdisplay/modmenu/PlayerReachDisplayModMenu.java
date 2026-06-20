package kikijiji.playerreachdisplay.modmenu;


import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import kikijiji.playerreachdisplay.screen.PlayerReachDisplayConfigScreen;


public class PlayerReachDisplayModMenu implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return PlayerReachDisplayConfigScreen::new;
    }
}