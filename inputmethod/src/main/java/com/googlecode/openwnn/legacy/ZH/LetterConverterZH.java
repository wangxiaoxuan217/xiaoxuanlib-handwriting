package com.googlecode.openwnn.legacy.ZH;

import java.util.HashMap;

import com.googlecode.openwnn.legacy.ComposingText;
import com.googlecode.openwnn.legacy.LetterConverter;
import com.googlecode.openwnn.legacy.StrSegment;

public class LetterConverterZH implements LetterConverter
{
    private static final HashMap<String, String> convTable = new HashMap<String, String>()
    {
        {
            put(".", "\u3002");
            put(",", "\uff0c");
            put("?", "\uff1f");
            put("(", "\uff08");
            put(")", "\uff09");
            put("~", "\uff5e");
            put("\"", "\u201c");
            put("'", "\u2018");
            put(":", "\uff1a");
            put(";", "\uff1b");
            put("!", "\uff01");
            put("^", "\u2026\u2026");
            put("\u00a5", "\uffe5");
            put("$", "\uffe5");
            put("\\", "\u3001");
            put("[", "\u3010");
            put("]", "\u3011");
            put("_", "\u2014\u2014");
            put("{", "\u3014");
            put("}", "\u3015");
            put("`", "\u00b7");
            put("<", "\u300a");
            put(">", "\u300b");
        }
    };
    
    private static final HashMap<String, String> convTableShifted = new HashMap<String, String>()
    {
        {
            put(".", "\u3002");
            put(",", "\uff0c");
            put("?", "\uff1f");
            put("(", "\uff08");
            put(")", "\uff09");
            put("~", "\uff5e");
            put("\"", "\u201c");
            put("'", "\u2018");
            put(":", "\uff1a");
            put(";", "\uff1b");
            put("!", "\uff01");
            put("^", "\u2026\u2026");
            put("\u00a5", "\uffe5");
            put("$", "\uffe5");
            put("\\", "\u3001");
            put("[", "\u3010");
            put("]", "\u3011");
            put("_", "\u2014\u2014");
            put("{", "\u3014");
            put("}", "\u3015");
            put("`", "\u00b7");
            put("<", "\u300a");
            put(">", "\u300b");
        }
    };
    
    public boolean convert(ComposingText text, int shift)
    {
        int cursor = text.getCursor(1);
        String match;
        if (cursor <= 0)
        {
            return false;
        }
        
        StrSegment[] str = new StrSegment[3];
        int start = 2;
        str[2] = text.getStrSegment(1, cursor - 1);
        if (cursor >= 2)
        {
            str[1] = text.getStrSegment(1, cursor - 2);
            start = 1;
            if (cursor >= 3)
            {
                str[0] = text.getStrSegment(1, cursor - 3);
                start = 0;
            }
        }
        
        StringBuffer key = new StringBuffer();
        while (start < 3)
        {
            for (int i = start; i < 3; i++)
            {
                key.append(str[i].string);
            }
            if (shift == 0)
            {
                match = (String)LetterConverterZH.convTable.get(key.toString().toLowerCase());
            }
            else
            {
                match = (String)LetterConverterZH.convTableShifted.get(key.toString().toLowerCase());
            }
            if (match != null)
            {
                StrSegment[] out;
                if (match.length() == 1)
                {
                    out = new StrSegment[1];
                    out[0] = new StrSegment(match, str[start].from, str[2].to);
                    text.replaceStrSegment(1, out, 3 - start);
                }
                else
                {
                    out = new StrSegment[2];
                    out[0] = new StrSegment(match.substring(0, match.length() - 1), str[start].from, str[2].to - 1);
                    out[1] = new StrSegment(match.substring(match.length() - 1), str[2].to, str[2].to);
                    text.replaceStrSegment(1, out, 3 - start);
                }
                return true;
            }
            start++;
            key.delete(0, key.length());
        }
        return false;
    }
    
    public boolean convert(ComposingText text)
    {
        return convert(text, 0);
    }
    
}
