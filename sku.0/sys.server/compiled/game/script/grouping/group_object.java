package script.grouping;

import script.*;
import script.library.utils;
import script.player.player_utility;

import java.util.*;

public class group_object extends script.base_script
{
    public static final string_id SID_READY_CHECK_RESPONSE_NO_CHECK = new string_id("spam", "ready_check_response_no_check");
    public static final string_id SID_READY_CHECK_CANCELLED = new string_id("spam", "ready_check_cancelled");
    public static final string_id SID_READY_CHECK_TWO_OR_MORE = new string_id("spam", "ready_check_two_or_more");
    public static final string_id SID_READY_CHECK_START = new string_id("spam", "ready_check_start");
    public static final string_id SID_READY_CHECK_DUPLICATE_YES = new string_id("spam", "ready_check_duplicate_yes");
    public static final string_id SID_READY_CHECK_DUPLICATE_NO = new string_id("spam", "ready_check_duplicate_no");
    public static final string_id SID_READY_CHECK_MUST_BE_LEADER = new string_id("spam", "ready_check_must_be_leader");
    public static final string_id SID_READY_CHECK_MUST_BE_GROUPED = new string_id("spam", "ready_check_must_be_grouped");

    public group_object()
    {
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int volleyTargetDone(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] members = getGroupMemberIds(self);
        if (members == null || members.length < 1)
        {
            return SCRIPT_CONTINUE;
        }
        for (obj_id member : members) {
            messageTo(member, "volleyTargetDone", params, 0, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int addGroupMember(obj_id self, dictionary params) throws InterruptedException
    {
        if (params != null)
        {
            obj_id newGroupMember = params.getObjId("memberObjectId");
            if (isIdValid(newGroupMember))
            {
                deltadictionary scriptVars = self.getScriptVars();
                if (scriptVars != null)
                {
                    selectNearestGroupMission(self);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int removeGroupMember(obj_id self, dictionary params) throws InterruptedException
    {
        if (params != null)
        {
            obj_id groupMember = params.getObjId("sender");
            if (isIdValid(groupMember))
            {
                deltadictionary scriptVars = self.getScriptVars();
                if (scriptVars != null)
                {
                    selectNearestGroupMission(self);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    private location calculateNearestGroupMission(obj_id self) throws InterruptedException
    {
        location result;
        deltadictionary scriptVars = self.getScriptVars();
        if (scriptVars != null)
        {
            location[] memberLocations = scriptVars.getLocationArray("memberLocations");
            if ((memberLocations == null) || (memberLocations.length < 1))
            {
                return null;
            }
            location[] missionLocations = scriptVars.getLocationArray("missionLocations");
            if ((missionLocations == null) || (missionLocations.length < 1))
            {
                return null;
            }
            location nearestGroupMission = scriptVars.getLocation("nearestGroupMission");
            if (nearestGroupMission == null)
            {
                if (missionLocations.length > 0)
                {
                    nearestGroupMission = (location)missionLocations[0].clone();
                }
                else 
                {
                    return null;
                }
            }
            Hashtable bestLocations = new Hashtable();
            int memberLocationIndex = 0;
            int preferredIndex = -1;
            for (memberLocationIndex = 0; memberLocationIndex < memberLocations.length; ++memberLocationIndex)
            {
                int missionLocationIndex = 0;
                float bestDistance = -1;
                for (missionLocationIndex = 0; missionLocationIndex < missionLocations.length; ++missionLocationIndex)
                {
                    location currentLocation = (location)missionLocations[missionLocationIndex].clone();
                    if (currentLocation.area.equals(memberLocations[memberLocationIndex].area))
                    {
                        float currentDistance = currentLocation.distance(memberLocations[memberLocationIndex]);
                        if (bestDistance < 0 || currentDistance < bestDistance)
                        {
                            preferredIndex = missionLocationIndex;
                            bestDistance = currentDistance;
                        }
                    }
                }
                if (preferredIndex > -1)
                {
                    Integer p = preferredIndex;
                    if (bestLocations.containsKey(preferredIndex))
                    {
                        int votes = (Integer) bestLocations.get(p);
                        votes = votes + 1;
                        bestLocations.put(p, votes);
                    }
                    else 
                    {
                        Integer votes = 1;
                        bestLocations.put(p, votes);
                    }
                }
            }
            Set keySet = bestLocations.keySet();
            Iterator votesIterator = keySet.iterator();
            int mostVotes = 0;
            Integer locationIndex;
            while (votesIterator.hasNext())
            {
                locationIndex = (Integer)(votesIterator.next());
                int votes = (Integer) bestLocations.get(locationIndex);
                if (votes > mostVotes)
                {
                    nearestGroupMission = (location)missionLocations[locationIndex].clone();
                    mostVotes = votes;
                }
            }
            result = (location)nearestGroupMission.clone();
            scriptVars.put("nearestGroupMission", nearestGroupMission);
        }
        else
        {
            result = null;
        }
        return result;
    }
    public int missionLocationResponse(obj_id self, dictionary params) throws InterruptedException
    {
        if ((params != null) && (params.containsKey("requestMissionLocationsNumber")))
        {
            deltadictionary scriptVars = self.getScriptVars();
            if ((scriptVars != null) && (scriptVars.hasKey("requestMissionLocationsNumber")) && (params.getInt("requestMissionLocationsNumber") == scriptVars.getInt("requestMissionLocationsNumber")))
            {
                obj_id missionHolder = params.getObjId("sender");
                if (isIdValid(missionHolder))
                {
                    location senderLocation = params.getLocation("senderLocation");
                    if (senderLocation != null)
                    {
                        Vector memberLocations = scriptVars.getResizeableLocationArray("memberLocations");
                        if (memberLocations == null)
                        {
                            memberLocations = new Vector();
                        }
                        memberLocations.add(senderLocation);
                        scriptVars.put("memberLocations", memberLocations);
                        location[] missionLocation = params.getLocationArray("missionLocation");
                        if ((missionLocation != null) && (missionLocation.length > 0))
                        {
                            Vector missionLocations = scriptVars.getResizeableLocationArray("missionLocations");
                            if (missionLocations == null)
                            {
                                missionLocations = new Vector();
                            }
                            Collections.addAll(missionLocations, missionLocation);
                            scriptVars.put("missionLocations", missionLocations);
                            location nearestGroupMission = scriptVars.getLocation("nearestGroupMission");
                            if (nearestGroupMission == null)
                            {
                                nearestGroupMission = (location)missionLocation[0].clone();
                                scriptVars.put("nearestGroupMission", nearestGroupMission);
                            }
                        }
                        if (memberLocations.size() >= getPCGroupSize(self))
                        {
                            tellMembersAboutNearestGroupMission(self, calculateNearestGroupMission(self));
                        }
                    }
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    private void tellMembersAboutNearestGroupMission(obj_id self, location nearestGroupMission) throws InterruptedException
    {
        if (nearestGroupMission != null)
        {
            deltadictionary scriptVars = self.getScriptVars();
            if (scriptVars != null)
            {
                obj_id[] members = getGroupMemberIds(self);
                dictionary msgData = new dictionary();
                msgData.put("waypointLocation", nearestGroupMission);
                int sequence = scriptVars.getInt("updateSequence");
                sequence++;
                scriptVars.put("updateSequence", sequence);
                msgData.put("updateSequence", sequence);
                for (obj_id member : members) {
                    messageTo(member, "updateGroupWaypoint", msgData, 0, false);
                }
            }
        }
    }
    private void selectNearestGroupMission(obj_id self) throws InterruptedException
    {
        deltadictionary scriptVars = self.getScriptVars();
        if (scriptVars != null)
        {
            scriptVars.remove("memberLocations");
            scriptVars.remove("missionLocations");
            scriptVars.remove("nearestGroupMission");
            obj_id[] members = getGroupMemberIds(self);
            if (members != null)
            {
                int requestMissionLocationsNumber = 1;
                if (scriptVars.hasKey("requestMissionLocationsNumber"))
                {
                    requestMissionLocationsNumber = scriptVars.getInt("requestMissionLocationsNumber");
                    ++requestMissionLocationsNumber;
                }
                scriptVars.put("requestMissionLocationsNumber", requestMissionLocationsNumber);
                dictionary requestData = new dictionary();
                requestData.put("requestMissionLocationsNumber", requestMissionLocationsNumber);
                int memberIndex = 0;
                for (memberIndex = 0; memberIndex < members.length; ++memberIndex)
                {
                    messageTo(members[memberIndex], "requestMissionLocations", requestData, 0, false);
                }
            }
        }
    }
    public int recaclulateNearestGroupWaypoint(obj_id self, dictionary params) throws InterruptedException
    {
        selectNearestGroupMission(self);
        return SCRIPT_CONTINUE;
    }
    public int removeMissionLocation(obj_id self, dictionary params) throws InterruptedException
    {
        selectNearestGroupMission(self);
        return SCRIPT_CONTINUE;
    }
    public static obj_id[] getGroupMemberPlayers(obj_id groupId)
    {
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        //determine the players in the group
        ArrayList<obj_id> memberPlayerIds = new ArrayList<obj_id>();
        for (obj_id member : groupMembers) {
            if (isPlayer(member)) {
                memberPlayerIds.add(member);
            }
        }
        return memberPlayerIds.toArray(obj_id[]::new);
    }
    public static obj_id[] getReadyCheckNoneIds(obj_id groupId) throws InterruptedException {
        obj_id[] readyCheckResponsesNone = utils.getObjIdArrayScriptVar(groupId, "readyCheck.responses.none");
        if (readyCheckResponsesNone == null) {
            readyCheckResponsesNone = new obj_id[0];
        }
        return readyCheckResponsesNone;
    }
    public static obj_id[] getReadyCheckYesIds(obj_id groupId) throws InterruptedException {
        obj_id[] readyCheckResponsesYes = utils.getObjIdArrayScriptVar(groupId, "readyCheck.responses.yes");
        if (readyCheckResponsesYes == null) {
            readyCheckResponsesYes = new obj_id[0];
        }
        return readyCheckResponsesYes;
    }
    public static obj_id[] getReadyCheckNoIds(obj_id groupId) throws InterruptedException {
        obj_id[] readyCheckResponsesNo = utils.getObjIdArrayScriptVar(groupId, "readyCheck.responses.no");
        if (readyCheckResponsesNo == null) {
            readyCheckResponsesNo = new obj_id[0];
        }
        return readyCheckResponsesNo;
    }
    //helper method to clear scriptvars related to ready check from the group
    public static void clearReadyCheckVars(obj_id groupId) throws InterruptedException
    {
        utils.removeScriptVar(groupId, "readyCheck.responses.none");
        utils.removeScriptVar(groupId, "readyCheck.responses.yes");
        utils.removeScriptVar(groupId, "readyCheck.responses.no");
        utils.removeScriptVar(groupId, "activeCleanupId");
        utils.removeScriptVar(groupId, "readyCheckPerformer");
    }
    public static void setReadyCheckVars(obj_id groupId, obj_id[] none, obj_id[] yes, obj_id[] no, obj_id performer) throws InterruptedException
    {
        setReadyCheckResponseVars(groupId, none, yes, no);
        utils.setScriptVar(groupId, "readyCheckPerformer", performer);
    }
    public static void setReadyCheckResponseVars(obj_id groupId, obj_id[] none, obj_id[] yes, obj_id[] no) throws InterruptedException
    {
        utils.setScriptVar(groupId, "readyCheck.responses.none", none);
        utils.setScriptVar(groupId, "readyCheck.responses.yes", yes);
        utils.setScriptVar(groupId, "readyCheck.responses.no", no);
    }
    public static void createNewReadyCheck(obj_id creator) throws InterruptedException
    {
        //ensure the creator is grouped
        var groupId = getGroupObject(creator);
        if (groupId == null)
        {
            sendSystemMessage(creator, SID_READY_CHECK_MUST_BE_GROUPED);
            return;
        }

        //Keep Non-Group Leaders from Creating a new Ready Check: Remove to enable non-leaders to create ready checks
        obj_id leaderId = getGroupLeaderId(groupId);
        if (leaderId != creator) {
            sendSystemMessage(creator, SID_READY_CHECK_MUST_BE_LEADER);
            return;
        }

        //send the readyCheck.request to the group members
        obj_id[] memberPlayerIds = group_object.getGroupMemberPlayers(groupId);
        if (memberPlayerIds.length < 2) {
            sendSystemMessage(creator, SID_READY_CHECK_TWO_OR_MORE);
            return;
        }
        for (obj_id member : memberPlayerIds) {
            sendSystemMessage(member, SID_READY_CHECK_START);
            dictionary readyRequestParams = new dictionary();
            readyRequestParams.put("performer_id", creator);
            messageTo(member, "receiveReadyRequest", readyRequestParams, 1.0f, false);
        }

        //set the readyCheck.responses
        obj_id[] yes = new obj_id[0];
        obj_id[] no = new obj_id[0];

        group_object.setReadyCheckVars(groupId, memberPlayerIds, yes, no, creator);

        //display the current status page to the creator player
        player_utility.showReadyCheckStatusPage(creator);

        //assign an id to this cleanup. if a cleanup attempt is processed and a different active cleanup ID is present, that cleanup can be discarded
        int activeCleanupId = utils.getIntScriptVar(groupId, "activeCleanupId");
        if (activeCleanupId > 0) {
            activeCleanupId++;
        } else {
            activeCleanupId = 1;
        }
        utils.setScriptVar(groupId, "activeCleanupId", activeCleanupId);

        //send the message for cleanup at the fixed duration for ready check timeouts
        dictionary cleanupParams = new dictionary();
        cleanupParams.put("cleanup_id", activeCleanupId);
        messageTo(groupId, "cleanupReadyCheck", cleanupParams, 60.0f, false);
    }
    //response method to the messageTo invocation from members to performers informing the performer of their ready status
    public int readyCheckResponse(obj_id self, dictionary params) throws InterruptedException
    {
        sendSystemMessageTestingOnly(getGroupLeaderId(self), "Ready Check Begin: " + self);
        //extract the responding object ID from the params dictionary
        obj_id respondingId = params.getObjId("responding_id");
        if (respondingId == null) {
            return SCRIPT_CONTINUE;
        }

        //get the response lists
        obj_id[] readyCheckResponsesNone = getReadyCheckNoneIds(self);
        obj_id[] readyCheckResponsesYes = getReadyCheckYesIds(self);
        obj_id[] readyCheckResponsesNo = getReadyCheckNoIds(self);

        if (readyCheckResponsesNone.length == 0 && readyCheckResponsesYes.length == 0 && readyCheckResponsesNo.length == 0) {
            sendSystemMessage(respondingId, SID_READY_CHECK_RESPONSE_NO_CHECK);
            return SCRIPT_CONTINUE;
        }

        boolean ready = params.getBoolean("ready");
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(self, "readyCheckPerformer");
        sendSystemMessageTestingOnly(readyCheckPerformer, "About to remove");

        var newNone = collections.removeElement(readyCheckResponsesNone, respondingId);
        var newYes = collections.removeElement(readyCheckResponsesYes, respondingId);
        var newNo = collections.removeElement(readyCheckResponsesNo, respondingId);

        String notificationMessage = "";
        if (ready)
        {
            notificationMessage = getPlayerName(respondingId) + " is ready";
            newYes = collections.addElement(newYes, respondingId);
            sendSystemMessage(readyCheckPerformer, "Added to yes", "readyCheck");
        }
        else
        {
            notificationMessage = getPlayerName(respondingId) + " is not ready";
            newNo = collections.addElement(newNo, respondingId);
            sendSystemMessage(readyCheckPerformer, "Added to no", "readyCheck");
        }


        //save the scriptvar values
        setReadyCheckResponseVars(self, newNone, newYes, newNo);
        sendSystemMessageTestingOnly(readyCheckPerformer, "set vars on " + self);
        reloadGroupMemberReadyCheckPages(self, notificationMessage);
        sendSystemMessage(readyCheckPerformer, "Reloaded", "readyCheck");
        return SCRIPT_CONTINUE;
    }
    public static void reloadGroupMemberReadyCheckPages(obj_id groupId, String notificationMessage) throws InterruptedException
    {
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        //notify the group members of the ready check response
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        sendSystemMessageTestingOnly(readyCheckPerformer, "group members: " + groupMembers.length);
        for (obj_id member : groupMembers) {
            sendSystemMessage(member, notificationMessage, "readyCheck");
            player_utility.reloadSnapshotPageIfOpen(member);
        }

        //perform operations on the performer
        if (readyCheckPerformer == null) {
            return;
        }
        player_utility.reloadStatusPageIfOpen(readyCheckPerformer);
    }
    //handler for messageTo for cleaning up the active ready check upon timeout
    public int cleanupReadyCheck(obj_id self, dictionary params) throws InterruptedException
    {
        //kick out this cleanup attempt if the active cleanup ID is not this request
        int activeCleanupId = utils.getIntScriptVar(self, "activeCleanupId");
        if (params.getInt("cleanup_id") != activeCleanupId) {
            return SCRIPT_CONTINUE;
        }

        //ensure there is a ready check performer on the group
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(self, "readyCheckPerformer");
        if (readyCheckPerformer == null) {
            return SCRIPT_CONTINUE;
        }

        //send the summary message
        obj_id[] none = getReadyCheckNoneIds(self);
        obj_id[] yes = getReadyCheckYesIds(self);
        obj_id[] no = getReadyCheckNoIds(self);

        if (none.length == 0 && yes.length == 0 && no.length == 0) {
            return SCRIPT_CONTINUE;
        }
        String message = "Ready Check Results: " + yes.length + " Ready | " + no.length + " Not Ready | " + none.length + " No Response";
        obj_id[] memberPlayerIds = getGroupMemberPlayers(self);
        for (obj_id member : memberPlayerIds) {
            sendSystemMessage(member, message, "readyCheck");
            player_utility.closeReadyCheckSnapshotPage(member);
        }
        player_utility.closeReadyCheckStatusPage(readyCheckPerformer);
        clearReadyCheckVars(self);
        return SCRIPT_CONTINUE;
    }
    //handler for when members leave the group to ensure they are properly removed from the ready check
    public int leftGroupReadyCheck(obj_id self, dictionary params) throws InterruptedException
    {
        //extract the obj_id of the person who left the group
        obj_id objIdLeftGroup = params.getObjId("obj_id");
        if (objIdLeftGroup == null)
        {
            return SCRIPT_CONTINUE;
        }

        //check if the object id matches the performer of the ready check
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(self, "readyCheckPerformer");
        if (readyCheckPerformer == null)
        {
            //if no performer active, ready check is inactive
            return SCRIPT_CONTINUE;
        }

        //cancel the ready check if the performer of the ready check leaves the group
        if (readyCheckPerformer == objIdLeftGroup)
        {
            cancelReadyCheck(objIdLeftGroup);
            return SCRIPT_CONTINUE;
        }

        //a non-performer of the ready check has left the group, remove them from the response lists
        obj_id[] noneIds = getReadyCheckNoneIds(self);
        obj_id[] yesIds = getReadyCheckYesIds(self);
        obj_id[] noIds = getReadyCheckNoIds(self);

        //ensure some ready check responses are present
        if (noneIds.length == 0 && yesIds.length == 0 && noIds.length == 0) {
            return SCRIPT_CONTINUE;
        }

        //make sure they are fully removed from any list they are contained in
        noneIds = collections.removeElement(noneIds, objIdLeftGroup);
        yesIds = collections.removeElement(yesIds, objIdLeftGroup);
        noIds = collections.removeElement(noIds, objIdLeftGroup);

        String notificationMessage = getPlayerName(objIdLeftGroup) + " has been removed from the Ready Check";

        //save the scriptvar values
        setReadyCheckResponseVars(self, noneIds, yesIds, noIds);
        reloadGroupMemberReadyCheckPages(self, notificationMessage);
        return SCRIPT_CONTINUE;
    }
    public static void cancelReadyCheck(obj_id cancelPerformer) throws InterruptedException
    {
        //get the group of cancelPerformer
        obj_id groupId = getGroupObject(cancelPerformer);
        if (groupId == null) {
            return;
        }

        //ensure the ready check performer is performing the ready check cancellation
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        if (readyCheckPerformer == null) {
            sendSystemMessage(cancelPerformer, SID_READY_CHECK_RESPONSE_NO_CHECK);
            return;
        }

        //ensure the ready check performer is performing the cancellation
        if (readyCheckPerformer != cancelPerformer) {
            return;
        }

        clearReadyCheckVars(groupId);
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        var rescindParams = new dictionary();
        rescindParams.put("performer_id", cancelPerformer);
        for (obj_id member : groupMembers) {
            if (isPlayer(member)) {
                messageTo(member, "rescindReadyCheckRequest", rescindParams, 1.0f, false);
            }
        }
        sendSystemMessage(cancelPerformer, SID_READY_CHECK_CANCELLED);
    }
}
