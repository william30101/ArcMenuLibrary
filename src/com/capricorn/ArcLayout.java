/*
 * Copyright (C) 2012 Capricorn
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

package com.capricorn;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

/**
 * A Layout that arranges its children around its center. The arc can be set by
 * calling {@link #setArc(float, float) setArc()}. You can override the method
 * {@link #onMeasure(int, int) onMeasure()}, otherwise it is always
 * WRAP_CONTENT.
 * 
 * @author Capricorn
 * 
 */
public class ArcLayout extends ViewGroup {
    /**
     * children will be set the same size.
     */
    private int mChildSize;

    public void setmChildSize(int mChildSize) {
		this.mChildSize = mChildSize;
	}

	private int mChildPadding = 5;

    private int mLayoutPadding = 10;

    public static final float DEFAULT_FROM_DEGREES = 270.0f;

    public static final float DEFAULT_TO_DEGREES = 360.0f;

    private float mFromDegrees = DEFAULT_FROM_DEGREES;

    private float mToDegrees = DEFAULT_TO_DEGREES;

    private static final int MIN_RADIUS = 80; // Child改變半徑

    /* the distance between the layout's center and any child's center */
    private int mRadius;

    private boolean mExpanded = false;
    
    static boolean isShrink = false;

    public ArcLayout(Context context) {
        super(context);
    }

    public ArcLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i("shinhua", "ArcLayout(Context context, AttributeSet attrs)");
        
