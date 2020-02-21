package com.hokuten.wikipicstrivia;


public class ModelCategoryProperty
{
    public enum Type
    {
        STRING,
        DATE,
        INT,
        FLOAT
    }

    public enum Unit
    {
        NONE,
        METER,
        KILOMETER,
        KMSQUARED,
        DAYS,
        KMSECOND
    }

    public enum Filter
    {
        NONE,
        CITY,
        COUNTRY,
        CONTINENT,
        REGION
    }

    public static final String[] Units = {"", "m", "km", "km^2", "days", "km/s"};
    public static final String[] Filters = {"", "/location/citytown", "/location/country", "/location/continent", "/location/statistical_region"};

    public int    id;
    public String name;
    public Type   type;
    public Unit   unit;
    public Filter filter;

    public ModelCategoryProperty()
    {
        id     = -1;
        name   = "";
        type   = Type.STRING;
        unit   = Unit.NONE;
        filter = Filter.NONE;
    }
}
