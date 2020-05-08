package com.fxn.pix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxn.adapters.InstantImageAdapter;
import com.fxn.adapters.MainImageAdapter;
import com.fxn.interfaces.OnSelectionListener;
import com.fxn.interfaces.WorkFinish;
import com.fxn.modals.Img;
import com.fxn.utility.Constants;
import com.fxn.utility.HeaderItemDecoration;
import com.fxn.utility.ImageVideoFetcher;
import com.fxn.utility.PermUtil;
import com.fxn.utility.Utility;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class BrowseGallery extends AppCompatActivity implements View.OnTouchListener {

    private static final int sBubbleAnimDuration = 1000;
    private static final int sScrollbarHideDelay = 1000;
    private static final String OPTIONS = "options";
    private static final int sTrackSnapRange = 5;
    public static String IMAGE_RESULTS = "image_results";
    public static float TOPBAR_HEIGHT;
    private static int maxVideoDuration = 40000;
    private static ImageVideoFetcher imageVideoFetcher;
    private int status_bar_height = 0;
    private int BottomBarHeight = 0;
    private int colorPrimaryDark;
    private float zoom = 0.0f;
    private float dist = 0.0f;
    private Handler handler = new Handler();
    private FastScrollStateChangeListener mFastScrollStateChangeListener;
    private RecyclerView recyclerView, instantRecyclerView;
    private BottomSheetBehavior mBottomSheetBehavior;
    private InstantImageAdapter initaliseadapter;
    private View status_bar_bg, mScrollbar, topbar, bottomButtons, sendButton;
    private TextView mBubbleView, img_count;
    private ImageView mHandleView, selection_back, selection_check;
    private ProgressBar video_counter_progressbar = null;
    private ViewPropertyAnimator mScrollbarAnimator;
    private ViewPropertyAnimator mBubbleAnimator;
    private Set<Img> selectionList = new HashSet<>();
    private Runnable mScrollbarHider = new Runnable() {
        @Override
        public void run() {
            hideScrollbar();
        }
    };
    private MainImageAdapter mainImageAdapter;
    private float mViewHeight;
    private boolean mHideScrollbar = true;
    private boolean LongSelection = false;
    private Options options = null;
    private TextView selection_count;
    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (!mHandleView.isSelected() && recyclerView.isEnabled()) {
                setViewPositions(getScrollProportion(recyclerView));
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (recyclerView.isEnabled()) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        handler.removeCallbacks(mScrollbarHider);
                        if (mScrollbar.getVisibility() != View.VISIBLE) {
                            Utility.cancelAnimation(mScrollbarAnimator);
                            if (!Utility.isViewVisible(mScrollbar) && (recyclerView.computeVerticalScrollRange()
                                    - mViewHeight > 0)) {

                                mScrollbarAnimator = Utility.showScrollbar(mScrollbar, BrowseGallery.this);
                            }
                        }
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        if (mHideScrollbar && !mHandleView.isSelected()) {
                            handler.postDelayed(mScrollbarHider, sScrollbarHideDelay);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private OnSelectionListener onSelectionListener = new OnSelectionListener() {
        @Override
        public void onClick(Img img, View view, int position) {
            if (LongSelection) {
                if (selectionList.contains(img)) {
                    selectionList.remove(img);
                    initaliseadapter.select(false, position);
                    mainImageAdapter.select(false, position);
                } else {
                    if (options.getCount() <= selectionList.size()) {
                        Toast.makeText(BrowseGallery.this,
                                String.format(getResources().getString(R.string.selection_limiter_pix),
                                        selectionList.size()), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    img.setPosition(position);
                    selectionList.add(img);
                    initaliseadapter.select(true, position);
                    mainImageAdapter.select(true, position);
                }
                if (selectionList.size() == 0) {
                    LongSelection = false;
                    selection_check.setVisibility(View.VISIBLE);
                    DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
                    topbar.setBackgroundColor(Color.parseColor("#ffffff"));
                    Animation anim = new ScaleAnimation(
                            1f, 0f, // Start and end values for the X axis scaling
                            1f, 0f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(300);
                    anim.setAnimationListener(new Animation.AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            sendButton.setVisibility(View.GONE);
                            sendButton.clearAnimation();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    sendButton.startAnimation(anim);
                }
                selection_count.setText(selectionList.size() + " " +
                        getResources().getString(R.string.pix_selected));
                img_count.setText(String.valueOf(selectionList.size()));
            } else {
                img.setPosition(position);
                selectionList.add(img);
                returnObjects();
                DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
                topbar.setBackgroundColor(Color.parseColor("#ffffff"));
            }
        }

        @Override
        public void onLongClick(Img img, View view, int position) {
            if (options.getCount() > 1) {
                Utility.vibe(BrowseGallery.this, 50);
                LongSelection = true;
                if ((selectionList.size() == 0) && (mBottomSheetBehavior.getState()
                        != BottomSheetBehavior.STATE_EXPANDED)) {
                    sendButton.setVisibility(View.VISIBLE);
                    Animation anim = new ScaleAnimation(
                            0f, 1f, // Start and end values for the X axis scaling
                            0f, 1f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(300);
                    sendButton.startAnimation(anim);
                }
                if (selectionList.contains(img)) {
                    selectionList.remove(img);
                    initaliseadapter.select(false, position);
                    mainImageAdapter.select(false, position);
                } else {
                    if (options.getCount() <= selectionList.size()) {
                        Toast.makeText(BrowseGallery.this,
                                String.format(getResources().getString(R.string.selection_limiter_pix),
                                        selectionList.size()), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    img.setPosition(position);
                    selectionList.add(img);
                    initaliseadapter.select(true, position);
                    mainImageAdapter.select(true, position);
                }
                selection_check.setVisibility(View.GONE);
                topbar.setBackgroundColor(colorPrimaryDark);
                selection_count.setText(selectionList.size() + " " + getResources().getString(R.string.pix_selected));
                img_count.setText(String.valueOf(selectionList.size()));
                DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
            }
        }
    };
    private int video_counter_progress = 0;

    public static void start(final Fragment context, final Options options) {
        PermUtil.checkForCamaraWritePermissions(context, new WorkFinish() {
            @Override
            public void onWorkFinish(Boolean check) {
                Intent i = new Intent(context.getActivity(), BrowseGallery.class);
                i.putExtra(OPTIONS, options);
                context.startActivityForResult(i, options.getRequestCode());
            }
        });
    }

    public static void start(Fragment context, int requestCode) {
        start(context, Options.init().setRequestCode(requestCode).setCount(1));
    }

    public static void start(final FragmentActivity context, final Options options) {
        PermUtil.checkForCamaraWritePermissions(context, new WorkFinish() {
            @Override
            public void onWorkFinish(Boolean check) {
                Intent i = new Intent(context, BrowseGallery.class);
                i.putExtra(OPTIONS, options);
                context.startActivityForResult(i, options.getRequestCode());
            }
        });
    }

    public static void start(final FragmentActivity context, int requestCode) {
        start(context, Options.init().setRequestCode(requestCode).setCount(1));
    }

    private void hideScrollbar() {
        float transX = getResources().getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding_end);
        mScrollbarAnimator = mScrollbar.animate().translationX(transX).alpha(0f)
                .setDuration(Constants.sScrollbarAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mScrollbar.setVisibility(View.GONE);
                        mScrollbarAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mScrollbar.setVisibility(View.GONE);
                        mScrollbarAnimator = null;
                    }
                });
    }

    public void returnObjects() {
        ArrayList<String> list = new ArrayList<>();
        for (Img i : selectionList) {
            list.add(i.getUrl());
            // Log.e("BrowseGallery images", "img " + i.getUrl());
        }
        Intent resultIntent = new Intent();
        resultIntent.putStringArrayListExtra(IMAGE_RESULTS, list);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utility.setupStatusBarHidden(this);
        Utility.hideStatusBar(this);
        setContentView(R.layout.activity_main_lib);
        initialize();
    }

    private void initialize() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        Utility.getScreenSize(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        try {
            options = (Options) getIntent().getSerializableExtra(OPTIONS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        maxVideoDuration = options.getVideoDurationLimitinSeconds() * 1000; //conversion in  milli seconds

        ((TextView) findViewById(R.id.message_bottom)).setText(options.isExcludeVideos() ? R.string.pix_bottom_message_without_video : R.string.pix_bottom_message_with_video);
        status_bar_height = Utility.getStatusBarSizePort(BrowseGallery.this);
        setRequestedOrientation(options.getScreenOrientation());
        colorPrimaryDark =
                ResourcesCompat.getColor(getResources(), R.color.colorPrimaryPix, getTheme());
        zoom = 0.0f;
        topbar = findViewById(R.id.topbar);
        video_counter_progressbar = findViewById(R.id.video_pbr);
        selection_count = findViewById(R.id.selection_count);
        selection_back = findViewById(R.id.selection_back);
        selection_check = findViewById(R.id.selection_check);
        selection_check.setVisibility((options.getCount() > 1) ? View.VISIBLE : View.GONE);
        sendButton = findViewById(R.id.sendButton);
        img_count = findViewById(R.id.img_count);
        mBubbleView = findViewById(R.id.fastscroll_bubble);
        mHandleView = findViewById(R.id.fastscroll_handle);
        mScrollbar = findViewById(R.id.fastscroll_scrollbar);
        mScrollbar.setVisibility(View.GONE);
        mBubbleView.setVisibility(View.GONE);
        bottomButtons = findViewById(R.id.bottomButtons);
        TOPBAR_HEIGHT = Utility.convertDpToPixel(56, BrowseGallery.this);
        status_bar_bg = findViewById(R.id.status_bar_bg);
        status_bar_bg.getLayoutParams().height = status_bar_height;
        status_bar_bg.setTranslationY(-1 * status_bar_height);
        status_bar_bg.requestLayout();
        instantRecyclerView = findViewById(R.id.instantRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        instantRecyclerView.setLayoutManager(linearLayoutManager);
        initaliseadapter = new InstantImageAdapter(this);
        initaliseadapter.addOnSelectionListener(onSelectionListener);
        instantRecyclerView.setAdapter(initaliseadapter);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.addOnScrollListener(mScrollListener);
        FrameLayout mainFrameLayout = findViewById(R.id.mainFrameLayout);
        CoordinatorLayout main_content = findViewById(R.id.main_content);
        BottomBarHeight = Utility.getSoftButtonsBarSizePort(this);
        FrameLayout.LayoutParams lp1 =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        lp1.setMargins(0, status_bar_height, 0, 0);
        main_content.setLayoutParams(lp1);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) sendButton.getLayoutParams();
        layoutParams.setMargins(0, 0, (int) (Utility.convertDpToPixel(16, this)),
                (int) (Utility.convertDpToPixel(174, this)));
        sendButton.setLayoutParams(layoutParams);
        mainImageAdapter = new MainImageAdapter(this);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, MainImageAdapter.SPAN_COUNT);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mainImageAdapter.getItemViewType(position) == MainImageAdapter.HEADER) {
                    return MainImageAdapter.SPAN_COUNT;
                }
                return 1;
            }
        });
        recyclerView.setLayoutManager(mLayoutManager);
        mainImageAdapter.addOnSelectionListener(onSelectionListener);
        recyclerView.setAdapter(mainImageAdapter);
        recyclerView.addItemDecoration(new HeaderItemDecoration(this, mainImageAdapter));
        mHandleView.setOnTouchListener(this);

        onClickMethods();

        if ((options.getPreSelectedUrls().size()) > options.getCount()) {
            int large = options.getPreSelectedUrls().size() - 1;
            int small = options.getCount();
            for (int i = large; i > (small - 1); i--) {
                options.getPreSelectedUrls().remove(i);
            }
        }
        DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
        updateImages();
        mBottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_HIDEABLE);
        mBottomSheetBehavior.setHideable(false);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        Utility.manipulateVisibility(BrowseGallery.this, 1, findViewById(R.id.arrow_up),
                instantRecyclerView, recyclerView, status_bar_bg,
                topbar, bottomButtons, sendButton, LongSelection);
        Utility.showScrollbar(mScrollbar, BrowseGallery.this);
        mainImageAdapter.notifyDataSetChanged();
        mViewHeight = mScrollbar.getMeasuredHeight();
        handler.post(new Runnable() {
            @Override
            public void run() {
                setViewPositions(getScrollProportion(recyclerView));
            }
        });
        sendButton.setVisibility(View.GONE);
    }

    private void onClickMethods() {
        findViewById(R.id.selection_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnObjects();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnObjects();
            }
        });
        selection_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        selection_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topbar.setBackgroundColor(colorPrimaryDark);
                selection_count.setText(getResources().getString(R.string.pix_tap_to_select));
                img_count.setText(String.valueOf(selectionList.size()));
                DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
                LongSelection = true;
                selection_check.setVisibility(View.GONE);
            }
        });
    }

    private void updateImages() {
        mainImageAdapter.clearList();
        Cursor cursor = Utility.getImageVideoCursor(BrowseGallery.this, options.isExcludeVideos());
        if (cursor == null) {
            return;
        }
        ArrayList<Img> INSTANTLIST = new ArrayList<>();
        String header = "";
        int limit = 100;
        if (cursor.getCount() < limit) {
            limit = cursor.getCount() - 1;
        }
        int data = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
        int mediaType = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE);
        int contentUrl = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
        //int videoDate = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);
        int imageDate = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);
        Calendar calendar;
        int pos = 0;
        for (int i = 0; i < limit; i++) {
            cursor.moveToNext();
            Uri path = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "" + cursor.getInt(contentUrl));
            calendar = Calendar.getInstance();
            int finDate = imageDate; // mediaType == 1 ? imageDate : videoDate;
            calendar.setTimeInMillis(cursor.getLong(finDate) * 1000);
            //Log.e("time",i+"->"+new SimpleDateFormat("hh:mm:ss dd/MM/yyyy",Locale.ENGLISH).format(calendar.getTime()));
            String dateDifference = Utility.getDateDifference(BrowseGallery.this, calendar);
            if (!header.equalsIgnoreCase("" + dateDifference)) {
                header = "" + dateDifference;
                pos += 1;
                INSTANTLIST.add(new Img("" + dateDifference, "", "", "", cursor.getInt(mediaType)));
            }
            Img img =
                    new Img("" + header, "" + path, cursor.getString(data), "" + pos, cursor.getInt(mediaType));
            img.setPosition(pos);
            if (options.getPreSelectedUrls().contains(img.getUrl())) {
                img.setSelected(true);
                selectionList.add(img);
            }
            pos += 1;
            INSTANTLIST.add(img);
        }
        if (selectionList.size() > 0) {
            LongSelection = true;
            sendButton.setVisibility(View.VISIBLE);
            Animation anim = new ScaleAnimation(
                    0f, 1f, // Start and end values for the X axis scaling
                    0f, 1f, // Start and end values for the Y axis scaling
                    Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                    Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
            anim.setFillAfter(true); // Needed to keep the result of the animation
            anim.setDuration(300);
            sendButton.startAnimation(anim);
            selection_check.setVisibility(View.GONE);
            topbar.setBackgroundColor(colorPrimaryDark);
            selection_count.setText(selectionList.size() + " " +
                    getResources().getString(R.string.pix_selected));
            img_count.setText(String.valueOf(selectionList.size()));
            DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
        }
        mainImageAdapter.addImageList(INSTANTLIST);
        initaliseadapter.addImageList(INSTANTLIST);
        imageVideoFetcher = new ImageVideoFetcher(BrowseGallery.this) {
            @Override
            protected void onPostExecute(ModelList modelList) {
                super.onPostExecute(modelList);
                mainImageAdapter.addImageList(modelList.getLIST());
                initaliseadapter.addImageList(modelList.getLIST());
                selectionList.addAll(modelList.getSelection());
                if (selectionList.size() > 0) {
                    LongSelection = true;
                    sendButton.setVisibility(View.VISIBLE);
                    Animation anim = new ScaleAnimation(
                            0f, 1f, // Start and end values for the X axis scaling
                            0f, 1f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(300);
                    sendButton.startAnimation(anim);
                    selection_check.setVisibility(View.GONE);
                    topbar.setBackgroundColor(colorPrimaryDark);
                    selection_count.setText(selectionList.size() + " " +
                            getResources().getString(R.string.pix_selected));
                    img_count.setText(String.valueOf(selectionList.size()));
                    DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
                }
            }
        };

        imageVideoFetcher.setStartingCount(pos);
        imageVideoFetcher.header = header;
        imageVideoFetcher.setPreSelectedUrls(options.getPreSelectedUrls());
        imageVideoFetcher.execute(Utility.getImageVideoCursor(BrowseGallery.this, options.isExcludeVideos()));
        cursor.close();
        setBottomSheetBehavior();
    }

    private void setBottomSheetBehavior() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        ScreenUtils screenUtils = new ScreenUtils(this);
        mBottomSheetBehavior.setPeekHeight(screenUtils.getHeight());
    }

    private float getScrollProportion(RecyclerView recyclerView) {
        final int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
        final int verticalScrollRange = recyclerView.computeVerticalScrollRange();
        final float rangeDiff = verticalScrollRange - mViewHeight;
        float proportion = (float) verticalScrollOffset / (rangeDiff > 0 ? rangeDiff : 1f);
        return mViewHeight * proportion;
    }

    private void setViewPositions(float y) {
        int handleY = Utility.getValueInRange(0, (int) (mViewHeight - mHandleView.getHeight()),
                (int) (y - mHandleView.getHeight() / 2));
        mBubbleView.setY(handleY + Utility.convertDpToPixel(60, this));
        mHandleView.setY(handleY);
    }

    private void setRecyclerViewPosition(float y) {
        if (recyclerView != null && recyclerView.getAdapter() != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            float proportion;

            if (mHandleView.getY() == 0) {
                proportion = 0f;
            } else if (mHandleView.getY() + mHandleView.getHeight() >= mViewHeight - sTrackSnapRange) {
                proportion = 1f;
            } else {
                proportion = y / mViewHeight;
            }

            int scrolledItemCount = Math.round(proportion * itemCount);
            int targetPos = Utility.getValueInRange(0, itemCount - 1, scrolledItemCount);
            recyclerView.getLayoutManager().scrollToPosition(targetPos);

            if (mainImageAdapter != null) {
                String text = mainImageAdapter.getSectionMonthYearText(targetPos);
                mBubbleView.setText(text);
                if (text.equalsIgnoreCase("")) {
                    mBubbleView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void showBubble() {
        if (!Utility.isViewVisible(mBubbleView)) {
            mBubbleView.setVisibility(View.VISIBLE);
            mBubbleView.setAlpha(0f);
            mBubbleAnimator = mBubbleView
                    .animate()
                    .alpha(1f)
                    .setDuration(sBubbleAnimDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        // adapter required for new alpha value to stick
                    });
            mBubbleAnimator.start();
        }
    }

    private void hideBubble() {
        if (Utility.isViewVisible(mBubbleView)) {
            mBubbleAnimator = mBubbleView.animate().alpha(0f)
                    .setDuration(sBubbleAnimDuration)
                    .setListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mBubbleView.setVisibility(View.GONE);
                            mBubbleAnimator = null;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            super.onAnimationCancel(animation);
                            mBubbleView.setVisibility(View.GONE);
                            mBubbleAnimator = null;
                        }
                    });
            mBubbleAnimator.start();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < mHandleView.getX() - ViewCompat.getPaddingStart(mHandleView)) {
                    return false;
                }
                mHandleView.setSelected(true);
                handler.removeCallbacks(mScrollbarHider);
                Utility.cancelAnimation(mScrollbarAnimator);
                Utility.cancelAnimation(mBubbleAnimator);

                if (!Utility.isViewVisible(mScrollbar) && (recyclerView.computeVerticalScrollRange()
                        - mViewHeight > 0)) {
                    mScrollbarAnimator = Utility.showScrollbar(mScrollbar, BrowseGallery.this);
                }

                if (mainImageAdapter != null) {
                    showBubble();
                }

                if (mFastScrollStateChangeListener != null) {
                    mFastScrollStateChangeListener.onFastScrollStart(this);
                }
            case MotionEvent.ACTION_MOVE:
                final float y = event.getRawY();
                setViewPositions(y - TOPBAR_HEIGHT);
                setRecyclerViewPosition(y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHandleView.setSelected(false);
                if (mHideScrollbar) {
                    handler.postDelayed(mScrollbarHider, sScrollbarHideDelay);
                }
                hideBubble();
                if (mFastScrollStateChangeListener != null) {
                    mFastScrollStateChangeListener.onFastScrollStop(this);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public interface FastScrollStateChangeListener {

        /**
         * Called when fast scrolling begins
         */
        void onFastScrollStart(BrowseGallery fastScroller);

        /**
         * Called when fast scrolling ends
         */
        void onFastScrollStop(BrowseGallery fastScroller);
    }

    @Override
    public void onBackPressed() {

        if (selectionList.size() > 0) {
            for (Img img : selectionList) {
                options.setPreSelectedUrls(new ArrayList<String>());
                mainImageAdapter.getItemList().get(img.getPosition()).setSelected(false);
                mainImageAdapter.notifyItemChanged(img.getPosition());
                initaliseadapter.getItemList().get(img.getPosition()).setSelected(false);
                initaliseadapter.notifyItemChanged(img.getPosition());
            }
            LongSelection = false;
            if (options.getCount() > 1) {
                selection_check.setVisibility(View.VISIBLE);
            }
            DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
            topbar.setBackgroundColor(Color.parseColor("#ffffff"));
            Animation anim = new ScaleAnimation(
                    1f, 0f, // Start and end values for the X axis scaling
                    1f, 0f, // Start and end values for the Y axis scaling
                    Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                    Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
            anim.setFillAfter(true); // Needed to keep the result of the animation
            anim.setDuration(300);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    sendButton.setVisibility(View.GONE);
                    sendButton.clearAnimation();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            sendButton.startAnimation(anim);
            selectionList.clear();
        } else {
            super.onBackPressed();
        }
    }

    public class ScreenUtils {

        Context ctx;
        DisplayMetrics metrics;

        public ScreenUtils(Context ctx) {
            this.ctx = ctx;
            WindowManager wm = (WindowManager) ctx
                    .getSystemService(Context.WINDOW_SERVICE);

            Display display = wm.getDefaultDisplay();
            metrics = new DisplayMetrics();
            display.getMetrics(metrics);

        }

        public int getHeight() {
            return metrics.heightPixels;
        }

        public int getWidth() {
            return metrics.widthPixels;
        }

        public int getRealHeight() {
            return metrics.heightPixels / metrics.densityDpi;
        }

        public int getRealWidth() {
            return metrics.widthPixels / metrics.densityDpi;
        }

        public int getDensity() {
            return metrics.densityDpi;
        }

        public int getScale(int picWidth) {
            Display display
                    = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            int width = display.getWidth();
            Double val = new Double(width) / new Double(picWidth);
            val = val * 100d;
            return val.intValue();
        }
    }
}
