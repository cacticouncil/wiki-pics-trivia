package com.hokuten.wikipicstrivia;


public class ModelCategory
{
    public int     id;
    public String  displayName;
    public String  name;
    public boolean checked;

    public ModelCategory()
    {
        id          = -1;
        displayName = "";
        name        = "";
        checked     = false;
    }
}
