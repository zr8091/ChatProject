package com.yzx.chat.mvp.view.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.yzx.chat.R;
import com.yzx.chat.base.BaseFragment;
import com.yzx.chat.base.BaseRecyclerViewAdapter;
import com.yzx.chat.bean.ContactBean;
import com.yzx.chat.broadcast.BackPressedReceive;
import com.yzx.chat.mvp.contract.ContactListContract;
import com.yzx.chat.mvp.presenter.ContactListPresenter;
import com.yzx.chat.util.AndroidUtil;
import com.yzx.chat.util.AnimationUtil;
import com.yzx.chat.mvp.view.activity.ContactOperationActivity;
import com.yzx.chat.mvp.view.activity.ContactProfileActivity;
import com.yzx.chat.mvp.view.activity.FindNewContactActivity;
import com.yzx.chat.mvp.view.activity.GroupListActivity;
import com.yzx.chat.mvp.view.activity.HomeActivity;
import com.yzx.chat.mvp.view.activity.RemarkInfoActivity;
import com.yzx.chat.widget.adapter.ContactAdapter;
import com.yzx.chat.widget.adapter.ContactSearchAdapter;
import com.yzx.chat.widget.listener.AutoCloseKeyboardScrollListener;
import com.yzx.chat.widget.listener.AutoEnableOverScrollListener;
import com.yzx.chat.widget.listener.OnRecyclerViewItemClickListener;
import com.yzx.chat.widget.view.BadgeView;
import com.yzx.chat.widget.view.IndexBarView;
import com.yzx.chat.widget.view.LetterSegmentationItemDecoration;
import com.yzx.chat.widget.view.OverflowMenuShowHelper;
import com.yzx.chat.widget.view.OverflowPopupMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YZX on 2017年06月28日.
 * 生命太短暂,不要去做一些根本没有人想要的东西
 */


public class ContactListFragment extends BaseFragment<ContactListContract.Presenter> implements ContactListContract.View {

    public static final String TAG = ContactListFragment.class.getSimpleName();

    private RecyclerView mRvContact;
    private RecyclerView mRvSearchContact;
    private ContactAdapter mContactAdapter;
    private ContactSearchAdapter mSearchAdapter;
    private IndexBarView mIndexBarView;
    private View mHeaderView;
    private TextView mTvIndexBarHint;
    private Toolbar mToolbar;
    private PopupWindow mSearchPopupWindow;
    private SearchView mSearchView;
    private SmartRefreshLayout mSmartRefreshLayout;
    private View mLlContactOperation;
    private View mLlGroup;
    private BadgeView mBadgeView;
    private FloatingActionButton mFBtnAdd;
    private LinearLayoutManager mLinearLayoutManager;
    private AutoEnableOverScrollListener mAutoEnableOverScrollListener;
    private LetterSegmentationItemDecoration mLetterSegmentationItemDecoration;
    private OverflowPopupMenu mContactMenu;
    private Handler mSearchHandler;
    private List<ContactBean> mContactList;
    private List<ContactBean> mContactSearchList;


    @Override
    protected int getLayoutID() {
        return R.layout.fragment_contact_list;
    }

    @Override
    protected void init(View parentView) {
        mToolbar = parentView.findViewById(R.id.Default_mToolbar);
        mRvContact = parentView.findViewById(R.id.ContactFragmentList_mRvContact);
        mIndexBarView = parentView.findViewById(R.id.ContactFragmentList_mIndexBarView);
        mTvIndexBarHint = parentView.findViewById(R.id.ContactFragmentList_mTvIndexBarHint);
        mFBtnAdd = parentView.findViewById(R.id.ContactFragmentList_mFBtnAdd);
        mSmartRefreshLayout = parentView.findViewById(R.id.ContactFragmentList_mSmartRefreshLayout);
        mHeaderView = LayoutInflater.from(mContext).inflate(R.layout.item_contact_header, (ViewGroup) parentView, false);
        mLlContactOperation = mHeaderView.findViewById(R.id.ContactFragmentList_mLlContactOperation);
        mLlGroup = mHeaderView.findViewById(R.id.ContactFragmentList_mLlGroup);
        mBadgeView = mHeaderView.findViewById(R.id.ContactFragmentList_mBadgeView);
        mRvSearchContact = new RecyclerView(mContext);
        mContactMenu = new OverflowPopupMenu(mContext);
        mAutoEnableOverScrollListener = new AutoEnableOverScrollListener(mSmartRefreshLayout);
        mContactList = new ArrayList<>(256);
        mContactSearchList = new ArrayList<>(32);
        mContactAdapter = new ContactAdapter(mContactList);
        mSearchAdapter = new ContactSearchAdapter(mContactList, mContactSearchList);
        mSearchHandler = new Handler();
    }

