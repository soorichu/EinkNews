package cloud.einknote.newspaper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class HeadlineAdapter extends RecyclerView.Adapter<HeadlineAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Article article, int pageNum);
    }

    private final List<Article> articles;
    private final OnItemClickListener listener;

    public HeadlineAdapter(List<Article> articles, OnItemClickListener listener) {
        this.articles = articles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_headline_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Article article = articles.get(position);
        int pageNum = position + 1;

        holder.tvIndex.setText(String.format(Locale.getDefault(), "%02d", pageNum));

        // 15~20자 처리 (필요시 말줄임 처리 함수 연결)
        String title = article.getTitle();
        if (title != null && title.length() > 20) {
            title = title.substring(0, 20);
        }
        holder.tvTitle.setText(title);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(article, pageNum);
            }
        });
    }

    @Override
    public int getItemCount() {
        return articles != null ? articles.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex;
        TextView tvTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvCardIndex);
            tvTitle = itemView.findViewById(R.id.tvCardTitle);
        }
    }
}