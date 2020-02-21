package com.hokuten.wikipicstrivia;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

public class ManagerReport
{
    HashMap<String, ModelCategoryStatistic> categoryStatistics;
    private static ManagerReport mInstance;
    long startTime;

    private ManagerReport()
    {
        categoryStatistics = new HashMap<String, ModelCategoryStatistic>();
    }

    public static ManagerReport instance()
    {
        if (mInstance == null) mInstance = new ManagerReport();
        return mInstance;
    }

    public void resetReport()
    {
        categoryStatistics.clear();
        startTime = System.currentTimeMillis();
    }

    public void addCategory(String categoryName, int numTopics,  List<ModelCategoryProperty> attributes)
    {
        categoryStatistics.put(categoryName, new ModelCategoryStatistic(categoryName, numTopics, attributes));
    }

    public void addTopic(String categoryName, String mid)
    {
        ModelCategoryStatistic stats = categoryStatistics.get(categoryName);
        stats.addTopic(mid);
    }

    public void setTopicStatus(String categoryName, String mid, ModelCategoryStatistic.Status status)
    {
        ModelCategoryStatistic stats = categoryStatistics.get(categoryName);
        ModelCategoryStatistic.ModelTopicStatistic topic = stats.topicStatistics.get(mid);

        topic.status = status;
    }

    public void setTopicAttributeValid(String categoryName, String mid, ModelCategoryProperty attribute)
    {
        ModelCategoryStatistic stats = categoryStatistics.get(categoryName);
        stats.setAttributeValid(mid, attribute);
    }

    public void outputReport()
    {
        if (AppConfig.DEBUG)
        {
            Log.v("ManagerReport", "Algorithm took " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");

            for (ModelCategoryStatistic category : categoryStatistics.values())
            {
                category.logStatistics();
            }
        }
    }
}
