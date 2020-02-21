package com.hokuten.wikipicstrivia;


public class ModelSettings
{
    public String  locale;
    public boolean sfx;
    public boolean doNotRepeat;
    public boolean learning;
    public boolean showAnswer;
    public int     updates;
    public int     increment;
    public int     help;
    public boolean qotd;
    public int     qotd_hour;
    public int     qotd_minute;
    public boolean music;

    public boolean isFirstTimeHelp(ModelHelp.Help h)
    {
        return ((help & (1 << h.ordinal())) == 0);
    }

    public void setDoNotShowHelpAgain(ModelHelp.Help h)
    {
        int flag = (1 << h.ordinal());
        help |= flag;
    }

    public void resetHelp()
    {
        help = 0;
    }
}
