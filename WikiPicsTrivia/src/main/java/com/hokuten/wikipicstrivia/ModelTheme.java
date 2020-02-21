package com.hokuten.wikipicstrivia;


public class ModelTheme
{
    public int     id;
    public String  name;
    public int     color;
    public boolean checked;
    public boolean expanded;

    public ModelTheme()
    {
        id       = -1;
        name     = "";
        color    = 0;
        checked  = false;
        expanded = false;
    }
}
