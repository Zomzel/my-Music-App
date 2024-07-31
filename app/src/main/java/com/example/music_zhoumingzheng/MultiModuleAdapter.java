package com.example.music_zhoumingzheng;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.music_zhoumingzheng.adapter.BannerAdapter;
import com.example.music_zhoumingzheng.adapter.CardAdapter;
import com.example.music_zhoumingzheng.adapter.PopularAdapter;
import com.example.music_zhoumingzheng.adapter.RecommendAdapter;
import com.example.music_zhoumingzheng.module.BannerModule;
import com.example.music_zhoumingzheng.module.BaseModule;
import com.example.music_zhoumingzheng.module.CardModule;
import com.example.music_zhoumingzheng.module.DataBean;
import com.example.music_zhoumingzheng.module.PopularModule;
import com.example.music_zhoumingzheng.module.RecommendModule;
import com.example.music_zhoumingzheng.module.TitleModule;



import java.util.List;

public class MultiModuleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<BaseModule> moduleList;

    //2.定义接口类型的变量存储数据
    private OnItemClickListener mOnItemClickListener;
    //1.定义item点击事件的接口
    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    // 3. 定义回调方法
    public void setOnItemClickListener(OnItemClickListener listener){
        this.mOnItemClickListener = listener;
    }

    public MultiModuleAdapter(Context context, List<BaseModule> moduleList) {
        this.context = context;
        this.moduleList = moduleList;
    }

    @Override
    public int getItemViewType(int position) {
        return moduleList.get(position).getType();
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case BaseModule.TYPE_TITLE:
                View titleView = inflater.inflate(R.layout.item_title, parent, false);
                return new TitleViewHolder(titleView);
            case BaseModule.TYPE_BANNER:
                View bannerView = inflater.inflate(R.layout.item_banner, parent, false);
                return new BannerViewHolder(bannerView);
            case BaseModule.TYPE_CARD:
                View cardView = inflater.inflate(R.layout.item_card, parent, false);
                return new CardViewHolder(cardView);
            case BaseModule.TYPE_RECOMMEND:
                View recommendView = inflater.inflate(R.layout.item_recommend, parent, false);
                return new RecommendViewHolder(recommendView);
            case BaseModule.TYPE_POPULAR:
                View popularView = inflater.inflate(R.layout.item_popular, parent, false);
                return new PopularViewHolder(popularView);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BaseModule module = moduleList.get(position);
        switch (module.getType()) {
            case BaseModule.TYPE_BANNER:
                ((BannerViewHolder) holder).bind((BannerModule) module);
                break;
            case BaseModule.TYPE_CARD:
                ((CardViewHolder) holder).bind((CardModule) module);
                break;
            case BaseModule.TYPE_RECOMMEND:
                ((RecommendViewHolder) holder).bind((RecommendModule) module);
                break;
            case BaseModule.TYPE_POPULAR:
                ((PopularViewHolder) holder).bind((PopularModule) module);
                break;
            case BaseModule.TYPE_TITLE:
                ((TitleViewHolder) holder).bind((TitleModule) module);
                break;
        }

        //4. 调用接口方法
        if(mOnItemClickListener != null){
            // Set click listener for all item types
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(pos);
                    }
                }
            });
        }
    }


    public static class TitleViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;

        public TitleViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.title_text_view);
        }

        public void bind(TitleModule module) {
            titleTextView.setText(module.getTittle());
        }
    }
    public static class CardViewHolder extends RecyclerView.ViewHolder {

        RecyclerView recyclerView;
        CardAdapter adapter;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view_card);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        }

        public void bind(CardModule module) {
            adapter = new CardAdapter(itemView.getContext(), module.getMusicInfos());
            recyclerView.setAdapter(adapter);
            int initialPosition = adapter.getItemCount() / 2;
            recyclerView.scrollToPosition(initialPosition);
        }

    }
    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        private ViewPager2 viewPager2;
        private BannerAdapter bannerAdapter;
        private List<DataBean.Data.Record.MusicInfo> musicInfos;
        private LinearLayout indicatorLayout;
        private Handler handler;
        private Runnable runnable;
        private boolean isIndicatorInitialized;

        BannerViewHolder(View itemView) {
            super(itemView);
            viewPager2 = itemView.findViewById(R.id.viewPager2);
            indicatorLayout = itemView.findViewById(R.id.indicatorLayout);
            handler = new Handler(Looper.getMainLooper());
            isIndicatorInitialized = false;
        }

        void bind(BannerModule module) {
            this.musicInfos = module.getMusicInfos();
            if (viewPager2.getAdapter() == null) {
                bannerAdapter = new BannerAdapter(viewPager2.getContext(), musicInfos);
                viewPager2.setAdapter(bannerAdapter);
            } else {
//                bannerAdapter.setMusicInfos(musicInfos);
//                bannerAdapter.notifyDataSetChanged();
            }

            if (!isIndicatorInitialized) {
                setupIndicator();
                isIndicatorInitialized = true;
            }

            autoSlideImages();
        }

        private void setupIndicator() {
            int itemCount = bannerAdapter.getItemCount();
            ImageView[] indicators = new ImageView[itemCount];
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);

            for (int i = 0; i < itemCount; i++) {
                indicators[i] = new ImageView(indicatorLayout.getContext());
                indicators[i].setImageDrawable(ContextCompat.getDrawable(
                        indicatorLayout.getContext(),
                        R.drawable.grey_dot
                ));
                indicators[i].setLayoutParams(params);
                indicatorLayout.addView(indicators[i]);
            }

            if (indicators.length > 0) {
                indicators[0].setImageDrawable(ContextCompat.getDrawable(
                        indicatorLayout.getContext(),
                        R.drawable.red_dot
                ));
            }

            viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    for (int i = 0; i < indicators.length; i++) {
                        indicators[i].setImageDrawable(ContextCompat.getDrawable(
                                indicatorLayout.getContext(),
                                R.drawable.grey_dot
                        ));
                    }
                    indicators[position].setImageDrawable(ContextCompat.getDrawable(
                            indicatorLayout.getContext(),
                            R.drawable.red_dot
                    ));
                }
            });
        }

        private void autoSlideImages() {
            runnable = new Runnable() {
                @Override
                public void run() {
                    int currentItem = viewPager2.getCurrentItem();
                    int totalItemCount = bannerAdapter.getItemCount();
                    if (currentItem == totalItemCount - 1) {
                        viewPager2.setCurrentItem(0);
                    } else {
                        viewPager2.setCurrentItem(currentItem + 1);
                    }
                    handler.postDelayed(this, 3000); // 每隔3秒切换一次图片
                }
            };
            handler.postDelayed(runnable, 3000); // 开始自动轮播
        }


    }
    public static class RecommendViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recommendRecyclerView;

        public RecommendViewHolder(@NonNull View itemView) {
            super(itemView);
            recommendRecyclerView = itemView.findViewById(R.id.recommend_recycler_view);
            recommendRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.VERTICAL, false));
        }

        public void bind(RecommendModule module) {
            recommendRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            RecommendAdapter adapter = new RecommendAdapter(itemView.getContext(),module.getMusicInfos());
            recommendRecyclerView.setAdapter(adapter);
        }
    }
    public static class PopularViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView recyclerView;
        private PopularAdapter adapter;

        PopularViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view_popular);
        }

        void bind(PopularModule module) {
            adapter = new PopularAdapter(itemView.getContext(), module.getMusicInfos());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new GridLayoutManager(itemView.getContext(), 2));
        }
    }

    @Override
    public int getItemCount() {
        return moduleList.size();
    }

    public BaseModule getItem(int position){
        int index = position;
        if(position<0 || position >= moduleList.size()){
            Toast.makeText(context, "位置非法", Toast.LENGTH_SHORT).show();
            index = 0;
        }
        if(moduleList.isEmpty())
            return null;
        return moduleList.get(index);
    }


}
