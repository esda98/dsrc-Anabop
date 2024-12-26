package script.grouping;

import script.*;
import script.library.*;

import java.util.ArrayList;
import java.util.Arrays;

public class group_ready_check extends script.base_script
{
    public static final string_id SID_READY_CHECK_MUST_BE_GROUPED = new string_id("spam", "ready_check_must_be_grouped");
    public static final string_id SID_READY_CHECK_TWO_OR_MORE = new string_id("spam", "ready_check_two_or_more");
    public static final string_id SID_READY_CHECK_START = new string_id("spam", "ready_check_start");
    public static final string_id SID_READY_CHECK_RESPONSE_NO_CHECK = new string_id("spam", "ready_check_response_no_check");
    public static final string_id SID_READY_CHECK_CANCELLED = new string_id("spam", "ready_check_cancelled");
    public static final string_id SID_READY_CHECK_DUPLICATE_YES = new string_id("spam", "ready_check_duplicate_yes");
    public static final string_id SID_READY_CHECK_DUPLICATE_NO = new string_id("spam", "ready_check_duplicate_no");
    public static final string_id SID_READY_CHECK_MUST_BE_LEADER = new string_id("spam", "ready_check_must_be_leader");
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

        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");

        //check if this is a ready check response command
        if ((params.equals("yes") || params.equals("y"))
            || (params.equals("no") || params.equals("n"))) {

            //if there is no active ready check, inform the command performer that there is no ready check to respond to
            if (readyCheckPerformer == null) {
                sendSystemMessage(self, SID_READY_CHECK_RESPONSE_NO_CHECK);
                return SCRIPT_CONTINUE;
            }

            dictionary responseParams = new dictionary();
            responseParams.put("responding_id", self);
            responseParams.put("ready", params.equals("yes") || params.equals("y"));
            messageTo(readyCheckPerformer, "readyCheckResponse", responseParams, 1.0f, false);
            //try to auto refresh the snapshot page if it's open on the host
            if (sui.hasPid(self, "readyCheck.snapshot"))
            {
                messageTo(self, "refreshSnapshot", new dictionary(), 3.0f, false);
            }
            return SCRIPT_CONTINUE;
        }

