package edu.rhit.groupalarm.groupalarm;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class AlarmPagerAdapter extends FragmentPagerAdapter {
    private User mUser;

    public AlarmPagerAdapter(FragmentManager fm, User user) {
        super(fm);
        mUser = user;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return PlaceholderFragment.newInstance(position + 1, mUser);
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return 3;
    }
}