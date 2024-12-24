package script.grouping;

import script.*;
import script.library.*;

import java.util.ArrayList;
import java.util.Arrays;

public class group_ready_check extends script.base_script
{
    public static final string_id SID_READY_CHECK_MUST_BE_GROUPED = new string_id("spam", "ready_check_must_be_grouped");
    public static final string_id SID_READY_CHECK_TWO_OR_MORE = new string_id("spam", "ready_check_two_or_more");
    public static final string_id SID_READY_CHECK_MUST_BE_GROUP_LEADER = new string_id("spam", "ready_check_must_be_group_leader");
    public group_ready_check()
    {
    }
    public int readyCheck(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        //close the existing page if it is already open
        if (sui.hasPid(self, "readyCheck"))
        {
            int pid = sui.getPid(self, "readyCheck");
            forceCloseSUIPage(pid);
            sui.removePid(self, "readyCheck");
        }

        //get the group members of self
        obj_id groupId = getGroupObject(self);
        //if there is no group ID, give an error message to the user
        if (groupId == null) {
            sendSystemMessage(self, SID_READY_CHECK_MUST_BE_GROUPED);
            return SCRIPT_CONTINUE;
        }

        //ensure the group leader is performing the ready check
        obj_id groupLeaderId = getGroupLeaderId(groupId);
        if (groupLeaderId == null) {
            return SCRIPT_CONTINUE;
        }
        if (groupLeaderId != self) {
            sendSystemMessage(self, SID_READY_CHECK_MUST_BE_GROUP_LEADER);
            return SCRIPT_CONTINUE;
        }

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

        //send the readyCheck.request to the group members
        for (obj_id member : memberPlayerIds) {
            dictionary readyRequestParams = new dictionary();
            readyRequestParams.put("leader_id", self);
            messageTo(member, "receiveReadyRequest", readyRequestParams, 1.0f, false);
        }

        //set the readyCheck.responses
        obj_id[] none = memberPlayerIds.toArray(obj_id[]::new);
        obj_id[] yes = new obj_id[] {};
        obj_id[] no = new obj_id[] {};

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
        }
        for (obj_id member : yes)
        {
            memberPlayersReady[i][0] = getPlayerName(member);
            memberPlayersReady[i][1] = "\\#3bcf00Ready";
        }
        for (obj_id member : no)
        {
            memberPlayersReady[i][0] = getPlayerName(member);
            memberPlayersReady[i][1] = "\\#eb1d0eNot Ready";
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
            case sui.BP_REVERT:
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
            messageTo(leaderId, "readyCheckResponse", params, 1.0f, false);
        }
        else
        {
            responseParams.put("ready", true);
            messageTo(leaderId, "readyCheckResponse", params, 1.0f, false);
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
            return SCRIPT_CONTINUE;
        }
        //get the list of people who have responded no to the ready check
        obj_id[] readyCheckResponsesNo = utils.getObjIdArrayObjVar(self, "readyCheck.responses.no");
        if (readyCheckResponsesNo == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure the responder is in the none section
        boolean fail = true;
        for (obj_id none : readyCheckResponsesNone) {
            if (none == respondingId) {
                fail = false;
                break;
            }
        }
        if (fail) {
            return SCRIPT_CONTINUE;
        }

        //ensure the responder does not appear in the Yes or No section already
        for (obj_id yes : readyCheckResponsesYes) {
            if (yes == respondingId) {
                return SCRIPT_CONTINUE;
            }
        }
        for (obj_id no : readyCheckResponsesNo) {
            if (no == respondingId) {
                return SCRIPT_CONTINUE;
            }
        }

        //create the list of people who have not yet responded to the ready check as the previous list's set without the responder
        ArrayList<obj_id> newNoneResponses = new ArrayList<>();
        for (obj_id member : readyCheckResponsesNone) {
            if (member != respondingId) {
                newNoneResponses.add(member);
            }
        }

        //create the lists to save as the ready check responses for Yes and No as the lists from previous, ready to add the new one from this response
        ArrayList<obj_id> newYesResponses = new ArrayList<>(Arrays.asList(readyCheckResponsesYes));
        ArrayList<obj_id> newNoResponses = new ArrayList<>(Arrays.asList(readyCheckResponsesNo));
        boolean ready = params.getBoolean("ready");
        if (ready)
        {
            newYesResponses.add(respondingId);
        }
        else
        {
            newNoResponses.add(respondingId);
        }

        //save the objvar values
        utils.setObjVar(self, "readyCheck.responses.none", newNoneResponses.toArray(obj_id[]::new));
        utils.setObjVar(self, "readyCheck.responses.yes", newYesResponses.toArray(obj_id[]::new));
        utils.setObjVar(self, "readyCheck.responses.no", newNoResponses.toArray(obj_id[]::new));
        reloadReadyCheckPage(self);
        return SCRIPT_CONTINUE;
    }
    //response method to the messageTo invocation from members to leader informing the leader of their ready status
    public void reloadReadyCheckPage(obj_id self) throws InterruptedException
    {
        //get the list of people who have yet to respond to the ready check
        obj_id[] none = utils.getObjIdArrayObjVar(self, "readyCheck.responses.none");
        if (none == null) {
            return;
        }
        //get the list of people who have responded yes to ready check
        obj_id[] yes = utils.getObjIdArrayObjVar(self, "readyCheck.responses.yes");
        if (yes == null) {
            return;
        }
        //get the list of people who have responded no to the ready check
        obj_id[] no = utils.getObjIdArrayObjVar(self, "readyCheck.responses.no");
        if (no == null) {
            return;
        }

        showReadyCheckStatusPage(none, yes, no, self);
    }
}