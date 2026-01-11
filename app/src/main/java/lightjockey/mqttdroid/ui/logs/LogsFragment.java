package lightjockey.mqttdroid.ui.logs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lightjockey.mqttdroid.MqttClient;
import lightjockey.mqttdroid.R;
import lightjockey.mqttdroid.databinding.FragmentLogsBinding;

public class LogsFragment extends Fragment {
    private FragmentLogsBinding binding;
    private LogAdapter logAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogsBinding.inflate(inflater, container, false);

        setupRecyclerView();
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

                // Update UI on main thread
                handler.post(() -> {
                    if (logLines.isEmpty()) {
                        logAdapter.setLogs(Collections.singletonList(getString(R.string.no_logs_yet)));
                    } else {
                        logAdapter.setLogs(logLines);
                    }
                });
            } catch (IOException ignored) {
                handler.post(() -> {
                    logAdapter.setLogs(Collections.singletonList(getString(R.string.error_loading_logs)));
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}