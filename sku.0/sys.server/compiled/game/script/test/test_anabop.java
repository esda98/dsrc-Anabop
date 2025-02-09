package script.test;
import script.library.utils;
import script.obj_id;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.URL;
import java.io.BufferedReader;
import java.lang.management.ManagementFactory;

public class test_anabop extends script.base_script
{
    public test_anabop()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        debugSpeakMsg(self, "test_anabop: attached!");
        debugServerConsoleMsg(self, "test_anabop: attached!");
        return SCRIPT_CONTINUE;
    }
    public int OnDetach(obj_id self) throws InterruptedException
    {
        debugSpeakMsg(self, "test_anabop: detached!");
        debugServerConsoleMsg(self, "test_anabop: detached!");
        return SCRIPT_CONTINUE;
    }
    public int OnSpeaking(obj_id self, String text) throws InterruptedException
    {
        if (text.startsWith("elour spawn "))
        {
            //https://backup.swglegends.com/exportResourcesApiMagicYep.php?key=swglrox&res=pevisis
            /*obj_id target = getIntendedTarget(self);
            sendSystemMessageTestingOnly(self, getResourceCtsData(target));*/
            String resName = text.substring(12);
            sendSystemMessageTestingOnly(self, "Extracted resource name: " + resName);
            obj_id inv = utils.getInventoryContainer(self);
            obj_id newItem = createObject("object/resource_container/inorganic_water.iff", inv, "");
            if(isIdValid(newItem))
            {
                try
                {
                    URL url = new URL("https://backup.swglegends.com/exportResourcesApiMagicYep.php?key=swglrox&res=" + resName);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.addRequestProperty("Content-Type", "application/json");
                    connection.addRequestProperty("User-Agent", "Java-BlourBot");
                    connection.setDoOutput(true);
                    //connection.setRequestMethod("POST");
                    //OutputStream stream = connection.getOutputStream();
                    //stream.write(json.toString().getBytes());
                    //stream.flush();
                    //stream.close();
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder builder = new StringBuilder();
                    String lines;
                    while ((lines = reader.readLine()) != null) {
                        builder.append(lines);
                        builder.append(System.getProperty("line.separator"));
                    }
                    String retStr = builder.toString().trim();
                    connection.getInputStream().close();
                    connection.disconnect();
                    spawnResourceString(self, retStr, newItem);
                }
                catch( Exception err )
                {
                    sendSystemMessageTestingOnly(self, "Issue with HttpUrlConn? " + err.getLocalizedMessage());
                }
            }
            else
            {
                sendSystemMessageTestingOnly(self, "Error creating resource container in inventory. Full?");
            }
            return SCRIPT_OVERRIDE;
        }
        else if (text.startsWith("anabop spawntext "))
        {
            try
            {
                String resStr = text.substring(17);
                sendSystemMessageTestingOnly(self, "Extracted resource string: " + resStr);
                spawnResourceString(self, resStr);
            }
            catch( Exception err )
            {
                sendSystemMessageTestingOnly(self, "An error occurred: " + err.getLocalizedMessage());
            }
        }
        return SCRIPT_CONTINUE;
    }
    private void spawnResourceString(obj_id player, String resStr, obj_id replaceObj) throws InterruptedException
    {
        obj_id inv = utils.getInventoryContainer(player);
        if (replaceObj == null) {
            replaceObj = createObject("object/resource_container/inorganic_water.iff", inv, "");
        }
        sendSystemMessageTestingOnly(player, resStr); //debug
        if(resStr.length() < 15)
        {
            sendSystemMessageTestingOnly(player, "Resource not found on Omega");
            destroyObject(replaceObj);
            return;
        }
        setResourceCtsData(replaceObj, 1000000, resStr);
        sendSystemMessageTestingOnly(player, "Successfully created resource");
    }
    private void spawnResourceString(obj_id player, String resStr) throws InterruptedException
    {
        spawnResourceString(player, resStr, null);
    }
}