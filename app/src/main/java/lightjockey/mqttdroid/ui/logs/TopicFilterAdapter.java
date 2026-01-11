package lightjockey.mqttdroid.ui.logs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lightjockey.mqttdroid.R;
import lightjockey.mqttdroid.databinding.ItemTopicFilterBinding;

public class TopicFilterAdapter extends RecyclerView.Adapter<TopicFilterAdapter.TopicViewHolder> {
    public List<String> topics = new ArrayList<>();
    private OnTopicClickListener listener;

    public interface OnTopicClickListener {
        void onTopicClick(String topic);
        void onTopicRemove(String topic);
    }

    public void setOnTopicClickListener(OnTopicClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTopicFilterBinding binding = ItemTopicFilterBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new TopicViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        holder.bind(topics.get(position));
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    public void setTopics(List<String> newTopics) {
        this.topics = newTopics != null ? newTopics : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addTopic(String topic) {
        if (topic != null && !topic.isEmpty() && !topics.contains(topic)) {
            topics.add(topic);
            notifyItemInserted(topics.size() - 1);
        }
    }

    public void removeTopic(String topic) {
        int index = topics.indexOf(topic);
        if (index >= 0) {
            topics.remove(index);
            notifyItemRemoved(index);
        }
    }

    class TopicViewHolder extends RecyclerView.ViewHolder {
        private final ItemTopicFilterBinding binding;

        TopicViewHolder(@NonNull ItemTopicFilterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String topic) {
            binding.textTopic.setText(topic);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTopicClick(topic);
                }
            });
            binding.buttonRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTopicRemove(topic);
                }
            });
        }
    }
}
