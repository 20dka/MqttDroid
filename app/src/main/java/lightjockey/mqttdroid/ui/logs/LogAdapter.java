package lightjockey.mqttdroid.ui.logs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private List<String> logs = new ArrayList<>();

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        textView.setTextSize(12);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        int padding = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding / 2, padding, padding / 2);
        textView.setSingleLine(false);
        return new LogViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public void setLogs(List<String> newLogs) {
        this.logs = newLogs != null ? newLogs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addLog(String log) {
        if (log != null && !log.isEmpty()) {
            logs.add(0, log); // Add to the beginning (newest first)
            notifyItemInserted(0);
        }
    }

    public void clearLogs() {
        int size = logs.size();
        logs.clear();
        notifyItemRangeRemoved(0, size);
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        LogViewHolder(@NonNull TextView textView) {
            super(textView);
            this.textView = textView;
        }

        void bind(String log) {
            textView.setText(log);
        }
    }
}