        //see if this is the active performer of a ready check
        if (readyCheckPerformer != self) {
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
    public int refreshSnapshot(obj_id self, dictionary params) throws InterruptedException
    {
        if (sui.hasPid(self, "readyCheck.snapshot"))
        {
            //get the group members of self
            obj_id groupId = getGroupObject(self);
            //if there is no group ID, give an error message to the user
            if (groupId == null) {
                sendSystemMessage(self, SID_READY_CHECK_MUST_BE_GROUPED);
                return SCRIPT_CONTINUE;
            }

            obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
            if (readyCheckPerformer == null) {
                return SCRIPT_CONTINUE;
            }

            showReadyCheckSnapshotPage(self);
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
        //Keep Non-Group Leaders from Creating a new Ready Check: Remove to enable non-leaders to create ready checks
        obj_id leaderId = getGroupLeaderId(getGroupObject(self));
        if (leaderId != self) {
            sendSystemMessage(self, SID_READY_CHECK_MUST_BE_LEADER);
            return;
        }

        //send the readyCheck.request to the group members
        obj_id[] memberPlayerIds = getGroupMemberPlayers(getGroupObject(self));
        if (memberPlayerIds.length < 2) {
            sendSystemMessage(self, SID_READY_CHECK_TWO_OR_MORE);
            return;
        }
        for (obj_id member : memberPlayerIds) {
            sendSystemMessage(member, SID_READY_CHECK_START);
            dictionary readyRequestParams = new dictionary();
            readyRequestParams.put("performer_id", self);
            messageTo(member, "receiveReadyRequest", readyRequestParams, 1.0f, false);
        }

        //set the readyCheck.responses
        obj_id[] yes = new obj_id[0];
        obj_id[] no = new obj_id[0];

        setReadyCheckResponseScriptVars(memberPlayerIds, yes, no, self);

        //display the current status
        showReadyCheckStatusPage(memberPlayerIds, yes, no, self);

        //assign an id to this cleanup. if a cleanup attempt is processed and a different active cleanup ID is present, that cleanup can be discarded
        int activeCleanupId = utils.getIntScriptVar(self, "activeCleanupId");
        if (activeCleanupId > 0) {
            activeCleanupId++;
        } else {
            activeCleanupId = 1;
        }
        utils.setScriptVar(self, "activeCleanupId", activeCleanupId);
        dictionary cleanupParams = new dictionary();
        cleanupParams.put("cleanup_id", activeCleanupId);
        messageTo(self, "cleanupReadyCheck", cleanupParams, 60.0f, false);
    }
    //response to invoke messageTo for group members to be sent ready checks
    public int cleanupReadyCheck(obj_id self, dictionary params) throws InterruptedException
    {
        //kick out this cleanup attempt if the active cleanup ID is not this request
        int activeCleanupId = utils.getIntScriptVar(self, "activeCleanupId");
        if (params.getInt("cleanup_id") != activeCleanupId) {
            return SCRIPT_CONTINUE;
        }

        //ensure the user is grouped when receiving a ready request
        obj_id groupId = getGroupObject(self);
        if (groupId == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure there is a ready check performer on the group
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        if (readyCheckPerformer == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure ready check performer is self
        if (readyCheckPerformer != self) {
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
            closeReadyCheckSnapshotPage(member);
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

        //ensure the ready check performer is performing the ready check cancellation
        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        if (readyCheckPerformer == null) {
            sendSystemMessage(host, SID_READY_CHECK_RESPONSE_NO_CHECK);
            return;
        }

        //ensure the ready check performer is performing the cancellation
        if (readyCheckPerformer != host) {
            return;
        }

        clearReadyCheckResponseScriptVars(host);
        obj_id[] groupMembers = getGroupMemberIds(groupId);
        var rescindParams = new dictionary();
        rescindParams.put("performer_id", host);
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
    //handler for performer to invoke on members to cancel ready check operations on their client
    public int rescindReadyCheckRequest(obj_id self, dictionary params) throws InterruptedException
    {
        //get the performer of the ready check cancellation
        obj_id performerId = params.getObjId("performer_id");
        if (performerId == null)
        {
            return SCRIPT_CONTINUE;
        }

        //ensure the user is grouped when receiving a ready request
        obj_id groupId = getGroupObject(self);
        if (groupId == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure the performer of the active ready check has issued the cancellation request
        obj_id groupReadyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        if (groupReadyCheckPerformer == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure the ready check performer is performing the cancellation
        if (groupReadyCheckPerformer != performerId) {
            return SCRIPT_CONTINUE;
        }

        closeReadyCheckRequestPage(self);
        closeReadyCheckSnapshotPage(self);
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
        obj_id groupId = getGroupObject(host);
        if (groupId == null) {
            return;
        }

        utils.setScriptVar(groupId, "readyCheck.responses.none", none);
        utils.setScriptVar(groupId, "readyCheck.responses.yes", yes);
        utils.setScriptVar(groupId, "readyCheck.responses.no", no);
        utils.setScriptVar(groupId, "readyCheckPerformer", host);
    }
    public void showReadyCheckStatusPage(obj_id[] none, obj_id[] yes, obj_id[] no, obj_id host) throws InterruptedException
    {
        if (none.length == 0 && yes.length == 0 && no.length == 0) {
            //Keep Non-Group Leaders from Creating a new Ready Check: Remove to enable non-leaders to create ready checks
            obj_id leaderId = getGroupLeaderId(getGroupObject(host));
            if (leaderId != host) {
                sendSystemMessage(host, SID_READY_CHECK_MUST_BE_LEADER);
                return;
            }

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
        obj_id groupId = getGroupObject(host);
        if (groupId == null) {
            return;
        }

        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        if (readyCheckPerformer == null) {
            sendSystemMessage(host, SID_READY_CHECK_RESPONSE_NO_CHECK);
            return;
        }

        obj_id[] none = utils.getObjIdArrayScriptVar(groupId, "readyCheck.responses.none");
        if (none == null) {
            none = new obj_id[0];
        }

        obj_id[] yes = utils.getObjIdArrayScriptVar(groupId, "readyCheck.responses.yes");
        if (yes == null) {
            yes = new obj_id[0];
        }

        obj_id[] no = utils.getObjIdArrayScriptVar(groupId, "readyCheck.responses.no");
        if (no == null) {
            no = new obj_id[0];
        }

        if (none.length == 0 && yes.length == 0 && no.length == 0) {
            closeReadyCheckNoSnapshotPage(host);
            int pid = sui.msgbox(host, host, "@spam:ready_check_no_snapshot_prompt", sui.OK_ONLY, "@spam:ready_check_no_snapshot_title", sui.MSG_QUESTION, "onReadyCheckNoSnapshotResponse");
            sui.setPid(host, pid, "readyCheck.noSnapshot");
            return;
        }

        String[][] memberPlayersReady = buildReadyCheckTable(none, yes, no);

        //establish constants of the window layout
        String prompt = "Ready Check Status";
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

        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_OK)
        {
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
    //response method to SUI MessageBox asking if the group member would like to create a new ready check
    public int onReadyCheckCreateNewResponse(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_OK)
        {
            createNewReadyCheck(self);
        }
        return SCRIPT_CONTINUE;
    }
    //response method to SUI MessageBox informing a non-performer member there is no snapshot of a ready check to show
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
        //get the performer cancelling the ready request
        obj_id performerId = params.getObjId("performer_id");
        if (performerId == null)
        {
            return SCRIPT_CONTINUE;
        }

        //ensure the user is grouped when receiving a ready request
        obj_id groupId = getGroupObject(self);
        if (groupId == null) {
            return SCRIPT_CONTINUE;
        }

        obj_id groupReadyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        if (groupReadyCheckPerformer == null) {
            return SCRIPT_CONTINUE;
        }

        //ensure the group ready check performer matches who sent the requests for ready checks
        if (groupReadyCheckPerformer != performerId) {
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

        obj_id readyCheckPerformer = utils.getObjIdScriptVar(groupId, "readyCheckPerformer");
        if (readyCheckPerformer == null) {
            return SCRIPT_CONTINUE;
        }

        dictionary responseParams = new dictionary();
        responseParams.put("responding_id", self);
        if (btn == sui.BP_CANCEL)
        {
            responseParams.put("ready", false);
            messageTo(readyCheckPerformer, "readyCheckResponse", responseParams, 1.0f, false);
        }
        else
        {
            responseParams.put("ready", true);
            messageTo(readyCheckPerformer, "readyCheckResponse", responseParams, 1.0f, false);
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
    //response method to the messageTo invocation from members to performers informing the performer of their ready status
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
    //reload the ready check status page for performers
    public void reloadReadyCheckPage(obj_id self) throws InterruptedException
    {
        obj_id[] none = getNoneResponseIds(self);
        obj_id[] yes = getYesResponseIds(self);
        obj_id[] no = getNoResponseIds(self);
        showReadyCheckStatusPage(none, yes, no, self);
    }
}