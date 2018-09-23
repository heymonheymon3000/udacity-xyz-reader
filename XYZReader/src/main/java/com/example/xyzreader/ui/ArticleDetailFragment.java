package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.support.v4.content.Loader;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
@SuppressLint("SimpleDateFormat")
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private ImageView mPhotoView;
    private Toolbar toolbar;

    private View mFab;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mFab = mRootView.findViewById(R.id.share_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        toolbar = mRootView.findViewById(R.id.toolBar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                getActivityCast().onBackPressed();
            }
        });


        bindViews();

        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {
            CollapsingToolbarLayout collapsingToolbarLayoutView = mRootView.findViewById(R.id.collapsingToolbarLayoutView);
            TextView bylineView = mRootView.findViewById(R.id.article_byline);
            bylineView.setMovementMethod(new LinkMovementMethod());
            TextView bodyView = mRootView.findViewById(R.id.article_body);
            final ImageView mPhotoView = mRootView.findViewById(R.id.photo);

            bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

            if (mCursor != null) {
                mRootView.setAlpha(0);
                mRootView.setVisibility(View.VISIBLE);
                mRootView.animate().alpha(1);
                collapsingToolbarLayoutView.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
                toolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
                Date publishedDate = parsePublishedDate();
                if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                    bylineView.setText(Html.fromHtml(
                            DateUtils.getRelativeTimeSpanString(
                                    publishedDate.getTime(),
                                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL).toString()
                                    + " by <font color='#ffffff'>"
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                    + "</font>"));

                } else {
                    // If date is before 1902, just show the string
                    bylineView.setText(Html.fromHtml(
                            outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                    + "</font>"));

                }
                bylineView.setText(bylineView.getText().toString());
                bylineView.setTextColor(ContextCompat.getColor(getActivityCast().getBaseContext(), R.color.almost_black));

                bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
                ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                        .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                                Bitmap bitmap = imageContainer.getBitmap();
                                if (bitmap != null) {
//                                Palette p = Palette.generate(bitmap, 12);
//                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                    mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                }
                            }

                            @Override
                            public void onErrorResponse(VolleyError volleyError) {

                            }
                        });
            } else {
                mRootView.setVisibility(View.GONE);
                collapsingToolbarLayoutView.setTitle("N/A");
                bylineView.setText("N/A");
                bodyView.setText("N/A");
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
//        bindViews();
    }
}
