package script.theme_park.dungeon.mustafar_trials.sher_kar_hm;

import script.dictionary;
import script.library.*;
import script.obj_id;

public class sher_kar_hm_manager extends script.base_script
{
    public sher_kar_hm_manager()
    {
    }
    public static final boolean LOGGING = false;
    public int beginSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        clearEventArea(self);
        messageTo(self, "prepareEventArea", null, 5, false);
        return SCRIPT_CONTINUE;
    }
    public int dungeonCleanup(obj_id self, dictionary params) throws InterruptedException
    {
        clearEventArea(self);
        return SCRIPT_CONTINUE;
    }
    public int cleanupSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        clearEventArea(self);
        return SCRIPT_CONTINUE;
    }
    public void clearEventArea(obj_id dungeon) throws InterruptedException
    {
        //this should be able to stay as is
        obj_id[] contents = trial.getAllObjectsInDungeon(dungeon);
        if (contents == null || contents.length == 0)
        {
            doLogging("clearEventArea", "Dungeon was empty, return");
            return;
        }
        for (obj_id content : contents) {
            if (isPlayer(content)) {
            } else {
                if (isMob(content)) {
                    trial.cleanupNpc(content);
                } else if (trial.isTempObject(content)) {
                    trial.cleanupNpc(content);
                }
            }
        }
    }
    public int prepareEventArea(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] spawnPointObjIds = trial.getObjectsInDungeonWithObjVar(self, trial.MONSTER_WP);
        if (spawnPointObjIds == null || spawnPointObjIds.length == 0)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id[] phase1Guards = new obj_id[5];
        int guardIndex = 0;
        for (obj_id spawnPointObjId : spawnPointObjIds)
        {
            var spawnPointName = getStringObjVar(spawnPointObjId, trial.MONSTER_WP);
            switch (spawnPointName)
            {
                case "sher_kar_hm":
                    obj_id sherKar = create.object("som_sherkar_hm", getLocation(spawnPointObjId));
                    setYaw(sherKar, -10);
                    utils.setScriptVar(self, trial.MONSTER_SHER_KAR, sherKar);
                    break;
                case "praetorian_hm0", "praetorian_hm1":
                    phase1Guards[guardIndex] = create.object("som_sherkar_praetorian_hm", getLocation(spawnPointObjId));
                    //Mark - Update to face the cave opening
                    faceTo(phase1Guards[guardIndex], utils.getObjIdScriptVar(self, trial.MONSTER_SHER_KAR));
                    guardIndex++;
                    break;
                case "karling_hm0", "karling_hm1":
                    phase1Guards[guardIndex] = create.object("som_sherkar_karling_hm", getLocation(spawnPointObjId));
                    //Mark - Update to face the cave opening
                    faceTo(phase1Guards[guardIndex], utils.getObjIdScriptVar(self, trial.MONSTER_SHER_KAR));
                    guardIndex++;
                    break;
                case "symbiot_hm0", "symbiot_hm1":
                    phase1Guards[guardIndex] = create.object("som_sherkar_symbiot_hm", getLocation(spawnPointObjId));
                    //Mark - Update to face the cave opening
                    faceTo(phase1Guards[guardIndex], utils.getObjIdScriptVar(self, trial.MONSTER_SHER_KAR));
                    guardIndex++;
                    break;
            }
        }
        ai_lib.establishAgroLink(phase1Guards[0], phase1Guards);
        return SCRIPT_CONTINUE;
    }
    public int doMidEvent(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] players = trial.getValidTargetsInDungeon(self);
        if (players == null || players.length == 0)
        {
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
    public int doEndEvent(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] players = trial.getValidTargetsInDungeon(self);
        if (players == null || players.length == 0)
        {
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
    public int sherKarDied(obj_id self, dictionary params) throws InterruptedException
    {
        utils.sendSystemMessagePob(self, trial.MONSTER_SK_DEFEATED);
        obj_id[] players = trial.getPlayersInDungeon(self);
        badge.grantBadge(players, "bdg_must_kill_sher_kar");
        instance.setClock(self, 305);
        return SCRIPT_CONTINUE;
    }
    public void doLogging(String section, String message) throws InterruptedException
    {
        if (LOGGING || trial.MONSTER_LOGGING)
        {
            LOG("doLogging/monster_manager/" + section, message);
        }
    }
}
