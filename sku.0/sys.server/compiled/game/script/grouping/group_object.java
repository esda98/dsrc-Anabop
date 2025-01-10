package script.grouping;

import script.*;
import script.library.group;
import script.library.utils;

import java.util.*;

public class group_object extends script.base_script
{
    public static final string_id SID_READY_CHECK_RESPONSE_NO_CHECK = new string_id("spam", "ready_check_response_no_check");

    public static final string_id SID_READY_CHECK_JOINED = new string_id("spam", "ready_check_joined");

    public static final String VAR_READY_CHECK_PERFORMER = "readyCheckPerformer";

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
    //response method to the messageTo invocation from members to performers informing the performer of their ready status
    public int readyCheckResponse(obj_id self, dictionary params) throws InterruptedException
    {
        //extract the responding object ID from the params dictionary
        obj_id respondingId = params.getObjId("responding_id");
        if (respondingId == null) {
            return SCRIPT_CONTINUE;
        }

        //get the response lists
        obj_id[] readyCheckResponsesNone = group.getReadyCheckNoneIds(self);
        obj_id[] readyCheckResponsesYes = group.getReadyCheckYesIds(self);
        obj_id[] readyCheckResponsesNo = group.getReadyCheckNoIds(self);

        if (readyCheckResponsesNone.length == 0 && readyCheckResponsesYes.length == 0 && readyCheckResponsesNo.length == 0) {
            utils.sendPlayerSystemMessage(respondingId, SID_READY_CHECK_RESPONSE_NO_CHECK);
            return SCRIPT_CONTINUE;
        }

        boolean ready = params.getBoolean("ready");
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(self, VAR_READY_CHECK_PERFORMER);

        obj_id[] newNone = collections.removeElement(readyCheckResponsesNone, respondingId);
        obj_id[] newYes = collections.removeElement(readyCheckResponsesYes, respondingId);
        obj_id[] newNo = collections.removeElement(readyCheckResponsesNo, respondingId);

        String notificationMessage = "";
        if (ready)
        {
            notificationMessage = getPlayerName(respondingId) + " is ready";
            newYes = collections.addElement(newYes, respondingId);
        }
        else
        {
            notificationMessage = getPlayerName(respondingId) + " is not ready";
            newNo = collections.addElement(newNo, respondingId);
        }

        //save the scriptvar values
        group.setReadyCheckResponseVars(self, newNone, newYes, newNo);
        group.reloadGroupMemberReadyCheckPages(self, notificationMessage);

        if (newNone.length == 0 && newNo.length == 0 && newYes.length > 0)
        {
            int activeCleanupId = utils.getIntScriptVar(self, "activeCleanupId");
            dictionary cleanupParams = new dictionary();
            cleanupParams.put("cleanup_id", activeCleanupId);
            cleanupParams.put("all_yes", true);
            messageTo(self, "cleanupReadyCheck", cleanupParams, 2, false);
        }

        return SCRIPT_CONTINUE;
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
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(self, VAR_READY_CHECK_PERFORMER);
        if (readyCheckPerformer == null) {
            return SCRIPT_CONTINUE;
        }

        //send the summary message
        obj_id[] none = group.getReadyCheckNoneIds(self);
        obj_id[] yes = group.getReadyCheckYesIds(self);
        obj_id[] no = group.getReadyCheckNoIds(self);

        if (none.length == 0 && yes.length == 0 && no.length == 0) {
            return SCRIPT_CONTINUE;
        }
        String message = "Ready Check Results: ";
        if (yes.length > 0 && no.length == 0 && none.length == 0)
        {
            message += "All (" + yes.length + "/" + yes.length + ") Members Are Ready";
        }
        else if (yes.length == 0 && none.length == 0 && no.length > 0)
        {
            message += "Nobody is Ready";
        }
        else
        {
            message += yes.length + " Ready | " + no.length + " Not Ready | " + none.length + " No Response";
        }
        obj_id[] memberPlayerIds = utils.getGroupMemberPlayers(self);
        for (obj_id member : memberPlayerIds) {
            utils.sendPlayerSystemMessage(member, message, "readyCheck");
            utils.sendCloseReadyCheckSnapshotPage(member);
            utils.sendCloseReadyCheckRequestPage(member);
        }
        utils.sendCloseReadyCheckStatusPage(readyCheckPerformer);
        group.clearReadyCheckVars(self);
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

        obj_id groupId = params.getObjId("group_id");
        if (groupId == null)
        {
            return SCRIPT_CONTINUE;
        }

        //check if the object id matches the performer of the ready check
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, VAR_READY_CHECK_PERFORMER);
        if (readyCheckPerformer == null)
        {
            //if no performer active, ready check is inactive
            return SCRIPT_CONTINUE;
        }

        //cancel the ready check if the performer of the ready check leaves the group
        if (readyCheckPerformer == objIdLeftGroup)
        {
            group.cancelGroupReadyCheck(groupId, objIdLeftGroup);
            return SCRIPT_CONTINUE;
        }

        //a non-performer of the ready check has left the group, remove them from the response lists
        obj_id[] noneIds = group.getReadyCheckNoneIds(groupId);
        obj_id[] yesIds = group.getReadyCheckYesIds(groupId);
        obj_id[] noIds = group.getReadyCheckNoIds(groupId);

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
        group.setReadyCheckResponseVars(self, noneIds, yesIds, noIds);
        group.reloadGroupMemberReadyCheckPages(self, notificationMessage);

        //close the windows on the person leaving
        utils.sendCloseReadyCheckSnapshotPage(objIdLeftGroup);
        utils.sendCloseReadyCheckStatusPage(objIdLeftGroup);
        utils.sendCloseReadyCheckRequestPage(objIdLeftGroup);

        return SCRIPT_CONTINUE;
    }
    //handler for when members join the group to ensure they are properly added to the ready check
    public int addGroupReadyCheck(obj_id self, dictionary params) throws InterruptedException
    {
        //extract the obj_id of the person who left the group
        obj_id objIdAddGroup = params.getObjId("obj_id");
        if (objIdAddGroup == null)
        {
            return SCRIPT_CONTINUE;
        }

        obj_id groupId = params.getObjId("group_id");
        if (groupId == null)
        {
            return SCRIPT_CONTINUE;
        }

        //check if the object id matches the performer of the ready check
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, VAR_READY_CHECK_PERFORMER);
        if (readyCheckPerformer == null)
        {
            //if no performer active, ready check is inactive
            return SCRIPT_CONTINUE;
        }

        //a non-performer of the ready check has left the group, remove them from the response lists
        obj_id[] noneIds = group.getReadyCheckNoneIds(groupId);
        obj_id[] yesIds = group.getReadyCheckYesIds(groupId);
        obj_id[] noIds = group.getReadyCheckNoIds(groupId);

        noneIds = collections.addElement(noneIds, objIdAddGroup);

        String notificationMessage = getPlayerName(objIdAddGroup) + " has been added to the Ready Check";

        //save the scriptvar values
        group.setReadyCheckResponseVars(self, noneIds, yesIds, noIds);
        group.reloadGroupMemberReadyCheckPages(self, notificationMessage);

        //display the ready check prompt for the member joining
        utils.sendPlayerSystemMessage(objIdAddGroup, SID_READY_CHECK_JOINED);
        dictionary readyRequestParams = new dictionary();
        readyRequestParams.put("performer_id", readyCheckPerformer);
        messageTo(objIdAddGroup, "receiveReadyRequest", readyRequestParams, 1.0f, false);

        return SCRIPT_CONTINUE;
    }

}
