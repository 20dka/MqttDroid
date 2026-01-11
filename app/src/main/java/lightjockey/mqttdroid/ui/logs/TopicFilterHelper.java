package lightjockey.mqttdroid.ui.logs;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopicFilterHelper {
    private static final String PREFS_NAME = "log_topic_filters";
    private static final String KEY_TOPICS = "saved_topics";
    
    // Pattern to extract topic from log messages
    // Matches: topic: "xxx" or topic \"xxx\" or topic \""xxx\"
    private static final Pattern TOPIC_PATTERN = Pattern.compile("topic[:\"\\s]+[\"']?([^\"'\\s]+)[\"']?", Pattern.CASE_INSENSITIVE);

    public static List<String> extractTopicsFromLogs(List<String> logLines) {
        Set<String> topics = new HashSet<>();
        
        for (String line : logLines) {
            Matcher matcher = TOPIC_PATTERN.matcher(line);
            while (matcher.find()) {
                String topic = matcher.group(1);
                if (topic != null && !topic.isEmpty()) {
                    topics.add(topic);
                }
            }
        }
        
        List<String> topicList = new ArrayList<>(topics);
        java.util.Collections.sort(topicList);
        return topicList;
    }

    public static List<String> getSavedTopics(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> topics = prefs.getStringSet(KEY_TOPICS, new HashSet<>());
        List<String> topicList = new ArrayList<>(topics);
        java.util.Collections.sort(topicList);
        return topicList;
    }

    public static void saveTopic(Context context, String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> topics = new HashSet<>(prefs.getStringSet(KEY_TOPICS, new HashSet<>()));
        topics.add(topic.trim());
        
        prefs.edit().putStringSet(KEY_TOPICS, topics).apply();
    }

    public static void removeTopic(Context context, String topic) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> topics = new HashSet<>(prefs.getStringSet(KEY_TOPICS, new HashSet<>()));
        topics.remove(topic);
        
        prefs.edit().putStringSet(KEY_TOPICS, topics).apply();
    }

    public static void clearAllTopics(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static List<String> filterLogsByTopic(List<String> logLines, String selectedTopic) {
        if (selectedTopic == null || selectedTopic.equals("all") || selectedTopic.isEmpty()) {
            return logLines;
        }
        
        List<String> filtered = new ArrayList<>();
        for (String line : logLines) {
            if (line.contains(selectedTopic)) {
                filtered.add(line);
            }
        }
        return filtered;
    }
}
