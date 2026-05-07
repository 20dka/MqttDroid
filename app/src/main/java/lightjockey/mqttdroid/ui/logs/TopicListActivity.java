package lightjockey.mqttdroid.ui.logs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lightjockey.mqttdroid.R;
import lightjockey.mqttdroid.databinding.ActivityTopicListBinding;
import lightjockey.mqttdroid.databinding.ItemTopicFilterBinding;

public class TopicListActivity extends AppCompatActivity {
    private ActivityTopicListBinding binding;
    private TopicListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTopicListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.all_topics);
        }

        setupRecyclerView();
        loadTopics();
        
        binding.buttonClearAll.setOnClickListener(v -> {
            TopicFilterHelper.clearAllTopics(this);
            adapter.setTopics(new java.util.ArrayList<>());
        });
    }

    private void setupRecyclerView() {
        adapter = new TopicListAdapter();
        RecyclerView recyclerView = binding.recyclerTopics;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadTopics() {
        List<String> topics = TopicFilterHelper.getSavedTopics(this);
        adapter.setTopics(topics);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class TopicListAdapter extends RecyclerView.Adapter<TopicListAdapter.TopicViewHolder> {
        private List<String> topics = new java.util.ArrayList<>();

        public void setTopics(List<String> newTopics) {
            this.topics = newTopics != null ? newTopics : new java.util.ArrayList<>();
            notifyDataSetChanged();
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

        class TopicViewHolder extends RecyclerView.ViewHolder {
            private final ItemTopicFilterBinding binding;

            TopicViewHolder(@NonNull ItemTopicFilterBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(String topic) {
                binding.textTopic.setText(topic);
                binding.getRoot().setOnClickListener(v -> {
                    // Return topic to LogsFragment
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selected_topic", topic);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
                binding.buttonRemove.setOnClickListener(v -> {
                    TopicFilterHelper.removeTopic(TopicListActivity.this, topic);
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        topics.remove(position);
                        notifyItemRemoved(position);
                    }
                });
            }
        }
    }
}