        if (attrs != null) { // Judge XML attributes
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ArcLayout, 0, 0);
            mFromDegrees = a.getFloat(R.styleable.ArcLayout_fromDegrees, DEFAULT_FROM_DEGREES);
            mToDegrees = a.getFloat(R.styleable.ArcLayout_toDegrees, DEFAULT_TO_DEGREES);
            mChildSize = Math.max(a.getDimensionPixelSize(R.styleable.ArcLayout_childSize, 0), 0);
            a.recycle();
        }
    }

    private static int computeRadius(final float arcDegrees, final int childCount, final int childSize,
            final int childPadding, final int minRadius) {
        if (childCount < 2) {
            return minRadius;
        }

        final float perDegrees = arcDegrees / (childCount - 1);
        final float perHalfDegrees = perDegrees / 2;
        final int perSize = childSize + childPadding;

        final int radius = (int) ((perSize / 2) / Math.sin(Math.toRadians(perHalfDegrees)));

        return Math.max(radius, minRadius);
    }

    private static Rect computeChildFrame(final int centerX, final int centerY, final int radius, final float degrees,
            final int size) {

        final double childCenterX = centerX + radius * Math.cos(Math.toRadians(degrees));
        final double childCenterY = centerY + radius * Math.sin(Math.toRadians(degrees));

        return new Rect((int) (childCenterX - size / 2), (int) (childCenterY - size / 2),
                (int) (childCenterX + size / 2), (int) (childCenterY + size / 2));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i("shinhua", "onMeasure(int widthMeasureSpec, int heightMeasureSpec)");
    	
        final int radius = mRadius = computeRadius(Math.abs(mToDegrees - mFromDegrees), getChildCount(), mChildSize,
                mChildPadding, MIN_RADIUS);
        final int size = radius * 2 + mChildSize + mChildPadding + mLayoutPadding * 2;

        Log.i("shinhua", "onMeasure size: " + size);
        setMeasuredDimension(size, size);

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(mChildSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mChildSize, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        final int centerX = getWidth() / 2;
//        final int centerY = getHeight() / 2;
    	final int centerX = getWidth() / 2;
        final int centerY = getHeight() * 3 / 4;
        final int radius = mExpanded ? mRadius : 0;
 
        
        Log.i("shinhua", "onLayout(boolean changed, int l, int t, int r, int b)");   
		Log.i("shinhua", "centerX: " + centerX + " centerY: " + centerY
				+ " gw: " + getWidth() + " gh: " + getHeight());
        
		
        final int childCount = getChildCount();
        final float perDegrees = (mToDegrees - mFromDegrees) / (childCount - 1);

        float degrees = mFromDegrees;
        for (int i = 0; i < childCount; i++) {
        	
        	// This line
            Rect frame = computeChildFrame(centerX, centerY, radius, degrees, mChildSize);
        	//Rect frame = computeChildFrame(centerX, centerY, radius, degrees, 10);
            degrees += perDegrees;
            getChildAt(i).layout(frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    /**
     * refers to {@link LayoutAnimationController#getDelayForView(View view)}
     */
    private static long computeStartOffset(final int childCount, final boolean expanded, final int index,
            final float delayPercent, final long duration, Interpolator interpolator) {
        final float delay = delayPercent * duration;
        final long viewDelay = (long) (getTransformedIndex(expanded, childCount, index) * delay);
        final float totalDelay = delay * childCount;

        float normalizedDelay = viewDelay / totalDelay;
        normalizedDelay = interpolator.getInterpolation(normalizedDelay);

        return (long) (normalizedDelay * totalDelay);
    }

    private static int getTransformedIndex(final boolean expanded, final int count, final int index) {
        if (expanded) {
            return count - 1 - index;
        }

        return index;
    }

    private static Animation createExpandAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta,
            long startOffset, long duration, Interpolator interpolator) {
    	
    	// Child item expand Animation 
    	Log.i("shinhua", "createExpandAnimation2");
    	
        //Animation animation = new RotateAndTranslateAnimation(0, toXDelta, 0, toYDelta, 0, 720);
        //Animation animation = new RotateAndTranslateAnimation(0, toXDelta, 0, toYDelta, 0, 0);

//        Animation animation = new TranslateAnimation(0, toXDelta, 0, toYDelta);
//        animation.setStartOffset(startOffset);
//        animation.setDuration(duration);
//        animation.setInterpolator(interpolator);
//        animation.setFillAfter(true);
//
//        return animation;
    	AnimationSet animationSet = new AnimationSet(false);
        animationSet.setFillAfter(true);

		Animation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        animationSet.addAnimation(alphaAnimation);
        
        Animation scaleAnimation = new ScaleAnimation(0, 50, 0, 50,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animationSet.addAnimation(scaleAnimation);
      
        
		Animation animation = new TranslateAnimation(0, toXDelta, 0, toYDelta);
		animation.setStartOffset(startOffset);
		animation.setDuration(duration);
		animation.setInterpolator(interpolator);
		animation.setFillAfter(true);
        animationSet.addAnimation(animation);
        
//        Animation sacleanimation = new ScaleAnimation(0, toXDelta, 0,
//        		toYDelta, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//        
//        animationSet.addAnimation(sacleanimation);
        
        return animationSet;
        
    }

    private static Animation createShrinkAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta,
            long startOffset, long duration, Interpolator interpolator) {
    	
    	// Child item expand Animation 
    	Log.i("shinhua", "CreateShrink");   	
    	
    	
    	AnimationSet animationSet = new AnimationSet(false);
        animationSet.setFillAfter(true);
        //animationSet.setFillAfter(false);

        final long preDuration = duration / 2;
        Animation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        
        rotateAnimation.setStartOffset(startOffset);
        rotateAnimation.setDuration(preDuration);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setFillAfter(true);

        animationSet.addAnimation(rotateAnimation);

        //Animation translateAnimation = new RotateAndTranslateAnimation(0, toXDelta, 0, toYDelta, 360, 720);
        //Animation translateAnimation = new RotateAndTranslateAnimation(0, toXDelta, 0, toYDelta, 0, 0);
        Animation translateAnimation = new TranslateAnimation(0, toXDelta, 0, toYDelta);
        translateAnimation.setStartOffset(startOffset + preDuration);
        translateAnimation.setDuration(duration - preDuration);
        translateAnimation.setInterpolator(interpolator);
        translateAnimation.setFillAfter(true);

        animationSet.addAnimation(translateAnimation);
        
        // Shinhua, add AlphaAnimation - disappear effect
        Animation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setDuration(1000);
        animationSet.addAnimation(alphaAnimation);
        
        isShrink = true;

        return animationSet;
    }

    private void bindChildAnimation(final View child, final int index, final long duration) {
        final boolean expanded = mExpanded;
        final int centerX = getWidth() / 2;
        //final int centerY = getHeight() / 2;
        final int centerY = getHeight() * 3 / 4;
        final int radius = expanded ? 0 : mRadius;

        final int childCount = getChildCount();
        final float perDegrees = (mToDegrees - mFromDegrees) / (childCount - 1);
        Rect frame = computeChildFrame(centerX, centerY, radius, mFromDegrees + index * perDegrees, mChildSize);

        final int toXDelta = frame.left - child.getLeft();
        final int toYDelta = frame.top - child.getTop();

//        Interpolator interpolator = mExpanded ? new AccelerateInterpolator() : new OvershootInterpolator(1.5f);
        Interpolator interpolator = new AccelerateInterpolator();
        
        final long startOffset = computeStartOffset(childCount, mExpanded, index, 0.1f, duration, interpolator);

        
        /* Shrink Animation & Expand Animation */
        Animation animation = mExpanded ? createShrinkAnimation(0, toXDelta, 0, toYDelta, startOffset, duration,
                interpolator) : createExpandAnimation(0, toXDelta, 0, toYDelta, startOffset, duration, interpolator);

        
        
        final boolean isLast = getTransformedIndex(expanded, childCount, index) == childCount - 1;
        
        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isLast) {
                	Log.i("shinhua", "onAnimationEnd");
                    postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            onAllAnimationsEnd();
                        }
                    }, 0);
                    
                }
            }
        });

        child.setAnimation(animation);
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setArc(float fromDegrees, float toDegrees) {
        if (mFromDegrees == fromDegrees && mToDegrees == toDegrees) {
            return;
        }

        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;

        requestLayout();
    }

    public void setChildSize(int size) {
        if (mChildSize == size || size < 0) {
            return;
        }

        mChildSize = size;

        requestLayout();
    }

    public int getChildSize() {
        return mChildSize;
    }

    /**
     * switch between expansion and shrink
     * 
     * @param showAnimation
     */
    public void switchState(final boolean showAnimation) {
        if (showAnimation) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                bindChildAnimation(getChildAt(i), i, 300);
                // bindChildAnimation(ChildItem, childItemSeqence, showAnimationTime);
            }
        }

        mExpanded = !mExpanded;

        if (!showAnimation) {
            requestLayout();
        }
        
        invalidate();
    }

    private void onAllAnimationsEnd() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).clearAnimation();
        }
        if(isShrink){
        	setChildSize(1);
        	isShrink = false;
        }
        requestLayout();
    }   
    
    
}
