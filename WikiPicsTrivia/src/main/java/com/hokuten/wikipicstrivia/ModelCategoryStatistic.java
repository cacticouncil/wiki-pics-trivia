package com.hokuten.wikipicstrivia;


import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelCategoryStatistic
{
    public enum Status
    {
        SUCCESS,
        FAILED_BLACKLIST,
        FAILED_WIKI,
        FAILED_IMAGE
    }

    public String                               name;
    public int                                  numTopicsQueried;
    public HashMap<String, ModelTopicStatistic> topicStatistics;
    public List<ModelCategoryProperty> propertyStatistics;

    public ModelCategoryStatistic(String name, int numTopics, List<ModelCategoryProperty> attributes)
    {
        this.name = name;
        this.numTopicsQueried = numTopics;
        this.topicStatistics = new HashMap<String, ModelTopicStatistic>();
        this.propertyStatistics = new ArrayList<ModelCategoryProperty>();

        if(attributes != null)
        {
            this.propertyStatistics.addAll(attributes);
        }
    }

    public void addTopic(String mid)
    {
        topicStatistics.put(mid, new ModelTopicStatistic(mid, propertyStatistics));
    }

    public void setAttributeValid(String mid, ModelCategoryProperty attribute)
    {
        ModelTopicStatistic topic = topicStatistics.get(mid);
        topic.setProperty(attribute, true);
    }

    public void logStatistics()
    {
        int failedTopics = 0;

        Log.v("ModelTopicStatistics", "#######################################################");
        Log.v("ModelCategoryStatistics", "Category: " + name);

        HashMap<String, Integer> finalStats = new HashMap<String, Integer>();

        for(ModelTopicStatistic topic: topicStatistics.values())
        {
            if(topic.status != Status.SUCCESS)
                failedTopics++;

            for(ModelCategoryProperty property: propertyStatistics)
            {
                if(!topic.getPropertyStatus(property.id))
                {
                    Integer numFailedAttributes = finalStats.get(property.name);
                    if(numFailedAttributes != null)
                    {
                        finalStats.put(property.name, numFailedAttributes++);
                    }
                    else
                    {
                        finalStats.put(property.name, 1);
                    }
                }
            }

            topic.logStatistics();
        }
        for(Map.Entry<String, Integer> entry : finalStats.entrySet())
        {
            Log.v("ModelCategoryStatistics", " Failed " + entry.getValue() + "/" +
                    numTopicsQueried + " for property: " + entry.getKey());
        }
        Log.v("ModelCategoryStatistics", " Failed " + failedTopics + "/" + numTopicsQueried + " # of topics");

        Log.v("ModelTopicStatistics", "#######################################################");
    }

    // Class to keep track of topic statistics
    public class ModelTopicStatistic
    {
        public String mid;
        public Status status;

        public List<ModelTopicPropertyStatistic> propertyStatistics;

        public ModelTopicStatistic(String mid, List<ModelCategoryProperty> properties)
        {
            this.mid = mid;
            this.status = Status.SUCCESS;
            this.propertyStatistics = new ArrayList<ModelTopicPropertyStatistic>();

            if(properties != null)
            {
                for (ModelCategoryProperty categoryProperty : properties)
                {
                    this.propertyStatistics.add(
                            new ModelTopicPropertyStatistic(categoryProperty, false));
                }
            }
        }

        public void setProperty(ModelCategoryProperty categoryProperty, Boolean status)
        {
            if(propertyStatistics != null)
            {
                for (ModelTopicPropertyStatistic propertyStatistic : propertyStatistics)
                {
                    if (propertyStatistic.property.id == categoryProperty.id)
                    {
                        propertyStatistic.status = status;
                    }
                }
            }
        }

        public boolean getPropertyStatus(int id)
        {
            for (ModelTopicPropertyStatistic propertyStatistic : propertyStatistics)
            {
                if (propertyStatistic.property.id == id)
                {
                    return propertyStatistic.status;
                }
            }

            return false;
        }


        public void logStatistics()
        {
            Log.v("ModelTopicStatistics", "===============================================");
            Log.v("ModelTopicStatistics", "Mid: " + mid + " Status: " + status.toString());

            if(propertyStatistics != null)
            {
                for (ModelTopicPropertyStatistic property : propertyStatistics)
                {
                    property.logStatistics();
                }
            }
            Log.v("ModelTopicStatistics", "===============================================");
        }

        // Class to keep track of property statistics
        public class ModelTopicPropertyStatistic
        {
            ModelCategoryProperty property;
            boolean               status;

            public ModelTopicPropertyStatistic(ModelCategoryProperty property, boolean status)
            {
                this.property = property;
                this.status   = status;
            }

            public void logStatistics()
            {
                Log.v("ModelTopicStatistics", "Attribute Id: " + property.id);
                Log.v("ModelTopicStatistics", "Attribute Name: " + property.name);
                Log.v("ModelTopicStatistics", "Valid: " + Boolean.toString(status));
            }
        }
    }

}