    @Override
    protected void setup() {
        setSearchBar();

        mLetterSegmentationItemDecoration = new LetterSegmentationItemDecoration();
        mLetterSegmentationItemDecoration.setLineColor(ContextCompat.getColor(mContext, R.color.divider_color_black));
        mLetterSegmentationItemDecoration.setLineWidth(1);
        mLetterSegmentationItemDecoration.setTextColor(ContextCompat.getColor(mContext, R.color.divider_color_black));
        mLetterSegmentationItemDecoration.setTextSize(AndroidUtil.sp2px(16));

        mLinearLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        mRvContact.setLayoutManager(mLinearLayoutManager);
        mRvContact.setAdapter(mContactAdapter);
        mRvContact.setHasFixedSize(true);
        mRvContact.addItemDecoration(mLetterSegmentationItemDecoration);
        mRvContact.addOnScrollListener(mAutoEnableOverScrollListener);
        mRvContact.addOnItemTouchListener(mOnRecyclerViewItemClickListener);

        mRvSearchContact.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mRvSearchContact.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mRvSearchContact.setRecycledViewPool(mRvContact.getRecycledViewPool());
        mRvSearchContact.setAdapter(mSearchAdapter);
        mRvSearchContact.addOnScrollListener(new AutoCloseKeyboardScrollListener(getActivity()));

        mLlContactOperation.setOnClickListener(mOnContactOperationClick);
        mLlGroup.setOnClickListener(mOnGroupClick);

        mIndexBarView.setSelectedTextColor(ContextCompat.getColor(mContext, R.color.text_secondary_color_black));
        mIndexBarView.setOnTouchSelectedListener(mIndexBarSelectedListener);

        mFBtnAdd.setOnClickListener(mOnAddNewContactClick);

        mContactAdapter.setHeaderView(mHeaderView);
        setOverflowMenu();

        BackPressedReceive.registerBackPressedListener(mBackPressedListener);
    }


    private void setSearchBar() {
        final int searchPopupWindowWidth = (int) (AndroidUtil.getScreenWidth() - AndroidUtil.dip2px(32));
        mSearchPopupWindow = new PopupWindow(mContext);
        mSearchPopupWindow.setAnimationStyle(-1);
        mSearchPopupWindow.setWidth(searchPopupWindowWidth);
        mSearchPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mSearchPopupWindow.setContentView(mRvSearchContact);
        mSearchPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        mSearchPopupWindow.setElevation(AndroidUtil.dip2px(8));
        mSearchPopupWindow.setOutsideTouchable(true);
        mSearchPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        mSearchPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);


//        mSearchPopupWindow.setAdapter(mSearchAdapter);
//
//        mSearchPopupWindow.setWidth(500);
//        mSearchPopupWindow.setHeight(500);
//        mSearchPopupWindow.setModal(false);
//        mSearchPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                mOnRecyclerViewItemClickListener.onItemClick(position + 1, null);
//                // mSearchView.setText(null);
//            }
//        });


