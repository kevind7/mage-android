package mil.nga.giat.mage.map;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.cache.CacheOverlay;

public class MapOverlaysListFragment extends Fragment {

    public static class CacheOverlayViewHolder extends DragItemAdapter.ViewHolder {

        public CacheOverlayViewHolder(View itemView, int handleResId, boolean dragOnLongPress) {
            super(itemView, handleResId, dragOnLongPress);
        }
    }

    public static class OverlayItemAdapter extends DragItemAdapter<CacheOverlay, CacheOverlayViewHolder> {

        @Override
        public long getUniqueItemId(int i) {
            return 0;
        }

        @Override
        public CacheOverlayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }
    }

    private DragListView overlaysListView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map_overlays_tiles, container, true);
        overlaysListView = (DragListView) root.findViewById(R.id.tile_overlays_list);
        overlaysListView.getRecyclerView().setVerticalScrollBarEnabled(true);
        overlaysListView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }
}
