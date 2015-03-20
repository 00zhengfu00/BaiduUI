/*
 * Copyright 2014 Soichiro Kashima
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ksoichiro.android.observablescrollview.sampler;

import sggs.android.baiduui.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.github.ksoichiro.android.observablescrollview.CacheFragmentStatePagerAdapter;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.github.ksoichiro.android.observablescrollview.Scrollable;
import com.github.ksoichiro.android.observablescrollview.TouchInterceptionFrameLayout;
import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

/**
 * Another implementation of ViewPagerTabActivity. This uses
 * TouchInterceptionFrameLayout to move Fragments.
 * <p/>
 * SlidingTabLayout and SlidingTabStrip are from google/iosched:
 * https://github.com/google/iosched
 */
public class ViewPagerTab2Activity extends BaseActivity implements ObservableScrollViewCallbacks {
	private static final boolean ADJUSTTOOLBAR_ENABLE = false;
	private static final boolean SCROLLINGUP_NOW = false;
	
	private View mToolbarView;
	private TouchInterceptionFrameLayout mInterceptionLayout;
	private ViewPager mPager;
	private NavigationAdapter mPagerAdapter;
	private int mSlop;
	private boolean mScrolled;
	private ScrollState mLastScrollState;
	private int lp_height = 0;
	private View search;
	private int startAmi;
	private int tabHeight;
	private View toolbar;
	private View view_toolbar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewpagertab2);

		mToolbarView = findViewById(R.id.toolbar_translationY);
		mPagerAdapter = new NavigationAdapter(getSupportFragmentManager());
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mPagerAdapter);
		
		toolbar = findViewById(R.id.toolbar);
		view_toolbar = findViewById(R.id.view_toolbar);
		ViewHelper.setAlpha(toolbar, 0);
		// Padding for ViewPager must be set outside the ViewPager itself
		// because with padding, EdgeEffect of ViewPager become strange.
		tabHeight = getResources().getDimensionPixelSize(R.dimen.tab_height);
		startAmi = tabHeight * 2 - getTranslationY();
		findViewById(R.id.pager_wrapper).setPadding(0, getTranslationY() + tabHeight * 2, 0, 0);

		SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
		slidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);
		slidingTabLayout.setSelectedIndicatorColors(getResources().getColor(R.color.accent));
		slidingTabLayout.setDistributeEvenly(true);
		slidingTabLayout.setViewPager(mPager);

		ViewConfiguration vc = ViewConfiguration.get(this);
		mSlop = vc.getScaledTouchSlop();
		mInterceptionLayout = (TouchInterceptionFrameLayout) findViewById(R.id.container);
		mInterceptionLayout.setScrollInterceptionListener(mInterceptionListener);
		
		search = findViewById(R.id.search);
	}

	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
	}

	@Override
	public void onDownMotionEvent() {
	}

	@Override
	public void onUpOrCancelMotionEvent(ScrollState scrollState) {
		if (!mScrolled) {
			// This event can be used only when TouchInterceptionFrameLayout
			// doesn't handle the consecutive events.
			adjustToolbar(scrollState);
		}
	}

	private TouchInterceptionFrameLayout.TouchInterceptionListener mInterceptionListener = new TouchInterceptionFrameLayout.TouchInterceptionListener() {
		@Override
		public boolean shouldInterceptTouchEvent(MotionEvent ev, boolean moving, float diffX, float diffY) {
			if (!mScrolled && mSlop < Math.abs(diffX) && Math.abs(diffY) < Math.abs(diffX)) {
				// Horizontal scroll is maybe handled by ViewPager
				return false;
			}

			Scrollable scrollable = getCurrentScrollable();
			if (scrollable == null) {
				mScrolled = false;
				return false;
			}

			// If interceptionLayout can move, it should intercept.
			// And once it begins to move, horizontal scroll shouldn't work any
			// longer.
			int toolbarHeight = mToolbarView.getHeight();
			int translationY = (int) ViewHelper.getTranslationY(mInterceptionLayout);
			boolean scrollingUp = 0 < diffY;
			boolean scrollingDown = diffY < 0;
			if (scrollingUp) {
				if ((SCROLLINGUP_NOW || scrollable.getCurrentScrollY() == 0) && translationY < 0) {
					mScrolled = true;
					mLastScrollState = ScrollState.UP;
					return true;
				}
			} else if (scrollingDown) {
				
				if (-toolbarHeight < translationY) {
					mScrolled = true;
					mLastScrollState = ScrollState.DOWN;
					return true;
				}
			}
			mScrolled = false;
			return false;
		}

		@Override
		public void onDownMotionEvent(MotionEvent ev) {
		}

		@Override
		public void onMoveMotionEvent(MotionEvent ev, float diffX, float diffY) {
			float translationY = ScrollUtils.getFloat(ViewHelper.getTranslationY(mInterceptionLayout) + diffY, -mToolbarView.getHeight(), 0);
			ViewHelper.setTranslationY(mInterceptionLayout, translationY);
			ViewHelper.setTranslationY(view_toolbar, -translationY);
			if (translationY < 0) {
				if (lp_height == 0) {
					FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInterceptionLayout.getLayoutParams();
					lp.height = (int) (-translationY + getScreenHeight());
					mInterceptionLayout.requestLayout();
				}
				
				RelativeLayout.LayoutParams params = (LayoutParams) search.getLayoutParams();
				int left = dipToPx(10);
				int right = dipToPx(10);
				float percent = 0;
				if (translationY < startAmi) {
					percent = (startAmi - translationY) / tabHeight / 2;
				} 
				ViewHelper.setAlpha(toolbar, percent);
				
				int abSize = getTranslationY();
				percent = -translationY / abSize;
				left += percent * dipToPx(30);
				right += percent * dipToPx(30);

				params.setMargins(left, params.topMargin, right, params.bottomMargin);
				search.setLayoutParams(params);
			}
		}

		@Override
		public void onUpOrCancelMotionEvent(MotionEvent ev) {
			mScrolled = false;
			adjustToolbar(mLastScrollState);
		}
	};

	public int dipToPx(int dipValue){
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, getResources().getDisplayMetrics());
	}
	
	private Scrollable getCurrentScrollable() {
		Fragment fragment = getCurrentFragment();
		if (fragment == null) {
			return null;
		}
		View view = fragment.getView();
		if (view == null) {
			return null;
		}
		return (Scrollable) view.findViewById(R.id.scroll);
	}

	private void adjustToolbar(ScrollState scrollState) {
		if(!ADJUSTTOOLBAR_ENABLE)
			return;
		int toolbarHeight = mToolbarView.getHeight();
		final Scrollable scrollable = getCurrentScrollable();
		if (scrollable == null) {
			return;
		}
		int scrollY = scrollable.getCurrentScrollY();
		if (scrollState == ScrollState.DOWN) {
			showToolbar();
		} else if (scrollState == ScrollState.UP) {
			if (toolbarHeight <= scrollY) {
				hideToolbar();
			} else {
				showToolbar();
			}
		} else if (!toolbarIsShown() && !toolbarIsHidden()) {
			// Toolbar is moving but doesn't know which to move:
			// you can change this to hideToolbar()
			showToolbar();
		}
	}

	private Fragment getCurrentFragment() {
		return mPagerAdapter.getItemAt(mPager.getCurrentItem());
	}

	private boolean toolbarIsShown() {
		return ViewHelper.getTranslationY(mInterceptionLayout) == 0;
	}

	private boolean toolbarIsHidden() {
		return ViewHelper.getTranslationY(mInterceptionLayout) == -mToolbarView.getHeight();
	}

	private void showToolbar() {
		animateToolbar(0);
	}

	private void hideToolbar() {
		animateToolbar(-mToolbarView.getHeight());
	}

	private void animateToolbar(final float toY) {
		float layoutTranslationY = ViewHelper.getTranslationY(mInterceptionLayout);
		if (layoutTranslationY != toY) {
			ValueAnimator animator = ValueAnimator.ofFloat(ViewHelper.getTranslationY(mInterceptionLayout), toY).setDuration(200);
			animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					float translationY = (Float) animation.getAnimatedValue();
					ViewHelper.setTranslationY(mInterceptionLayout, translationY);
					if (translationY < 0) {
						FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInterceptionLayout.getLayoutParams();
						lp.height = (int) (-translationY + getScreenHeight());
						mInterceptionLayout.requestLayout();
					}
				}
			});
			animator.start();
		}
	}

	/**
	 * This adapter provides two types of fragments as an example.
	 * {@linkplain #createItem(int)} should be modified if you use this example
	 * for your app.
	 */
	private static class NavigationAdapter extends CacheFragmentStatePagerAdapter {

		private static final String[] TITLES = new String[] { "Applepie", "Butter Cookie", "Cupcake", "Donut", "Eclair", "Froyo", "Gingerbread", "Honeycomb",
				"Ice Cream Sandwich", "Jelly Bean", "KitKat", "Lollipop" };

		public NavigationAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		protected Fragment createItem(int position) {
			Fragment f;
			final int pattern = position % 5;
			switch (pattern) {
			case 0:
				f = new ViewPagerTab2ListViewFragment();
				break;
			case 1:
				f = new ViewPagerTab2ListViewFragment();
				break;
			case 2:
				f = new ViewPagerTab2ListViewFragment();
				break;
			case 3:
				f = new ViewPagerTab2ListViewFragment();
				break;
			case 4:
			default:
				f = new ViewPagerTab2ListViewFragment();
				break;
			}
			return f;
		}

		@Override
		public int getCount() {
			return TITLES.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return TITLES[position];
		}
	}
}