        mToolbar.inflateMenu(R.menu.menu_contact_list);
        MenuItem searchItem = mToolbar.getMenu().findItem(R.id.ContactList_Search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                mSearchHandler.removeCallbacksAndMessages(null);
                mSearchHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(newText)) {
                            if (mSearchAdapter.setFilterText(newText) > 0) {
                                if (!mSearchPopupWindow.isShowing()) {
                                    mSearchPopupWindow.showAsDropDown(mToolbar, (mToolbar.getWidth() - searchPopupWindowWidth) / 2, 0, Gravity.START);
                                }
                            } else {
                                mSearchPopupWindow.dismiss();
                            }
                        } else {
                            mSearchAdapter.setFilterText(null);
                            mSearchPopupWindow.dismiss();
                        }
                    }
                }, 250);
                return false;
            }
        });
    }

    private void setOverflowMenu() {
        mContactMenu.setWidth((int) AndroidUtil.dip2px(128));
        mContactMenu.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(mContext, R.color.theme_background_color_white)));
        mContactMenu.setElevation(AndroidUtil.dip2px(2));
        mContactMenu.inflate(R.menu.menu_contact_overflow);
        mContactMenu.setOnMenuItemClickListener(new OverflowPopupMenu.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(int position, int menuID) {
                int index = (int) mRvContact.getTag();
                if (index < mContactList.size()) {
                    switch (menuID) {
                        case R.id.ContactMenu_UpdateRemarkInfo:
                            Intent intent = new Intent(mContext, RemarkInfoActivity.class);
                            intent.putExtra(RemarkInfoActivity.INTENT_EXTRA_CONTACT, mContactList.get(index));
                            startActivityForResult(intent, 0);
                            break;
                    }
                }
            }
        });
    }


    @Override
    protected void onFirstVisible() {
        mPresenter.loadUnreadCount();
        mPresenter.loadAllContact();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSearchView.clearFocus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BackPressedReceive.unregisterBackPressedListener(mBackPressedListener);
        mSearchHandler.removeCallbacksAndMessages(null);
    }

    private final BackPressedReceive.BackPressedListener mBackPressedListener = new BackPressedReceive.BackPressedListener() {

        @Override
        public boolean onBackPressed(String initiator) {
            if (HomeActivity.class.getSimpleName().equals(initiator)) {
                if (mSearchPopupWindow.isShowing()) {
                    mSearchPopupWindow.dismiss();
                    return true;
                }
            }
            return false;
        }
    };

    private final View.OnClickListener mOnContactOperationClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(mContext, ContactOperationActivity.class));
        }
    };

    private final View.OnClickListener mOnGroupClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(mContext, GroupListActivity.class));
        }
    };

    private final View.OnClickListener mOnAddNewContactClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(mContext, FindNewContactActivity.class));
        }
    };

    private final OnRecyclerViewItemClickListener mOnRecyclerViewItemClickListener = new OnRecyclerViewItemClickListener() {

        @Override
        public void onItemClick(final int position, RecyclerView.ViewHolder viewHolder) {
            if (position == 0 && mContactAdapter.isHasHeaderView()) {
                return;
            }
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(mContext, ContactProfileActivity.class);
                    intent.putExtra(ContactProfileActivity.INTENT_EXTRA_CONTACT_ID, mContactList.get(position - 1).getUserProfile().getUserID());
                    startActivity(intent);
                }
            });

        }

        @Override
        public void onItemLongClick(int position, RecyclerView.ViewHolder viewHolder, float touchX, float touchY) {
            if (position == 0 && mContactAdapter.isHasHeaderView()) {
                return;
            }
            mRvContact.setTag(position - 1);
            OverflowMenuShowHelper.show(viewHolder.itemView, mContactMenu, mRvContact.getHeight(), (int) touchX, (int) touchY);
        }
    };


    private final IndexBarView.OnTouchSelectedListener mIndexBarSelectedListener = new IndexBarView.OnTouchSelectedListener() {
        @Override
        public void onSelected(int position, String text) {
            final int scrollPosition = mContactAdapter.findPositionByLetter(text);
            if (scrollPosition >= 0) {
                mRvContact.scrollToPosition(scrollPosition);
                mRvContact.post(new Runnable() {
                    @Override
                    public void run() {
                        int firstPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
                        if (scrollPosition > firstPosition) {
                            View childView = mRvContact.getChildAt(scrollPosition - firstPosition);
                            int scrollY = childView.getTop() - mLetterSegmentationItemDecoration.getSpace();
                            mRvContact.scrollBy(0, scrollY);
                        }
                    }
                });
            }
            if (mFBtnAdd.getTag() == null) {
                AnimationUtil.scaleAnim(mFBtnAdd, 0, 0, 300);
                AnimationUtil.scaleAnim(mTvIndexBarHint, 1f, 1f, 300);
                mFBtnAdd.setTag(true);
            }
            mTvIndexBarHint.setText(text);
            mAutoEnableOverScrollListener.setEnableOverScroll(false);

        }

        @Override
        public void onCancelSelected() {
            mFBtnAdd.setTag(null);
            AnimationUtil.scaleAnim(mTvIndexBarHint, 0, 0, 250);
            AnimationUtil.scaleAnim(mFBtnAdd, 1f, 1f, 250);
            mAutoEnableOverScrollListener.setEnableOverScroll(true);
        }

        @Override
        public void onMove(int offsetPixelsY) {
            int startOffset = mTvIndexBarHint.getHeight() / 2;
            if (startOffset > offsetPixelsY) {
                mTvIndexBarHint.setTranslationY(0);
            } else if (offsetPixelsY > mIndexBarView.getHeight() - startOffset) {
                mTvIndexBarHint.setTranslationY(mIndexBarView.getHeight() - startOffset * 2);
            } else {
                mTvIndexBarHint.setTranslationY(offsetPixelsY - startOffset);
            }
        }
    };

    @Override
    public ContactListContract.Presenter getPresenter() {
        return new ContactListPresenter();
    }

    @Override
    public void updateUnreadBadge(int unreadCount) {
        if (unreadCount == 0) {
            mBadgeView.setVisibility(View.INVISIBLE);
        } else {
            mBadgeView.setVisibility(View.VISIBLE);
        }
        mBadgeView.setBadgeNumber(unreadCount);
    }

    @Override
    public void updateContactItem(ContactBean contactBean) {
        int updatePosition = mContactList.indexOf(contactBean);
        if (updatePosition != -1) {
            mContactList.set(updatePosition, contactBean);
            mContactAdapter.notifyItemChangedEx(updatePosition);
        }
    }

    @Override
    public void updateContactListView(DiffUtil.DiffResult diffResult, List<ContactBean> newFriendList) {
        mContactList.clear();
        mContactList.addAll(newFriendList);
        diffResult.dispatchUpdatesTo(new BaseRecyclerViewAdapter.ListUpdateCallback(mContactAdapter));
    }
}
