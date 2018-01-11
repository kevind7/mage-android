package mil.nga.giat.mage.map;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.cache.CacheOverlayOnMap;

public class MapOverlaysListFragment extends Fragment {

    public static class OverlayItemAdapter extends DragItemAdapter<CacheOverlayOnMap, OverlayItemViewHolder> {

        @Override
        public long getUniqueItemId(int i) {
            return 0;
        }

        @Override
        public OverlayItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cache_overlay_list_item, parent, false);
            return new OverlayItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(OverlayItemViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            CacheOverlayOnMap overlay = getItemList().get(position);
            Integer iconResourceId = overlay.getCacheOverlay().getIconImageResourceId();
            if (iconResourceId != null) {
                // TODO: default image
                holder.icon.setImageResource(overlay.getCacheOverlay().getIconImageResourceId());
            }
            holder.name.setText(overlay.getCacheOverlay().getOverlayName());
            holder.enabled.setChecked(overlay.isOnMap() && overlay.isVisible());
        }
    }

    private static class OverlayItemViewHolder extends DragItemAdapter.ViewHolder implements CompoundButton.OnCheckedChangeListener {

        private final ImageView icon;
        private final TextView name;
        private final SwitchCompat enabled;

        public OverlayItemViewHolder(View itemView) {
            super(itemView, R.id.overlay_item_name, false);
            icon = (ImageView) itemView.findViewById(R.id.overlay_item_image);
            name = (TextView) itemView.findViewById(R.id.overlay_item_name);
            enabled = (SwitchCompat) itemView.findViewById(R.id.overlay_item_enabled);
            enabled.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        }
    }


    private DragListView overlaysListView;
    private List<CacheOverlayOnMap> overlays;

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
        overlaysListView.setAdapter(new OverlayItemAdapter(), true);
        return root;
    }

    public void setOverlays(List<CacheOverlayOnMap> x) {
        List<CacheOverlayOnMap> writableList = new ArrayList<>(x);
        OverlayItemAdapter adapter = (OverlayItemAdapter) overlaysListView.getAdapter();
        adapter.setItemList(writableList);
    }
}
