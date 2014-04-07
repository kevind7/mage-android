	package mil.nga.giat.mage;

import java.util.Locale;

import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.newsfeed.NewsFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.preferences.PublicPreferencesActivity;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

/**
 * FIXME: Currently a mock of what a landing page might look like. Could be
 * replaced entirely if need be. Menu options do exist.
 * 
 * @author wiedemannse
 * 
 */
public class LandingActivity extends FragmentActivity {
	
	private static final int RESULT_PUBLIC_PREFERENCES = 1;
	private static final int RESULT_MAP_PREFERENCES = 2;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	PagerTabStrip tabStrip;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landing);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		tabStrip = (PagerTabStrip)findViewById(R.id.pager_tab_strip);
		tabStrip.setTabIndicatorColor(getResources().getColor(android.R.color.holo_blue_bright));
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// FIXME : need to consider connectivity before talking to the server!!!
		((MAGE) getApplication()).startFetching();
		
		((MAGE) getApplication()).startPushing();
		
		// Start location services
		((MAGE) getApplication()).initLocationService();
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		((MAGE) getApplication()).destroyFetching();
		((MAGE) getApplication()).destroyPushing();
		((MAGE) getApplication()).destroyLocationService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.landing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_settings: {
				Intent i = new Intent(this, PublicPreferencesActivity.class);
				startActivityForResult(i, RESULT_PUBLIC_PREFERENCES);
				break;
			}
			case R.id.menu_logout: {
				// TODO : wipe user certs, really just wipe out the token from shared preferences
				UserUtility.getInstance(getApplicationContext()).clearTokenInformation();
				startActivity(new Intent(getApplicationContext(), LoginActivity.class));
				finish();
				break;
			}

		case R.id.observation_new:
			 Intent intent = new Intent(this, ObservationEditActivity.class);
			 LocationService ls = ((MAGE) getApplication()).getLocationService();
			 Location l = ls.getLocation();
			 intent.putExtra(ObservationEditActivity.LOCATION, l);
	       	 startActivity(intent);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case RESULT_PUBLIC_PREFERENCES:
			System.out.println(RESULT_PUBLIC_PREFERENCES);
			break;
		case RESULT_MAP_PREFERENCES:
			System.out.println(RESULT_MAP_PREFERENCES);
			break;
		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch (position) {
				case 0: {
					fragment = new MapFragment();
					break;
				}
				case 1: {
					fragment = new NewsFeedFragment();
					break;
				}
				case 2: {
					fragment = new PeopleFeedFragment();
					break;
				}
				default: {
					// TODO not sure what to do here, if anything (fix your code)
				}
			}
			
			return fragment;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_map).toUpperCase(l);
			case 1:
				return getString(R.string.title_observations).toUpperCase(l);
			case 2:
				return getString(R.string.title_people).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * Takes you to the home screen
	 */
	@Override
	public void onBackPressed() {
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}

}