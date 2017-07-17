package com.tech42.callrecorder.viewpager;

/**
 * Created by sathish on 29/06/17.
 */

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.tech42.callrecorder.views.LocalStorage;
import com.tech42.callrecorder.views.RecordedFragment;
import com.tech42.callrecorder.views.SavedFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position ==0) {
            return new RecordedFragment();
        }else if (position == 1){
            return new SavedFragment();
        } else{
            return new LocalStorage();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
