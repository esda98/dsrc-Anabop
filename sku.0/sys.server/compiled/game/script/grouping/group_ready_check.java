package script.grouping;

import script.*;
import script.library.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class group_ready_check extends script.base_script
{
    public static final string_id SID_READY_CHECK_MUST_BE_GROUPED = new string_id("spam", "ready_check_must_be_grouped");
    public static final string_id SID_READY_CHECK_TWO_OR_MORE = new string_id("spam", "ready_check_two_or_more");
    public static final string_id SID_READY_CHECK_MUST_BE_GROUP_LEADER = new string_id("spam", "ready_check_must_be_group_leader");
    public static final string_id SID_READY_CHECK_LEADER_START = new string_id("spam", "ready_check_leader_start");
    public group_ready_check()
    {
    }
    public int readyCheck(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        //get the group members of self
        obj_id groupId = getGroupObject(self);
        //if there is no group ID, give an error message to the user
        if (groupId == null) {
            sendSystemMessage(self, SID_READY_CHECK_MUST_BE_GROUPED);
            return SCRIPT_CONTINUE;
        }

        //get the group leader
        obj_id groupLeaderId = getGroupLeaderId(groupId);
        if (groupLeaderId == null) {
            return SCRIPT_CONTINUE;
        }

        //check if this is a ready check response command
        if (!params.isEmpty()) {
            dictionary responseParams = new dictionary();
            responseParams.put("responding_id", self);
            if (params.equals("yes")) {
                //send yes to leader
                responseParams.put("ready", true);
            } else if (params.equals("no")) {
                //send no to leader
                responseParams.put("ready", false);
            } else {
                //Unknown parameter
                return SCRIPT_CONTINUE;
            }
            messageTo(groupLeaderId, "readyCheckResponse", responseParams, 1.0f, false);
            return SCRIPT_CONTINUE;
        }

        //make sure the group leader is performing the ready check
        if (groupLeaderId != self) {
            sendSystemMessage(self, SID_READY_CHECK_MUST_BE_GROUP_LEADER);
            return SCRIPT_CONTINUE;
        }

        closeReadyCheckStatusPage(self);

        obj_id[] groupMembers = getGroupMemberIds(groupId);
        //determine the players in the group
        ArrayList<obj_id> memberPlayerIds = new ArrayList<obj_id>();
        for (obj_id member : groupMembers) {
            if (isPlayer(member)) {
                memberPlayerIds.add(member);
            }
        }

        //ensure there are at least two players in the group to perform the ready check
        if (memberPlayerIds.size() < 2) {
            sendSystemMessage(self, SID_READY_CHECK_TWO_OR_MORE);
            return SCRIPT_CONTINUE;
        }

        //notify the members of the Ready Check initiation
        for (obj_id member : groupMembers) {
            sendSystemMessage(member, SID_READY_CHECK_LEADER_START);
        }

        //send the readyCheck.request to the group members
        for (obj_id member : memberPlayerIds) {
            dictionary readyRequestParams = new dictionary();
            readyRequestParams.put("leader_id", self);
            messageTo(member, "receiveReadyRequest", readyRequestParams, 1.0f, false);
        }

        //set the readyCheck.responses
        obj_id[] none = memberPlayerIds.toArray(obj_id[]::new);
        obj_id[] yes = new obj_id[0];
        obj_id[] no = new obj_id[0];

        setReadyCheckResponseObjVars(none, yes, no, self);

        //display the current status
        showReadyCheckStatusPage(none, yes, no, self);
        return SCRIPT_CONTINUE;
    }
    public void cancelReadyCheck(obj_id host)
    {
        //get the group of host
        obj_id groupId = getGroupObject(host);
        if (groupId == null) {
            return;
        }

        //ensure the group leader is performing the ready check cancellation
        obj_id groupLeaderId = getGroupLeaderId(groupId);
        if (groupLeaderId == null) {
            return;
        }
        if (groupLeaderId != host) {
            return;
        }

        obj_id[] groupMembers = getGroupMemberIds(groupId);
        var rescindParams = new dictionary();
        rescindParams.put("leader_id", host);
        for (obj_id member : groupMembers) {
            if (isPlayer(member)) {
                messageTo(member, "rescindReadyCheckRequest", rescindParams, 1.0f, false);
            }
        }
    }
    public void closeReadyCheckStatusPage(obj_id host) throws InterruptedException
    {
        //close the existing page if it is already open
        if (sui.hasPid(host, "readyCheck"))
        {
            int pid = sui.getPid(host, "readyCheck");
            forceCloseSUIPage(pid);
            sui.removePid(host, "readyCheck");
        }
    }
    //handler for the status page of the Ready Check being performed
    public int rescindReadyCheckRequest(obj_id self, dictionary params) throws InterruptedException
    {
        //get the leader of the group cancelling the ready request
        obj_id leaderId = params.getObjId("leader_id");
        if (leaderId == null)
        {
            return SCRIPT_CONTINUE;
        }

        //ensure the user is grouped when receiving a ready request
        obj_id groupId = getGroupObject(self);
        if (groupId == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure there is a group leader of self's group
        obj_id selfGroupLeaderId = getGroupLeaderId(groupId);
        if (selfGroupLeaderId == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure self's group leader is the same as the group leader requesting the ready check
        if (selfGroupLeaderId != leaderId) {
            return SCRIPT_CONTINUE;
        }

        closeReadyCheckRequestPage(self);
        return SCRIPT_CONTINUE;
    }
    public void clearReadyCheckResponseObjVars(obj_id host)
    {
        obj_id[] none = new obj_id[] {};
        obj_id[] yes = new obj_id[] {};
        obj_id[] no = new obj_id[] {};

        setReadyCheckResponseObjVars(none, yes, no, host);
    }
    public void setReadyCheckResponseObjVars(obj_id[] none, obj_id[] yes, obj_id[] no, obj_id host)
    {
        utils.setObjVar(host, "readyCheck.responses.none", none);
        utils.setObjVar(host, "readyCheck.responses.yes", yes);
        utils.setObjVar(host, "readyCheck.responses.no", no);
    }
    public void showReadyCheckStatusPage(obj_id[] none, obj_id[] yes, obj_id[] no, obj_id host) throws InterruptedException
    {
        //build the display table
        String[][] memberPlayersReady = new String[none.length + yes.length + no.length][2];
        int i = 0;
        for (obj_id member : none)
        {
            memberPlayersReady[i][0] = getPlayerName(member);
            memberPlayersReady[i][1] = "\\#ff913dNo Response";
            i++;
        }
        for (obj_id member : yes)
        {
            memberPlayersReady[i][0] = getPlayerName(member);
            memberPlayersReady[i][1] = "\\#3bcf00Ready";
            i++;
        }
        for (obj_id member : no)
        {
            memberPlayersReady[i][0] = getPlayerName(member);
            memberPlayersReady[i][1] = "\\#eb1d0eNot Ready";
            i++;
        }

        //establish constants of the window layout
        String prompt = "@spam:ready_check_table_prompt";
        String[] table_titles =
        {
            "@spam:table_title_player",
            "@spam:table_title_status",
        };
        String[] table_types =
        {
            "text",
            "text"
        };

        //sort the display list by the names of the toons
        Arrays.sort(memberPlayersReady, (row1, row2) -> row1[0].compareToIgnoreCase(row2[0]));

        int pid = sui.tableRowMajor(host, host, sui.REFRESH_CANCEL, "Ready Check", "handleReadyCheckPageResponse", prompt, table_titles, table_types, memberPlayersReady, false);
        sui.setPid(host, pid, "readyCheck");
    }
    //handler for the status page of the Ready Check being performed
    public int handleReadyCheckPageResponse(obj_id self, dictionary params) throws InterruptedException
    {
        //ensure there are some values in the params dictionary
        if (params == null || params.isEmpty())
        {
            clearReadyCheckResponseObjVars(self);
            return SCRIPT_CONTINUE;
        }

        int bp = sui.getIntButtonPressed(params);
        switch (bp) {
            case sui.BP_CANCEL:
                return SCRIPT_CONTINUE;
            case sui.BP_OK:
                closeReadyCheckRequestPage(self);
                reloadReadyCheckPage(self);
                return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
    //response to invoke messageTo for group members to be sent ready checks
    public int receiveReadyRequest(obj_id self, dictionary params) throws InterruptedException
    {
        //get the leader of the group sending the ready request
        obj_id leaderId = params.getObjId("leader_id");
        if (leaderId == null)
        {
            return SCRIPT_CONTINUE;
        }

        //ensure the user is grouped when receiving a ready request
        obj_id groupId = getGroupObject(self);
        if (groupId == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure there is a group leader of self's group
        obj_id selfGroupLeaderId = getGroupLeaderId(groupId);
        if (selfGroupLeaderId == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure self's group leader is the same as the group leader requesting the ready check
        if (selfGroupLeaderId != leaderId) {
            return SCRIPT_CONTINUE;
        }

        closeReadyCheckRequestPage(self);

        //display the ready check request
        play2dNonLoopingSound(self, "sound/ui_incoming_im.snd");
        int pid = sui.msgbox(self, self, "@spam:ready_check_request_prompt", sui.YES_NO, "@spam:ready_check_request_title", sui.MSG_QUESTION, "onReadyCheckRequestResponse");
        sui.setPid(self, pid, "readyCheck.request");
        return SCRIPT_CONTINUE;
    }
    public void closeReadyCheckRequestPage(obj_id host) throws InterruptedException
    {
        //close the existing readyCheck.request if it is already open
        if (sui.hasPid(host, "readyCheck.request"))
        {
            int pid = sui.getPid(host, "readyCheck.request");
            forceCloseSUIPage(pid);
            sui.removePid(host, "readyCheck.request");
        }
    }
    //response method to SUI MessageBox asking if the group member is ready
    public int onReadyCheckRequestResponse(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);

        //ensure the user is grouped when performing response
        obj_id groupId = getGroupObject(self);
        if (groupId == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure there is a group leader to send the response to
        obj_id leaderId = getGroupLeaderId(groupId);
        if (leaderId == null) {
            return SCRIPT_CONTINUE;
        }

        dictionary responseParams = new dictionary();
        responseParams.put("responding_id", self);
        if (btn == sui.BP_CANCEL)
        {
            responseParams.put("ready", false);
            messageTo(leaderId, "readyCheckResponse", responseParams, 1.0f, false);
        }
        else
        {
            responseParams.put("ready", true);
            messageTo(leaderId, "readyCheckResponse", responseParams, 1.0f, false);
        }
        return SCRIPT_CONTINUE;
    }
    //response method to the messageTo invocation from members to leader informing the leader of their ready status
    public int readyCheckResponse(obj_id self, dictionary params) throws InterruptedException
    {
        //extract the responding object ID from the params dictionary
        obj_id respondingId = params.getObjId("responding_id");
        if (respondingId == null) {
            return SCRIPT_CONTINUE;
        }

        //get the list of people who have yet to respond to the ready check
        obj_id[] readyCheckResponsesNone = utils.getObjIdArrayObjVar(self, "readyCheck.responses.none");
        if (readyCheckResponsesNone == null) {
            return SCRIPT_CONTINUE;
        }

        //get the list of people who have responded yes to ready check
        obj_id[] readyCheckResponsesYes = utils.getObjIdArrayObjVar(self, "readyCheck.responses.yes");
        if (readyCheckResponsesYes == null) {
            readyCheckResponsesYes = new obj_id[0];
        }

        //get the list of people who have responded no to the ready check
        obj_id[] readyCheckResponsesNo = utils.getObjIdArrayObjVar(self, "readyCheck.responses.no");
        if (readyCheckResponsesNo == null) {
            readyCheckResponsesNo = new obj_id[0];
        }

        //create the list of people who have not yet responded to the ready check as the previous list's set without the responder
        //removes the responder from the list if they previously were in it
        ArrayList<obj_id> newNoneResponses = new ArrayList<>();
        for (obj_id member : readyCheckResponsesNone) {
            if (member != respondingId) {
                newNoneResponses.add(member);
            }
        }

        //create the yes list as the previous list's set without the responder
        //removes the responder from the list if they previously were in it
        ArrayList<obj_id> newYesResponses = new ArrayList<>();
        for (obj_id member : readyCheckResponsesYes) {
            if (member != respondingId) {
                newYesResponses.add(member);
            }
        }

        //create the yes list as the previous list's set without the responder
        //removes the responder from the list if they previously were in it
        ArrayList<obj_id> newNoResponses = new ArrayList<>();
        for (obj_id member : readyCheckResponsesNo) {
            if (member != respondingId) {
                newNoResponses.add(member);
            }
        }

        boolean ready = params.getBoolean("ready");
        String notificationMessage = "";
        if (ready)
        {
            notificationMessage = getPlayerName(respondingId) + " is ready";
            newYesResponses.add(respondingId);
        }
        else
        {
            notificationMessage = getPlayerName(respondingId) + " is not ready";
            newNoResponses.add(respondingId);
        }

        //save the objvar values
        utils.setObjVar(self, "readyCheck.responses.none", newNoneResponses.toArray(obj_id[]::new));
        utils.setObjVar(self, "readyCheck.responses.yes", newYesResponses.toArray(obj_id[]::new));
        utils.setObjVar(self, "readyCheck.responses.no", newNoResponses.toArray(obj_id[]::new));

        //notify the group members of the ready check response
        obj_id groupId = getGroupObject(self);
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        for (obj_id member : groupMembers) {
            sendSystemMessage(member, notificationMessage, "readyCheck");
        }

        closeReadyCheckStatusPage(self);
        reloadReadyCheckPage(self);

        return SCRIPT_CONTINUE;
    }
    //response method to the messageTo invocation from members to leader informing the leader of their ready status
    public void reloadReadyCheckPage(obj_id self) throws InterruptedException
    {
        //get the list of people who have yet to respond to the ready check
        obj_id[] none = utils.getObjIdArrayObjVar(self, "readyCheck.responses.none");
        if (none == null) {
            none = new obj_id[0];
        }
        //get the list of people who have responded yes to ready check
        obj_id[] yes = utils.getObjIdArrayObjVar(self, "readyCheck.responses.yes");
        if (yes == null) {
            yes = new obj_id[0];
        }
        //get the list of people who have responded no to the ready check
        obj_id[] no = utils.getObjIdArrayObjVar(self, "readyCheck.responses.no");
        if (no == null) {
            no = new obj_id[0];
        }

        showReadyCheckStatusPage(none, yes, no, self);
    }
}