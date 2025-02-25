package script.player;

import script.*;
import script.library.*;

import static script.library.resource.getResourceContainerTemplate;

public class player_hand_sampling_droid extends script.base_script
{
    public static final float SAMPLE_DENSITY_THRESHOLD = 0.3f;
    public static final int SURVEY_TOOL_DELAY = 25;
    public static final int MIN_SURVEY_TOOL_DELAY = 10;
    private static String VAR_SAMPLE_ACTIVE = "HS_DROID_ACTIVE";
    private static String VAR_SAMPLE_RESOURCE = "HS_DROID_SAMPLE_RESOURCE";
    private static String VAR_LAST_SAMPLE_TIME = "HS_DROID_LAST_SAMPLE_TIME";
    private static String VAR_SAMPLE_RESOURCE_TYPE_ID = "HS_DROID_SAMPLE_RESOURCE_TYPE_ID";
    private static String VAR_SAMPLE_RESOURCE_DENSITY = "HS_DROID_SAMPLE_RESOURCE_DENSITY";
    private static String VAR_SAMPLE_SKILL_MOD = "HS_DROID_SAMPLE_SKILL_MOD";
    private static String VAR_SAMPLE_DELAY = "HS_DROID_SAMPLE_DELAY";
    private static String VAR_SAMPLE_RESOURCE_INCREASE = "HS_DROID_SAMPLE_RESOURCE_INCREASE";
    private static String VAR_SAMPLE_FAIL_RESOURCE_GEN = "HS_DROID_SAMPLE_FAIL_RESOURCE_GEN";
    private static String VAR_SAMPLE_COUNT = "HS_DROID_SAMPLE_COUNT";
    private static String VAR_SAMPLE_LOOPS = "HS_DROID_SAMPLE_LOOPS";
    private static string_id STR_HS_DROID_INVALID_RESOURCE = new string_id("spam", "hs_droid_invalid_resource");
    private static string_id STR_HS_DROID_NO_RESOURCE_DENSITY = new string_id("spam", "hs_droid_no_resource_density");
    private static string_id STR_HS_DROID_LOW_RESOURCE_DENSITY = new string_id("spam", "hs_droid_low_resource_density");
    private static string_id STR_HS_DROID_FAILED_GEN = new string_id("spam", "hs_droid_failed_gen");
    private static string_id STR_HS_DROID_DISPENSE_RESOURCES = new string_id("spam", "hs_droid_dispense_resources");
    private static string_id STR_HS_DROID_FAILED_DISPENSE_RESOURCES = new string_id("spam", "hs_droid_failed_dispense_resources");
    private static string_id STR_HS_DROID_NO_SURVEY = new string_id("spam", "hs_droid_no_survey");
    private static string_id STR_HS_DROID_BEGIN_SAMPLE = new string_id("spam", "hs_droid_begin_sample");
    public player_hand_sampling_droid()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        var res = utils.getStringObjVar(self, VAR_SAMPLE_RESOURCE);
        if (!isPlayer(self) || res == null || res.length() == 0) {
            detachScript(self, "player_hand_sampling_droid");
            return SCRIPT_CONTINUE;
        }

