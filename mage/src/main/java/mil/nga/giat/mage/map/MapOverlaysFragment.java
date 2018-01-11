package mil.nga.giat.mage.map;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mil.nga.giat.mage.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapOverlaysFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapOverlaysFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapOverlaysFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String tilesTitle;
    private String featuresTitle;
    private String mParam1;
    private String mParam2;
    private Fragment tilesFragment;
    private Fragment featuresFragment;
    private OverlayTabsPagerAdapter pagerAdapter;

    private OnFragmentInteractionListener mListener;

    public MapOverlaysFragment() {
    }

    // TODO: Rename and change types and number of parameters
    public static MapOverlaysFragment newInstance() { //String param1, String param2) {
        MapOverlaysFragment fragment = new MapOverlaysFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
        tilesTitle = getResources().getString(R.string.overlay_tab_tiles);
        featuresTitle = getResources().getString(R.string.overlay_tab_features);
        tilesFragment = new TestPageFragment();
        Bundle tilesArgs = new Bundle();
        tilesArgs.putCharSequence("text", "TILES");
        tilesFragment.setArguments(tilesArgs);
        featuresFragment = new TestPageFragment();
        Bundle featuresArgs = new Bundle();
        featuresArgs.putCharSequence("text", "FEATURES");
        featuresFragment.setArguments(featuresArgs);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map_overlays, container, false);
        ViewPager tabPager = (ViewPager) view.findViewById(R.id.map_overlay_tab_pager);
        if (tabPager != null) {
            tabPager.setAdapter(new OverlayTabsPagerAdapter(getChildFragmentManager()));
            TabLayout tabs = (TabLayout) view.findViewById(R.id.map_overlay_tabs);
            tabs.setupWithViewPager(tabPager);
        }
        else {
            getChildFragmentManager().beginTransaction()
                .replace(R.id.tiles_container, tilesFragment)
                .replace(R.id.features_container, featuresFragment)
                .commit();
        }
        return view;
    }

//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        ViewPager tabPager = (ViewPager) getView().findViewById(R.id.map_overlay_tab_pager);
//        if (tabPager != null) {
//            tabPager.setAdapter(new OverlayTabsPagerAdapter(getChildFragmentManager()));
//        }
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        }
//        else {
//            throw new RuntimeException(context.toString()
//                + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public static class TestPageFragment extends Fragment {

        private CharSequence text;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            text = args.getCharSequence("text");
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            TextView view = new TextView(getContext());
            view.setTextSize(28.0f);
            view.setText(String.valueOf(text));
            return view;
        }
    }

    class OverlayTabsPagerAdapter extends FragmentPagerAdapter {

        OverlayTabsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return tilesTitle;
                case 1:
                    return featuresTitle;
            }
            return null;
        }

        @Override
        public Fragment getItem(final int position) {
            switch (position) {
                case 0:
                    // tiles
                    return tilesFragment;
                case 1:
                    // features
                    return featuresFragment;
            }
//            Fragment item = new TestPageFragment();
//            Bundle args = new Bundle();
//            args.putCharSequence("text", String.valueOf(position));
//            item.setArguments(args);
            return null;
        }
    }
}
