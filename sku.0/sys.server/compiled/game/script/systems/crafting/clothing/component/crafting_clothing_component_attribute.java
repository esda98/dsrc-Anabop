package script.systems.crafting.clothing.component;

import script.library.craftinglib;
import script.library.utils;
import script.obj_id;

public class crafting_clothing_component_attribute extends script.base_script
{
    public crafting_clothing_component_attribute()
    {
    }
    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(self, craftinglib.OBJVAR_RE_VALUE))
        {
            names[idx] = "@crafting:power_bit_power";
            attribs[idx] = "" + getFloatObjVar(self, craftinglib.OBJVAR_RE_VALUE);
            idx++;
            if (idx >= names.length)
            {
                return SCRIPT_CONTINUE;
            }
        }
        if (hasObjVar(self, craftinglib.OBJVAR_RE_STAT_MODIFIED))
        {
            names[idx] = "@crafting:mod_bit_type";
            attribs[idx] = "@stat_n:" + getStringObjVar(self, craftinglib.OBJVAR_RE_STAT_MODIFIED);
            idx++;
            if (idx >= names.length)
            {
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_CONTINUE;
    }
}
