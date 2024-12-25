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
    public static final string_id SID_READY_CHECK_LEADER_START = new string_id("spam", "ready_check_leader_start");
    public static final string_id SID_READY_CHECK_CANCEL_MUST_BE_LEADER = new string_id("spam", "ready_check_cancel_must_be_leader");
    public static final string_id SID_READY_CHECK_RESPONSE_NO_CHECK = new string_id("spam", "ready_check_response_no_check");
    public static final string_id SID_READY_CHECK_CANCELLED = new string_id("spam", "ready_check_cancelled");
    public static final string_id SID_READY_CHECK_DUPLICATE_YES = new string_id("spam", "ready_check_duplicate_yes");
    public static final string_id SID_READY_CHECK_DUPLICATE_NO = new string_id("spam", "ready_check_duplicate_no");
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
        if ((params.equals("yes") || params.equals("y"))
            || (params.equals("no") || params.equals("n"))) {
            dictionary responseParams = new dictionary();
            responseParams.put("responding_id", self);
            responseParams.put("ready", params.equals("yes") || params.equals("y"));
            messageTo(groupLeaderId, "readyCheckResponse", responseParams, 1.0f, false);
            return SCRIPT_CONTINUE;
        }

        //make sure the group leader is performing the ready check
        if (groupLeaderId != self) {
            snapshotReadyCheckResponseScriptVars(groupLeaderId, self);
            showReadyCheckSnapshotPage(self);
            return SCRIPT_CONTINUE;
        }

        closeReadyCheckStatusPage(self);

        //ensure there are at least two players in the group to perform the ready check
        obj_id[] memberPlayerIds = getGroupMemberPlayers(groupId);
        if (memberPlayerIds.length < 2) {
            sendSystemMessage(self, SID_READY_CHECK_TWO_OR_MORE);
            return SCRIPT_CONTINUE;
        }

        if (params.equals("new")) {
            createNewReadyCheck(self);
        } else if (params.equals("cancel")) {
            cancelReadyCheck(self);
        } else {
            //get the response lists
            obj_id[] none = getNoneResponseIds(self);
            obj_id[] yes = getYesResponseIds(self);
            obj_id[] no = getNoResponseIds(self);

            //display the current status
            showReadyCheckStatusPage(none, yes, no, self);
        }


        return SCRIPT_CONTINUE;
    }
    private obj_id[] getGroupMemberPlayers(obj_id groupId)
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
    public void createNewReadyCheck(obj_id self) throws InterruptedException
    {
        //send the readyCheck.request to the group members
        obj_id[] memberPlayerIds = getGroupMemberPlayers(getGroupObject(self));
        if (memberPlayerIds.length < 2) {
            sendSystemMessage(self, SID_READY_CHECK_TWO_OR_MORE);
            return;
        }
        for (obj_id member : memberPlayerIds) {
            sendSystemMessage(member, SID_READY_CHECK_LEADER_START);
            dictionary readyRequestParams = new dictionary();
            readyRequestParams.put("leader_id", self);
            messageTo(member, "receiveReadyRequest", readyRequestParams, 1.0f, false);
        }

        //set the readyCheck.responses
        obj_id[] yes = new obj_id[0];
        obj_id[] no = new obj_id[0];

        setReadyCheckResponseScriptVars(memberPlayerIds, yes, no, self);

        //display the current status
        showReadyCheckStatusPage(memberPlayerIds, yes, no, self);

        messageTo(self, "cleanupReadyCheck", new dictionary(), 60.0f, false);
    }
    //response to invoke messageTo for group members to be sent ready checks
    public int cleanupReadyCheck(obj_id self, dictionary params) throws InterruptedException
    {
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

        //ensure self's group leader is the same as self
        if (selfGroupLeaderId != self) {
            return SCRIPT_CONTINUE;
        }

        //send the summary message
        obj_id[] none = getNoneResponseIds(self);
        obj_id[] yes = getYesResponseIds(self);
        obj_id[] no = getNoResponseIds(self);

        if (none.length == 0 && yes.length == 0 && no.length == 0) {
            return SCRIPT_CONTINUE;
        }
        String message = "Ready Check Results: " + yes.length + " Ready | " + no.length + " Not Ready | " + none.length + " No Response";
        obj_id[] memberPlayerIds = getGroupMemberPlayers(getGroupObject(self));
        for (obj_id member : memberPlayerIds) {
            sendSystemMessage(member, message, "readyCheck");
            clearSnapshotScriptVars(member);
        }
        closeReadyCheckStatusPage(self);
        clearReadyCheckResponseScriptVars(self);
        return SCRIPT_CONTINUE;
    }
    public void cancelReadyCheck(obj_id host) throws InterruptedException
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
            sendSystemMessage(host, SID_READY_CHECK_CANCEL_MUST_BE_LEADER);
            return;
        }
        clearReadyCheckResponseScriptVars(host);
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        var rescindParams = new dictionary();
        rescindParams.put("leader_id", host);
        for (obj_id member : groupMembers) {
            if (isPlayer(member)) {
                messageTo(member, "rescindReadyCheckRequest", rescindParams, 1.0f, false);
            }
        }
        sendSystemMessage(host, SID_READY_CHECK_CANCELLED);
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
    public void closeReadyCheckSnapshotPage(obj_id host) throws InterruptedException
    {
        //close the existing page if it is already open
        if (sui.hasPid(host, "readyCheck.snapshot"))
        {
            int pid = sui.getPid(host, "readyCheck.snapshot");
            forceCloseSUIPage(pid);
            sui.removePid(host, "readyCheck.snapshot");
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
    public void clearReadyCheckResponseScriptVars(obj_id host) throws InterruptedException
    {
        obj_id[] none = new obj_id[] {};
        obj_id[] yes = new obj_id[] {};
        obj_id[] no = new obj_id[] {};

        setReadyCheckResponseScriptVars(none, yes, no, host);
    }
    public void setReadyCheckResponseScriptVars(obj_id[] none, obj_id[] yes, obj_id[] no, obj_id host) throws InterruptedException
    {
        utils.setScriptVar(host, "readyCheck.responses.none", none);
        utils.setScriptVar(host, "readyCheck.responses.yes", yes);
        utils.setScriptVar(host, "readyCheck.responses.no", no);
    }
    public void snapshotReadyCheckResponseScriptVars(obj_id host, obj_id copyDestination) throws InterruptedException
    {
        obj_id[] none = getNoneResponseIds(host);
        obj_id[] yes = getYesResponseIds(host);
        obj_id[] no = getNoResponseIds(host);

        utils.setScriptVar(copyDestination, "readyCheck.snapshot.none", none);
        utils.setScriptVar(copyDestination, "readyCheck.snapshot.yes", yes);
        utils.setScriptVar(copyDestination, "readyCheck.snapshot.no", no);
        utils.setScriptVar(copyDestination, "readyCheck.snapshot.time", getCalendarTime());
    }
    public void clearSnapshotScriptVars(obj_id host) throws InterruptedException
    {
        utils.setScriptVar(host, "readyCheck.snapshot.none",  new obj_id[] {});
        utils.setScriptVar(host, "readyCheck.snapshot.yes",  new obj_id[] {});
        utils.setScriptVar(host, "readyCheck.snapshot.no",  new obj_id[] {});
        utils.setScriptVar(host, "readyCheck.snapshot.time", 0);
    }
    public void showReadyCheckStatusPage(obj_id[] none, obj_id[] yes, obj_id[] no, obj_id host) throws InterruptedException
    {
        if (none.length == 0 && yes.length == 0 && no.length == 0) {
            closeReadyCheckCreateNewPage(host);
            int pid = sui.msgbox(host, host, "@spam:ready_check_create_new_prompt", sui.YES_NO, "@spam:ready_check_create_new_title", sui.MSG_QUESTION, "onReadyCheckCreateNewResponse");
            sui.setPid(host, pid, "readyCheck.createNew");
            return;
        }

        String[][] memberPlayersReady = buildReadyCheckTable(none, yes, no);

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
        closeReadyCheckStatusPage(host);
        int pid = sui.tableRowMajor(host, host, sui.REFRESH_CANCEL, "Ready Check", "handleReadyCheckPageResponse", prompt, table_titles, table_types, memberPlayersReady, false);
        sui.setPid(host, pid, "readyCheck");
    }
    public void showReadyCheckSnapshotPage(obj_id host) throws InterruptedException
    {
        obj_id[] none = utils.getObjIdArrayScriptVar(host, "readyCheck.snapshot.none");
        if (none == null) {
            none = new obj_id[0];
        }

        obj_id[] yes = utils.getObjIdArrayScriptVar(host, "readyCheck.snapshot.yes");
        if (yes == null) {
            yes = new obj_id[0];
        }

        obj_id[] no = utils.getObjIdArrayScriptVar(host, "readyCheck.snapshot.no");
        if (no == null) {
            no = new obj_id[0];
        }

        if (none.length == 0 && yes.length == 0 && no.length == 0) {
            closeReadyCheckNoSnapshotPage(host);
            int pid = sui.msgbox(host, host, "@spam:ready_check_no_snapshot_prompt", sui.OK_ONLY, "@spam:ready_check_no_snapshot_title", sui.MSG_QUESTION, "onReadyCheckNoSnapshotResponse");
            sui.setPid(host, pid, "readyCheck.noSnapshot");
            return;
        }

        int snapshotTime = utils.getIntScriptVar(host, "readyCheck.snapshot.time");

        String[][] memberPlayersReady = buildReadyCheckTable(none, yes, no);

        //establish constants of the window layout
        String prompt = "Ready Check as of " + getCalendarTimeStringLocal(snapshotTime);
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
        closeReadyCheckSnapshotPage(host);
        int pid = sui.tableRowMajor(host, host, sui.REFRESH_ONLY, "Ready Check Snapshot", "handleReadyCheckSnapshotPageResponse", prompt, table_titles, table_types, memberPlayersReady, false);
        sui.setPid(host, pid, "readyCheck.snapshot");
    }
    //response method to Ready Check Snapshot SUI table view
    public int handleReadyCheckSnapshotPageResponse(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id groupId = getGroupObject(self);
        if (groupId == null) {
            return SCRIPT_CONTINUE;
        }
        obj_id groupLeaderId = getGroupLeaderId(groupId);

        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_OK)
        {
            snapshotReadyCheckResponseScriptVars(groupLeaderId, self);
            showReadyCheckSnapshotPage(self);
        }
        return SCRIPT_CONTINUE;
    }
    private String[][] buildReadyCheckTable(obj_id[] none, obj_id[] yes, obj_id[] no) {
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
        return memberPlayersReady;
    }
    //response method to SUI MessageBox asking if the group leader would like to create a new ready check
    public int onReadyCheckCreateNewResponse(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_OK)
        {
            createNewReadyCheck(self);
        }
        return SCRIPT_CONTINUE;
    }
    //response method to SUI MessageBox informing a non-leader member there is no snapshot of a ready check to show
    public int onReadyCheckNoSnapshotResponse(obj_id self, dictionary params) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    //handler for the status page of the Ready Check being performed
    public int handleReadyCheckPageResponse(obj_id self, dictionary params) throws InterruptedException
    {
        //ensure there are some values in the params dictionary
        if (params == null || params.isEmpty())
        {
            clearReadyCheckResponseScriptVars(self);
            return SCRIPT_CONTINUE;
        }

        int bp = sui.getIntButtonPressed(params);
        switch (bp) {
            case sui.BP_CANCEL:
                cancelReadyCheck(self);
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
    public void closeReadyCheckCreateNewPage(obj_id host) throws InterruptedException
    {
        //close the existing readyCheck.request if it is already open
        if (sui.hasPid(host, "readyCheck.createNew"))
        {
            int pid = sui.getPid(host, "readyCheck.createNew");
            forceCloseSUIPage(pid);
            sui.removePid(host, "readyCheck.createNew");
        }
    }
    public void closeReadyCheckNoSnapshotPage(obj_id host) throws InterruptedException
    {
        //close the existing readyCheck.request if it is already open
        if (sui.hasPid(host, "readyCheck.noSnapshot"))
        {
            int pid = sui.getPid(host, "readyCheck.noSnapshot");
            forceCloseSUIPage(pid);
            sui.removePid(host, "readyCheck.noSnapshot");
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
    private obj_id[] getNoneResponseIds(obj_id self) throws InterruptedException {
        obj_id[] readyCheckResponsesNone = utils.getObjIdArrayScriptVar(self, "readyCheck.responses.none");
        if (readyCheckResponsesNone == null) {
            readyCheckResponsesNone = new obj_id[0];
        }
        return readyCheckResponsesNone;
    }
    private obj_id[] getYesResponseIds(obj_id self) throws InterruptedException {
        obj_id[] readyCheckResponsesYes = utils.getObjIdArrayScriptVar(self, "readyCheck.responses.yes");
        if (readyCheckResponsesYes == null) {
            readyCheckResponsesYes = new obj_id[0];
        }
        return readyCheckResponsesYes;
    }
    private obj_id[] getNoResponseIds(obj_id self) throws InterruptedException {
        obj_id[] readyCheckResponsesNo = utils.getObjIdArrayScriptVar(self, "readyCheck.responses.no");
        if (readyCheckResponsesNo == null) {
            readyCheckResponsesNo = new obj_id[0];
        }
        return readyCheckResponsesNo;
    }
    //response method to the messageTo invocation from members to leader informing the leader of their ready status
    public int readyCheckResponse(obj_id self, dictionary params) throws InterruptedException
    {
        //extract the responding object ID from the params dictionary
        obj_id respondingId = params.getObjId("responding_id");
        if (respondingId == null) {
            return SCRIPT_CONTINUE;
        }

        //get the response lists
        obj_id[] readyCheckResponsesNone = getNoneResponseIds(self);
        obj_id[] readyCheckResponsesYes = getYesResponseIds(self);
        obj_id[] readyCheckResponsesNo = getNoResponseIds(self);

        if (readyCheckResponsesNone.length == 0 && readyCheckResponsesYes.length == 0 && readyCheckResponsesNo.length == 0) {
            sendSystemMessage(respondingId, SID_READY_CHECK_RESPONSE_NO_CHECK);
            return SCRIPT_CONTINUE;
        }

        //create the list of people who have not yet responded to the ready check as the previous list's set without the responder
        //removes the responder from the list if they previously were in it
        ArrayList<obj_id> newNoneResponses = new ArrayList<>();
        for (obj_id member : readyCheckResponsesNone) {
            if (member != respondingId) {
                newNoneResponses.add(member);
            }
        }

        boolean ready = params.getBoolean("ready");
        //create the yes list as the previous list's set without the responder
        //removes the responder from the list if they previously were in it
        ArrayList<obj_id> newYesResponses = new ArrayList<>();
        for (obj_id member : readyCheckResponsesYes) {
            if (member != respondingId) {
                newYesResponses.add(member);
            } else if (ready) {
                //if user already in yes list and responding ready, inform the responder they are already set as ready
                sendSystemMessage(respondingId, SID_READY_CHECK_DUPLICATE_YES);
                return SCRIPT_CONTINUE;
            }
        }

        //create the yes list as the previous list's set without the responder
        //removes the responder from the list if they previously were in it
        ArrayList<obj_id> newNoResponses = new ArrayList<>();
        for (obj_id member : readyCheckResponsesNo) {
            if (member != respondingId) {
                newNoResponses.add(member);
            } else if (!ready) {
                //if user already in no list and responding ready, inform the responder they are already set as not ready
                sendSystemMessage(respondingId, SID_READY_CHECK_DUPLICATE_NO);
                return SCRIPT_CONTINUE;
            }
        }

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

        //save the scriptvar values
        setReadyCheckResponseScriptVars(newNoneResponses.toArray(obj_id[]::new), newYesResponses.toArray(obj_id[]::new), newNoResponses.toArray(obj_id[]::new), self);

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
        obj_id[] none = getNoneResponseIds(self);
        obj_id[] yes = getYesResponseIds(self);
        obj_id[] no = getNoResponseIds(self);
        showReadyCheckStatusPage(none, yes, no, self);
    }
}