        prose_package pp = prose.getPackage(STR_HS_DROID_BEGIN_SAMPLE, res);
        sendSystemMessageProse(self, pp);
        messageTo(self, "getSample", new dictionary(), 1.0f, false);
        return SCRIPT_CONTINUE;
    }
    public static int getPlayerSurveyToolDelay(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return SURVEY_TOOL_DELAY;
        }
        int delay = SURVEY_TOOL_DELAY - (int)getSkillStatisticModifier(player, "expertise_resource_sampling_time_decrease");
        if (delay <= MIN_SURVEY_TOOL_DELAY)
        {
            delay = MIN_SURVEY_TOOL_DELAY;
        }
        return delay;
    }
    public static void beginHandSampleDroidLoop(obj_id hsDroidId, obj_id player) throws InterruptedException
    {
        location sampleLoc = player_utility.getPlayerSampleLoc(player);
        String sampleRes = player_utility.getPlayerSampleResource(player);
        sendSystemMessage(player, "Would sample " + sampleRes + " now.", "hs_droid");

        obj_id typeId = getResourceTypeByName(sampleRes);
        float density = getResourceEfficiency(typeId, getLocation(player));
        int modVal = getSkillStatMod(player, "surveying");
        int surveyDelay = getPlayerSurveyToolDelay(player);
        int resIncrease = getSkillStatisticModifier(player, "expertise_resource_sampling_increase");

        if (sampleLoc == null || sampleRes == null || !isIdValid(typeId) || density < SAMPLE_DENSITY_THRESHOLD)
        {
            sendSystemMessage(player, STR_HS_DROID_NO_SURVEY);
            return;
        }

        utils.setObjVar(player, VAR_SAMPLE_ACTIVE, true);
        utils.setObjVar(player, VAR_SAMPLE_RESOURCE, sampleRes);
        utils.setObjVar(player, VAR_SAMPLE_RESOURCE_TYPE_ID, typeId);
        utils.setObjVar(player, VAR_SAMPLE_RESOURCE_DENSITY, density);
        utils.setObjVar(player, VAR_SAMPLE_SKILL_MOD, modVal);
        utils.setObjVar(player, VAR_SAMPLE_DELAY, surveyDelay);
        utils.setObjVar(player, VAR_SAMPLE_RESOURCE_INCREASE, resIncrease);
        utils.setObjVar(player, VAR_LAST_SAMPLE_TIME, getGameTime());
        utils.setObjVar(player, VAR_SAMPLE_COUNT, 0);
        utils.setObjVar(player, VAR_SAMPLE_LOOPS, 0);


        utils.attachScript(player, "player_hand_sampling_droid");

    }
    public int performHandSampleDroidLoop(obj_id self, dictionary params) throws InterruptedException
    {
        //ensure active
        if (!utils.getBooleanObjVar(self, VAR_SAMPLE_ACTIVE))
        {
            return 0;
        }

        obj_id typeId = utils.getObjIdObjVar(self, VAR_SAMPLE_RESOURCE_TYPE_ID);
        if (!isIdValid(typeId))
        {
            debugSpeakMsg(self, "hand_sampling_droid::getSample: unable to retrieve obj_id for " + typeId);
            sendSystemMessage(self, STR_HS_DROID_INVALID_RESOURCE);
            return requestDroidCleanup(self);
        }

        int sampleDelay = utils.getIntObjVar(self, VAR_SAMPLE_DELAY);
        if (sampleDelay < MIN_SURVEY_TOOL_DELAY)
        {
            sampleDelay = MIN_SURVEY_TOOL_DELAY;
        }

        int lastSampleTime = utils.getIntObjVar(self, VAR_LAST_SAMPLE_TIME);
        int loopGameTime = getGameTime();
        long depletedTimestamp = getResourceDepletedTimestamp(typeId);
        long diff = 0;
        if (depletedTimestamp != -1 && depletedTimestamp < loopGameTime)
        {
            //sample has been depleted since last loop
            diff = depletedTimestamp - lastSampleTime;
        }
        else
        {
            if (depletedTimestamp == -1)
            {
                //not depleted
                diff = loopGameTime - lastSampleTime;
            }
            else
            {
                //error
                return requestDroidCleanup(self);
            }
        }
        int cyclesToProcess = (int)(diff / sampleDelay);
        int sumAmt = 0;
        for (int i = 0; i < cyclesToProcess; i++)
        {
            sumAmt += getHandSampleDroidAmount(self, typeId);
        }

        //save data related to the processing of the sampling
        utils.setObjVar(self, VAR_SAMPLE_COUNT, utils.getIntObjVar(self, VAR_SAMPLE_COUNT) + sumAmt);
        utils.setObjVar(self, VAR_SAMPLE_LOOPS, utils.getIntObjVar(self, VAR_SAMPLE_LOOPS) + 1);
        utils.setObjVar(self, VAR_LAST_SAMPLE_TIME, loopGameTime);

        //if depleted, request cleanup, otherwise, loop the sampling
        if (depletedTimestamp != -1 && depletedTimestamp < loopGameTime)
        {
            requestDroidCleanup(self);
        }
        else
        {
            messageTo(self, "getSample", new dictionary(), sampleDelay + 1, false);
        }
        return SCRIPT_CONTINUE;
    }
    public static int requestDroidCleanup(obj_id playerId)
    {
        messageTo(playerId, "handleDroidCleanup", new dictionary(), 1.0f, true);
        return SCRIPT_CONTINUE;
    }
    public int handleDroidCleanup(obj_id self, dictionary params) throws InterruptedException
    {
        //spawn the resources
        obj_id typeId = utils.getObjIdObjVar(self, VAR_SAMPLE_RESOURCE_TYPE_ID);
        if (!isIdValid(typeId))
        {
            debugSpeakMsg(self, "hand_sampling_droid::handleDroidCleanup: unable to retrieve obj_id for " + typeId);
            sendSystemMessage(self, STR_HS_DROID_INVALID_RESOURCE);
            return SCRIPT_CONTINUE;
        }

        String type = utils.getStringObjVar(self, VAR_SAMPLE_RESOURCE);

        int sampleCount = utils.getIntObjVar(self, VAR_SAMPLE_COUNT);

        String crateTemplate = getResourceContainerTemplate(typeId);
        if (!crateTemplate.equals(""))
        {
            obj_id pInv = utils.getInventoryContainer(self);
            if (pInv != null)
            {
                obj_id crate = createObject(crateTemplate, pInv, "");
                if (addResourceToContainer(crate, typeId, sampleCount, self))
                {
                    location curloc = getLocation(self);
                    setLocation(crate, curloc);
                    putIn(crate, pInv, self);

                    utils.setObjVar(self, VAR_SAMPLE_RESOURCE, "");
                    utils.setObjVar(self, VAR_SAMPLE_RESOURCE_TYPE_ID, obj_id.NULL_ID);
                    utils.setObjVar(self, VAR_SAMPLE_RESOURCE_DENSITY, 0.0f);
                    utils.setObjVar(self, VAR_SAMPLE_SKILL_MOD, 0);
                    utils.setObjVar(self, VAR_SAMPLE_DELAY, 0);
                    utils.setObjVar(self, VAR_SAMPLE_RESOURCE_INCREASE, 0);
                    utils.setObjVar(self, VAR_LAST_SAMPLE_TIME, -1);
                    utils.setObjVar(self, VAR_SAMPLE_COUNT, 0);
                    utils.setObjVar(self, VAR_SAMPLE_LOOPS, 0);

                    sendSystemMessageProse(self, prose.getPackage(STR_HS_DROID_DISPENSE_RESOURCES, type, sampleCount));
                }
                else
                {
                    sendSystemMessageProse(self, prose.getPackage(STR_HS_DROID_FAILED_DISPENSE_RESOURCES, type, sampleCount));
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    private static int getHandSampleDroidAmount(obj_id player, obj_id typeId) throws InterruptedException
    {
        float density = utils.getFloatObjVar(player, VAR_SAMPLE_RESOURCE_DENSITY);
        if (density < 0)
        {
            debugSpeakMsg(player, "hand_sampling_droid::getSample: density 0 for " + typeId);
            sendSystemMessage(player, STR_HS_DROID_NO_RESOURCE_DENSITY);
            return -1;
        }
        else if (density < 0.1f)
        {
            sendSystemMessage(player, STR_HS_DROID_LOW_RESOURCE_DENSITY);
            return -1;
        }
        int modVal = utils.getIntObjVar(player, VAR_SAMPLE_SKILL_MOD);
        float threshold = SAMPLE_DENSITY_THRESHOLD * ((100.0f - modVal) / 100.0f);

        //TODO: Need to pull this off the object's attributes
        float baseExtractionRate = 20.0f;

        if (density > threshold)
        {
            float deltaDensity = density - threshold;
            float famt = baseExtractionRate * deltaDensity;
            if (famt < 1)
            {
                sendSystemMessage(player, STR_HS_DROID_LOW_RESOURCE_DENSITY);
                return -1;
            }

            //might need renovations in Legends for city expertise
            int city_id = city.checkCity(player, false);

            //most of the chance logic should be identical to sampling
            float chance = 50.0f + 20.0f * ((modVal - 15.0f) / 85.0f);
            if (chance > 70)
            {
                chance = 70;
            }

            //sampling performs a roll that must be beaten by your chance to be successful
            int roll = rand(1, 100);
            //if sampling city, give 10% bonus to chance to sample
            if (city_id > 0 && city.cityHasSpec(city_id, city.SF_SPEC_SAMPLE_RICH))
            {
                chance += 10;
            }

            //if your chance beats your roll, you successfully sampled
            if (roll <= chance)
            {
                float resultModifier = ((2 * chance) - roll) / (2 * chance);
                int amt = (int)(famt * resultModifier);
                if (amt == 0)
                {
                    amt = 1;
                }
                if (city_id > 0 && city.cityHasSpec(city_id, city.SF_SPEC_SAMPLE_RICH))
                {
                    amt *= 1.2;
                }


                int rollResult = 10;
                if (isGod(player))
                {
                    rollResult = 100;
                }
                if (roll <= rollResult)
                {
                    // remnant logic from section of original hand sampling code that fired the SUI selection boxes.
                    // Kept this amount multiplier for crit rolls <= 50
                    int critRoll = rand(1, 100);
                    if (critRoll <= 50)
                    {
                        amt *= 2;
                    }
                }

                int expertiseResourceIncrease = utils.getIntObjVar(player, VAR_SAMPLE_RESOURCE_INCREASE);
                if (expertiseResourceIncrease > 0)
                {
                    amt += (int)(amt * expertiseResourceIncrease / 100.0f);
                }
                return amt;
            }
            else
            {
                return 0;
            }
        }
        else
        {
            sendSystemMessage(player, STR_HS_DROID_LOW_RESOURCE_DENSITY);
            return -1;
        }

    }
}
