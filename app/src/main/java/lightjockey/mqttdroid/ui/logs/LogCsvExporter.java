package lightjockey.mqttdroid.ui.logs;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogCsvExporter {
    // Logcat format: MM-DD HH:MM:SS.mmm PID TID Level/Tag: Message
    // Example: 01-11 13:45:23.456  1234  5678 D MQTT: Publishing msg (topic: "test", payload: "hello")
    // More flexible pattern to handle various logcat formats
    private static final Pattern LOGCAT_PATTERN = Pattern.compile(
        "(\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\d+\\s+\\d+\\s+([DEIWAV])\\s+[^:]+:\\s*(.+)"
    );
    
    // Pattern to extract topic from message
    private static final Pattern TOPIC_PATTERN = Pattern.compile("topic[:\"\\s]+[\"']?([^\"'\\s]+)[\"']?", Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract payload from message
    // Matches: payload: "xxx" or payload \"xxx\"
    private static final Pattern PAYLOAD_PATTERN = Pattern.compile("payload[:\"\\s]+[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    
    // Pattern to match Received msg with payload (handles JSON payloads)
    private static final Pattern RECEIVED_MSG_PATTERN = Pattern.compile("Received msg.*?payload[:\"\\s]+[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract JSON payload (handles payloads that start with { or [)
    // This pattern tries to match balanced braces/brackets
    private static final Pattern JSON_PAYLOAD_PATTERN = Pattern.compile("payload[:\"\\s]+[\"']?([{\\[][^\"']*[}\\]])[\"']?", Pattern.CASE_INSENSITIVE);

    public static class LogEntry {
        public String datetime;    // 年月日时分秒毫秒（yyyy-MM-dd HH:mm:ss.SSS格式）
        public String topic;       // topic，如果没有则为空
        public String level;       // 日志等级 D, E, I, W
        public String message;     // message，如果有payload则显示payload，否则显示完整message
    }

    public static List<LogEntry> parseLogs(List<String> logLines) {
        List<LogEntry> entries = new ArrayList<>();
        
        for (String line : logLines) {
            LogEntry entry = parseLogLine(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        
        return entries;
    }

    private static LogEntry parseLogLine(String line) {
        Matcher matcher = LOGCAT_PATTERN.matcher(line);
        if (!matcher.find()) {
            // If pattern doesn't match, try to parse as fallback
            return parseLogLineFallback(line);
        }

        LogEntry entry = new LogEntry();
        
        // Parse date and time
        String dateStr = matcher.group(1);  // MM-DD
        String timeStr = matcher.group(2);  // HH:MM:SS.mmm
        entry.level = matcher.group(3);     // D, E, I, W
        
        // Convert to datetime string: yyyy-MM-dd HH:mm:ss.SSS
        String currentYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        // dateStr is MM-DD, convert to yyyy-MM-dd
        String fullDate = currentYear + "-" + dateStr;  // yyyy-MM-DD
        // timeStr is already HH:MM:SS.mmm, just combine with date
        entry.datetime = fullDate + " " + timeStr;  // yyyy-MM-dd HH:mm:ss.SSS
        
        // Extract message part
        String messagePart = matcher.group(4);
        
        // Try to extract topic from message
        Matcher topicMatcher = TOPIC_PATTERN.matcher(messagePart);
        if (topicMatcher.find()) {
            entry.topic = topicMatcher.group(1);
        } else {
            // Also try to extract from full line
            topicMatcher = TOPIC_PATTERN.matcher(line);
            if (topicMatcher.find()) {
                entry.topic = topicMatcher.group(1);
            } else {
                entry.topic = "";
            }
        }
        
        // Try to extract payload from message
        String extractedPayload = null;
        
        // Look for payload: "..." pattern, handling escaped quotes and JSON
        int payloadIndex = messagePart.toLowerCase().indexOf("payload");
        if (payloadIndex >= 0) {
            // Find the opening quote after "payload"
            int quoteStart = messagePart.indexOf("\"", payloadIndex);
            if (quoteStart >= 0) {
                // Find the closing quote, handling escaped quotes
                int quoteEnd = findClosingQuote(messagePart, quoteStart + 1);
                if (quoteEnd > quoteStart) {
                    extractedPayload = messagePart.substring(quoteStart + 1, quoteEnd);
                    // Unescape quotes and backslashes
                    extractedPayload = extractedPayload.replace("\\\"", "\"").replace("\\\\", "\\");
                }
            }
        }
        
        // If payload found and meaningful (not just single character), use it
        if (extractedPayload != null && !extractedPayload.trim().isEmpty() && extractedPayload.trim().length() > 1) {
            entry.message = extractedPayload;
        } else {
            // Use full message part, ensure it's not empty
            // This ensures we don't lose information
            entry.message = (messagePart != null && !messagePart.trim().isEmpty()) ? messagePart : line;
        }
        
        return entry;
    }

    private static LogEntry parseLogLineFallback(String line) {
        // Fallback parser for lines that don't match standard pattern
        LogEntry entry = new LogEntry();
        
        // Try to extract basic info
        if (line.contains(" D ")) {
            entry.level = "D";
        } else if (line.contains(" E ")) {
            entry.level = "E";
        } else if (line.contains(" I ")) {
            entry.level = "I";
        } else if (line.contains(" W ")) {
            entry.level = "W";
        } else {
            entry.level = "?";
        }
        
        // Use current datetime as fallback
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        entry.datetime = sdf.format(new Date());
        
        // Try to extract topic
        Matcher topicMatcher = TOPIC_PATTERN.matcher(line);
        if (topicMatcher.find()) {
            entry.topic = topicMatcher.group(1);
        } else {
            entry.topic = "";
        }
        
        // Use full line as message
        entry.message = line;
        
        return entry;
    }

    public static File exportToCsv(Context context, List<LogEntry> entries, String topic) throws IOException {
        // Generate filename: yyyyMMddHHmmss_topic.csv
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String filename = timestamp + "_" + (topic != null && !topic.equals("all") ? topic : "all") + ".csv";
        
        // Create file in cache directory
        File cacheDir = context.getCacheDir();
        File csvFile = new File(cacheDir, filename);
        
        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write BOM for Excel compatibility
            writer.write('\ufeff');
            
            // Write CSV header
            writer.write("收到日期,topic,日志等级,message\n");
            
            // Write data rows
            for (LogEntry entry : entries) {
                writer.write(escapeCsv(entry.datetime) + ",");
                writer.write(escapeCsv(entry.topic) + ",");
                writer.write(escapeCsv(entry.level) + ",");
                writer.write(escapeCsv(entry.message) + "\n");
            }
        }
        
        return csvFile;
    }

    private static int findClosingQuote(String text, int startPos) {
        boolean escaped = false;
        for (int i = startPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
