package lightjockey.mqttdroid.ui.logs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lightjockey.mqttdroid.MqttClient;
import lightjockey.mqttdroid.R;
import lightjockey.mqttdroid.databinding.FragmentLogsBinding;
import lightjockey.mqttdroid.ui.logs.TopicListActivity;

public class LogsFragment extends Fragment {
    private FragmentLogsBinding binding;
    private LogAdapter logAdapter;
    private TopicFilterAdapter topicFilterAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<String> allLogLines = new ArrayList<>();
    private String selectedTopic = "all";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogsBinding.inflate(inflater, container, false);

        setupRecyclerView();
        setupTopicFilter();
        setupSwipeRefresh();
        loadLogs();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        logAdapter = new LogAdapter();
        RecyclerView recyclerView = binding.recyclerLogs;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(logAdapter);
    }

    private void setupTopicFilter() {
        // Setup topic filter RecyclerView
        topicFilterAdapter = new TopicFilterAdapter();
        RecyclerView topicRecycler = binding.recyclerTopics;
        topicRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        topicRecycler.setAdapter(topicFilterAdapter);

        // Load saved topics
        loadSavedTopics();

        // Setup topic filter click listener
        topicFilterAdapter.setOnTopicClickListener(new TopicFilterAdapter.OnTopicClickListener() {
            @Override
            public void onTopicClick(String topic) {
                selectedTopic = topic;
                binding.inputTopic.setText(topic);
                applyFilter();
            }

            @Override
            public void onTopicRemove(String topic) {
                TopicFilterHelper.removeTopic(getContext(), topic);
                topicFilterAdapter.removeTopic(topic);
                if (selectedTopic.equals(topic)) {
                    selectedTopic = "all";
                    binding.inputTopic.setText("");
                    applyFilter();
                }
            }
        });

        // Setup input field
        TextInputEditText inputTopic = binding.inputTopic;
        
        // Add TextWatcher for real-time filtering
        inputTopic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String topic = s != null ? s.toString().trim() : "";
                if (topic.equalsIgnoreCase("all") || topic.isEmpty()) {
                    selectedTopic = "all";
                    applyFilter();
                } else if (!topic.isEmpty()) {
                    // Real-time filter only, don't save until Enter/Done is pressed
                    selectedTopic = topic;
                    applyFilter();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Handle Enter/Done key press
        inputTopic.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String topic = v.getText() != null ? v.getText().toString().trim() : "";
                if (topic.equalsIgnoreCase("all") || topic.isEmpty()) {
                    selectedTopic = "all";
                    applyFilter();
                } else {
                    TopicFilterHelper.saveTopic(getContext(), topic);
                    if (!topicFilterAdapter.topics.contains(topic)) {
                        topicFilterAdapter.addTopic(topic);
                        // Reload to get sorted list
                        loadSavedTopics();
                    }
                    selectedTopic = topic;
                    applyFilter();
                }
                // Hide keyboard
                if (v != null) {
                    v.clearFocus();
                }
                return true;
            }
            return false;
        });

        // Setup share logs button
        binding.buttonShareLogs.setOnClickListener(v -> shareLogs());
        
        // Setup view all topics button
        binding.buttonViewAllTopics.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(), TopicListActivity.class);
            startActivityForResult(intent, 100);
        });
    }
    
    private void updateRecentTopics() {
        // Show only recent 5 topics to avoid clutter
        List<String> allTopics = TopicFilterHelper.getSavedTopics(getContext());
        int maxRecent = 5;
        List<String> recentTopics = allTopics.size() > maxRecent 
            ? allTopics.subList(0, Math.min(maxRecent, allTopics.size()))
            : allTopics;
        topicFilterAdapter.setTopics(recentTopics);
    }

    private void loadSavedTopics() {
        updateRecentTopics();
    }

    private void setupSwipeRefresh() {
        SwipeRefreshLayout swipeRefresh = binding.swipeRefresh;
        swipeRefresh.setOnRefreshListener(() -> {
            loadLogs();
            // Stop refreshing after a short delay
            handler.postDelayed(() -> swipeRefresh.setRefreshing(false), 500);
        });
    }

    private void loadLogs() {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("logcat -d");

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                List<String> logLines = new ArrayList<>();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains(MqttClient.TAG)) {
                        logLines.add(line);
                    }
                }

                // Reverse the list so newest logs appear first
                Collections.reverse(logLines);
                allLogLines = logLines;

                // Extract topics from logs and update saved topics
                List<String> extractedTopics = TopicFilterHelper.extractTopicsFromLogs(logLines);
                handler.post(() -> {
                    // Add new topics to the filter list
                    for (String topic : extractedTopics) {
                        TopicFilterHelper.saveTopic(getContext(), topic);
                    }
                    // Reload to get sorted list (show only recent ones)
                    updateRecentTopics();
                });

                // Update UI on main thread
                handler.post(() -> {
                    applyFilter();
                });
            } catch (IOException ignored) {
                handler.post(() -> {
                    logAdapter.setLogs(Collections.singletonList(getString(R.string.error_loading_logs)));
                });
            }
        }).start();
    }

    private void applyFilter() {
        List<String> filtered = TopicFilterHelper.filterLogsByTopic(allLogLines, selectedTopic);
        if (filtered.isEmpty()) {
            logAdapter.setLogs(Collections.singletonList(getString(R.string.no_logs_yet)));
        } else {
            logAdapter.setLogs(filtered);
        }
    }

    private void shareLogs() {
        new Thread(() -> {
            try {
                // Get filtered logs
                List<String> filteredLogs = TopicFilterHelper.filterLogsByTopic(allLogLines, selectedTopic);
                
                // Parse logs
                List<LogCsvExporter.LogEntry> entries = LogCsvExporter.parseLogs(filteredLogs);
                
                if (entries.isEmpty()) {
                    handler.post(() -> {
                        android.widget.Toast.makeText(getContext(), R.string.no_logs_to_share, android.widget.Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // Export to CSV
                File csvFile = LogCsvExporter.exportToCsv(getContext(), entries, selectedTopic);
                
                // Share file on main thread
                handler.post(() -> {
                    shareCsvFile(csvFile);
                });
            } catch (IOException e) {
                handler.post(() -> {
                    android.widget.Toast.makeText(getContext(), R.string.error_sharing_logs, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void shareCsvFile(File csvFile) {
        try {
            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                getContext(),
                getContext().getPackageName() + ".fileprovider",
                csvFile
            );

            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, csvFile.getName());
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_logs)));
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(), R.string.error_sharing_logs, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh recent topics when returning from TopicListActivity
        if (binding != null) {
            updateRecentTopics();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            String selectedTopic = data.getStringExtra("selected_topic");
            if (selectedTopic != null) {
                binding.inputTopic.setText(selectedTopic);
                this.selectedTopic = selectedTopic;
                applyFilter();
            }
            // Refresh recent topics
            updateRecentTopics();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}