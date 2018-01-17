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
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.OverlayOnMapManager;

public class MapOverlaysListFragment extends Fragment {

    public class OverlayItemAdapter extends DragItemAdapter<CacheOverlay, OverlayItemViewHolder> {

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
            CacheOverlay overlay = getItemList().get(position);
            holder.setOverlay(overlay);
        }
    }

    private class OverlayItemViewHolder extends DragItemAdapter.ViewHolder implements CompoundButton.OnCheckedChangeListener {

        private final ImageView icon;
        private final TextView name;
        private final SwitchCompat enabled;
        private CacheOverlay overlay;

        public OverlayItemViewHolder(View itemView) {
            super(itemView, R.id.overlay_item_name, false);
            icon = (ImageView) itemView.findViewById(R.id.overlay_item_image);
            name = (TextView) itemView.findViewById(R.id.overlay_item_name);
            enabled = (SwitchCompat) itemView.findViewById(R.id.overlay_item_enabled);
        }

        private void setOverlay(CacheOverlay overlay) {
            Integer iconResourceId = overlay.getIconImageResourceId();
            if (iconResourceId != null) {
                icon.setImageResource(iconResourceId);
            }
            name.setText(overlay.getOverlayName());
            enabled.setOnCheckedChangeListener(null);
            // TODO: set inivisible to suppress animation so switches don't animate as list scrolls; is there a better way?
            enabled.setVisibility(View.INVISIBLE);
            enabled.setChecked(overlayManager.isOverlayVisible(overlay));
            enabled.setOnCheckedChangeListener(this);
            enabled.setVisibility(View.VISIBLE);
//            enabled.jumpDrawablesToCurrentState();
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked){
                overlayManager.showOverlay(overlay);
            }
        }
    }


    private OverlayOnMapManager overlayManager;
    private DragListView overlaysListView;
    private List<CacheOverlay> overlays;

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

    public void setOverlays(List<CacheOverlay> x) {
        List<CacheOverlay> writableList = new ArrayList<>(x);
        OverlayItemAdapter adapter = (OverlayItemAdapter) overlaysListView.getAdapter();
        adapter.setItemList(writableList);
    }
}
