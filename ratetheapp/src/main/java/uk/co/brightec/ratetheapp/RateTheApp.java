/*
 * Copyright 2016 Brightec Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.brightec.ratetheapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The RateTheApp widget. See readme on github
 *
 * @see <a href="https://github.com/brightec/RateTheApp-Android">github.com/brightec/RateTheApp-Android</a>
 */
public class RateTheApp extends LinearLayout {

    static final String INSTANCE_PREFIX = "ratetheapp";

    private static final int DEFAULT_NUMBER_OF_STARS = 5;
    private static final float DEFAULT_STEP_SIZE = 1f;
    private static final float DEFAULT_RATING = 0.0f;

    private String mInstanceName;
    private String mTitleStr, mMessageStr;
    @StyleRes
    private int mTitleTextAppearanceResId, mMessageTextAppearanceResId;
    @ColorInt
    private Integer mSelectedStarColour, mUnselectedStarColour;
    private RatingBar mRatingBar;
    private int mNumberOfStars = DEFAULT_NUMBER_OF_STARS;
    @DrawableRes
    private int mDrawableResSelected, mDrawableResUnSelected;
    private float mStepSize = DEFAULT_STEP_SIZE;
    private float mDefaultRating = DEFAULT_RATING;
    private boolean mSaveRating;
    private InstanceSettings mInstanceSettings;

    private TextView mTextTitle, mTextMessage;
    private OnUserSelectedRatingListener mOnUserSelectedRatingListener;
    private RatingBar.OnRatingBarChangeListener ratingChangeListener = new RatingBar.OnRatingBarChangeListener() {

        @Override
        public void onRatingChanged(RatingBar ratingBar, final float rating, boolean fromUser) {
            // Save the rating
            if (mSaveRating) {
                mInstanceSettings.saveRating(rating);
            }

            // If a rateChangeListener was provided, call it
            if (mOnUserSelectedRatingListener != null && fromUser) {
                mOnUserSelectedRatingListener.onRatingChanged(RateTheApp.this, rating);
            }
        }
    };

    public RateTheApp(Context context) {
        super(context);
        init();
    }

    public RateTheApp(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttributes(attrs);
        init();
    }

