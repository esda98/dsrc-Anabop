package script.systems.resource_harvesting;

import script.*;
import script.library.bounty_hunter;
import script.library.groundquests;
import script.library.regions;
import script.library.utils;

public class hand_sampling_droid extends script.systems.missions.base.mission_dynamic_base
{
    private static string_id STR_NOT_TRADER = new string_id("spam", "hs_droid_not_trader");
    private static string_id STR_HS_DROID_ACTIVATE = new string_id("spam", "hs_droid_activate");
    private static string_id STR_HS_DROID_NO_SURVEY = new string_id("spam", "hs_droid_no_survey");
    private static string_id STR_HS_DROID_TOO_FAR = new string_id("spam", "hs_droid_survey_too_far");
    public hand_sampling_droid()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        int intRootMenu;
        //ensure the requesting player is a trader to use
        if (!utils.isProfession(player, utils.TRADER))
        {
            sendSystemMessage(player, STR_NOT_TRADER);
            return SCRIPT_CONTINUE;
        }

        //ensure the player has a surveyed resource already
        location sampleLoc = getSampleLocation(player);
        if (sampleLoc != null && getSampleResourceType(player) != null)
        {
            //ensure the player is nearby their sample location
            if (getDistance(player, sampleLoc) < 100)
            {
                mi.addRootMenu(menu_info_types.SERVER_PROBE_DROID_ACTIVATE, STR_HS_DROID_ACTIVATE);
            }
            else
            {
                sendSystemMessage(player, STR_HS_DROID_TOO_FAR);
            }
        }
        else
        {
            sendSystemMessage(player, STR_HS_DROID_NO_SURVEY);
        }

        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.EXAMINE)
        {
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.ITEM_DESTROY)
        {
            return SCRIPT_CONTINUE;
        }

        //hand sampling droid attempting to activate
        if (item == menu_info_types.SERVER_PROBE_DROID_ACTIVATE)
        {
            //ensure the player has a surveyed resource already
            location sampleLoc = getSampleLocation(player);
            if (sampleLoc != null && getSampleResourceType(player) != null)
            {
                beginSampling(self, player);
            }
            else
            {
                sendSystemMessage(player, STR_HS_DROID_NO_SURVEY);
            }
        }
        return SCRIPT_CONTINUE;
    }
    private location getSampleLocation(obj_id playerId)
    {
        return getLocationObjVar(playerId, "surveying.sampleLocation");
    }
    private String getSampleResourceType(obj_id playerId) throws InterruptedException
    {
        return utils.getStringScriptVar(playerId, "surveying.resource");
    }
    private void beginSampling(obj_id self, obj_id player) throws InterruptedException
    {
        location sampleLoc = getSampleLocation(player);
        String sampleResType = getSampleResourceType(player);
        sendSystemMessage(player, "Would sample " + sampleResType + " now.", "hs_droid");
    }


//    location sampleLocation = getLocationObjVar(player, "surveying.sampleLocation");
//    String resource_type = utils.getStringScriptVar(player, "surveying.resource");
}
