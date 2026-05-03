package com.smartcampus.manouba.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SocialPagerAdapter extends FragmentStateAdapter {

    public SocialPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return new ComposePostFragment();
            case 2:  return new SearchUsersFragment();
            default: return new FeedFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