    public RateTheApp(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        loadAttributes(attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RateTheApp(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        loadAttributes(attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void loadAttributes(AttributeSet attrs) {
        loadAttributes(attrs, 0, 0);
    }

    private void loadAttributes(AttributeSet attrs, int defStyleAttr) {
        loadAttributes(attrs, defStyleAttr, 0);
    }

    private void loadAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.RateTheApp, defStyleAttr, defStyleRes);

        // Instance name for this rating bar
        mInstanceName = Utils.getInstanceNameFromRateTheAppName(a.getString(R.styleable
                .RateTheApp_rateTheAppName));

        // Title Text Appearance
        mTitleTextAppearanceResId = a.getResourceId(R.styleable.RateTheApp_rateTheAppTitleTextAppearance
                , R.style.RateTheAppTitleTextAppearance);
        mTitleStr = a.getString(R.styleable.RateTheApp_rateTheAppTitleText);

        // Message Text Appearance
        mMessageTextAppearanceResId = a.getResourceId(R.styleable.RateTheApp_rateTheAppMessageTextAppearance
                , R.style.RateTheAppMessageTextAppearance);
        mMessageStr = a.getString(R.styleable.RateTheApp_rateTheAppMessageText);

        // Stars & Rating
        mNumberOfStars = a.getInt(R.styleable.RateTheApp_rateTheAppNumberOfStars, DEFAULT_NUMBER_OF_STARS);
        mStepSize = a.getFloat(R.styleable.RateTheApp_rateTheAppStepSize, DEFAULT_STEP_SIZE);
        mDefaultRating = a.getFloat(R.styleable.RateTheApp_rateTheAppDefaultRating, DEFAULT_RATING);

        int colorInt = a.getColor(R.styleable.RateTheApp_rateTheAppSelectedStarColor, -1);
        if (colorInt == -1) {
            mSelectedStarColour = null;
        } else {
            mSelectedStarColour = colorInt;
        }
        colorInt = a.getColor(R.styleable.RateTheApp_rateTheAppUnselectedStarColor, -1);
        if (colorInt == -1) {
            mUnselectedStarColour = null;
        } else {
            mUnselectedStarColour = colorInt;
        }

        mDrawableResUnSelected = a.getResourceId(R.styleable
                .RateTheApp_rateTheAppStarUnSelectedDrawable, R.drawable.ic_star_inactive_grey_50dp);
        mDrawableResSelected = a.getResourceId(R.styleable
                .RateTheApp_rateTheAppStarSelectedDrawable, R.drawable.ic_star_active_orange_50dp);

        mSaveRating = a.getBoolean(R.styleable.RateTheApp_rateTheAppSaveRating, true);

        a.recycle();
    }

    private void init() {
        //Create our Settings object
        mInstanceSettings = new InstanceSettings(getContext(), mInstanceName);

        if (!isInEditMode() && !shouldShow()) {
            this.setVisibility(GONE);
            return;
        }

        //Inflate and find all the relevant views
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View rootView = inflater.inflate(R.layout.ratetheapp_layout, this, false);
        mRatingBar = (RatingBar) rootView.findViewById(R.id.rating_bar);
        mTextTitle = (TextView) rootView.findViewById(R.id.text_rating_title);
        mTextMessage = (TextView) rootView.findViewById(R.id.text_rating_message);

        mRatingBar.setNumStars(mNumberOfStars);
        mRatingBar.setStepSize(mStepSize);

        // Initialise the title
        initTitle();

        // Initialise the message
        initMessage();

        // Initialise the stars
        mRatingBar.initStars(mDrawableResSelected, mSelectedStarColour, mDrawableResUnSelected,
                mUnselectedStarColour);


        float rating;
        if (isInEditMode()) {
            //Show some rating for editor
            rating = mDefaultRating;
        } else {
            // Set previously saved rating (else use default rating)
            rating = mInstanceSettings.getSavedRating(-1f);
            if (rating == -1f) {
                rating = mDefaultRating;
            }
        }

        mRatingBar.setRating(rating);

        mRatingBar.setOnRatingBarChangeListener(ratingChangeListener);

        if (!isInEditMode()) {
            // Set the default RateChangeListener
            setOnUserSelectedRatingListener(DefaultOnUserSelectedRatingListener
                    .createDefaultInstance(getContext()));
        }

        addView(rootView);
    }

    private void initTitle() {
        // Hide the title if an empty title text attribute was provided
        if (mTitleStr != null && mTitleStr.isEmpty()) {
            mTextTitle.setVisibility(GONE);
        } else {
            // Set the title text if provided
            if (mTitleStr != null) {
                mTextTitle.setText(mTitleStr);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTextTitle.setTextAppearance(mTitleTextAppearanceResId);
            } else {
                //noinspection deprecation
                mTextTitle.setTextAppearance(getContext(), mTitleTextAppearanceResId);
            }
        }
    }

    private void initMessage() {
        // Hide the message if an empty message text attribute was provided
        if (mMessageStr != null && mMessageStr.isEmpty()) {
            mTextMessage.setVisibility(GONE);
        } else {
            // Set the title text if provided
            if (mMessageStr != null) {
                mTextMessage.setText(mMessageStr);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTextMessage.setTextAppearance(mMessageTextAppearanceResId);
            } else {
                //noinspection deprecation
                mTextMessage.setTextAppearance(getContext(), mMessageTextAppearanceResId);
            }
        }
    }

    /**
     * Retrieve the currently set OnUserSelectedRatingListener
     *
     * @return OnUserSelectedRatingListener
     */
    @SuppressWarnings({"unused"})
    public OnUserSelectedRatingListener getOnUserSelectedRatingListener() {
        return mOnUserSelectedRatingListener;
    }

    /**
     * Set the listener for when the user selects a rating
     * Note: This will not be called if you manually set the rating
     *
     * @param onUserSelectedRatingListener OnUserSelectedRatingListener
     */
    @SuppressWarnings({"unused"})
    public void setOnUserSelectedRatingListener(OnUserSelectedRatingListener onUserSelectedRatingListener) {
        mOnUserSelectedRatingListener = onUserSelectedRatingListener;
    }

    /**
     * Returns a boolean to indicate whether RateTheApp should be shown
     *
     * @return boolean TRUE if should be shown
     */
    @SuppressWarnings({"unused"})
    public boolean shouldShow() {
        return mInstanceSettings.shouldShow();
    }

    /**
     * Set the given instance of RateTheApp to hide permanently
     */
    @SuppressWarnings({"unused"})
    public void hidePermanently() {
        mInstanceSettings.hidePermanently();
    }

    /**
     * Get the rating currently set on the RateTheApp widget
     *
     * @return float
     */
    @SuppressWarnings({"unused"})
    public float getRating() {
        return mRatingBar.getRating();
    }

    /**
     * Set the rating on RateTheApp widget
     * Note: this will not call OnUserSelectedRatingListener
     *
     * @param rating float
     */
    @SuppressWarnings({"unused"})
    public void setRating(float rating) {
        mRatingBar.setRating(rating);
    }

    /**
     * Reset the given instance of RateTheApp widget's rating and visibility.
     */
    @SuppressWarnings({"unused"})
    public void resetWidget() {
        mInstanceSettings.resetWidget();
    }

    /**
     * This method returns the TextView which represents the title in the layout. This method is provided for specific use cases where this library has not provided the exact config needed.
     *
     * @return TextView mTextTitle - The TextView associated with the title
     */
    @SuppressWarnings({"unused"})
    public TextView getTitleTextView() {
        return mTextTitle;
    }

    /**
     * This method returns the TextView which represents the message in the layout. This method is provided for specific use cases where this library has not provided the exact config needed.
     *
     * @return TextView mTextMessage - The TextView associated with the message
     */
    @SuppressWarnings({"unused"})
    public TextView getMessageTextView() {
        return mTextMessage;
    }

    /**
     * Get the settings for this instance of the RateTheApp widget
     *
     * @return InstanceSettings
     */
    @SuppressWarnings({"unused"})
    public InstanceSettings getInstanceSettings() {
        return mInstanceSettings;
    }

    public interface OnUserSelectedRatingListener {
        void onRatingChanged(RateTheApp rateTheApp, float rating);
    }
}