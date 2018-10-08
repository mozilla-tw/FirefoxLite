package org.mozilla.focus.tabs;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.DrawableUtils;
import org.mozilla.focus.widget.themed.ThemedImageView;
import org.mozilla.focus.widget.themed.ThemedRelativeLayout;
import org.mozilla.focus.widget.themed.ThemedTextView;

import java.text.NumberFormat;

public class TabCounter extends ThemedRelativeLayout {

    private final ThemedImageView box;
    private final ThemedImageView bar;
    private final ThemedTextView text;

    private final AnimatorSet animationSet;
    private int count;
    private float currentTextRatio;
    private ColorStateList menuIconColor;

    private static final int MAX_VISIBLE_TABS = 99;
    private static final String SO_MANY_TABS_OPEN = "∞";
    private static final String DEFAULT_TABS_COUNTER_TEXT = ":)";

    private static final float ONE_DIGIT_SIZE_RATIO = 0.6f;
    private static final float TWO_DIGITS_SIZE_RATIO = 0.5f;

    public TabCounter(Context context) {
        this(context, null);
    }

    public TabCounter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabCounter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        @ColorInt int defaultMenuIconColor = context.getResources().getColor(R.color.colorMenuIconForeground);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TabCounter, defStyle, 0);
        menuIconColor = typedArray.getColorStateList(R.styleable.TabCounter_drawableColor);
        typedArray.recycle();

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.button_tab_counter, this);

        box = findViewById(R.id.counter_box);
        bar = findViewById(R.id.counter_bar);
        text = findViewById(R.id.counter_text);
        text.setText(DEFAULT_TABS_COUNTER_TEXT);
        final int shiftOneDpForDefaultText = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());
        text.setPadding(0, 0, 0, shiftOneDpForDefaultText);

        if (menuIconColor.getDefaultColor() != defaultMenuIconColor) {
            tintDrawables(menuIconColor);
        }

        animationSet = createAnimatorSet();
    }

    public CharSequence getText() {
        return text.getText();
    }

    public void setCountWithAnimation(final int count) {
        // Don't animate from initial state.
        if (this.count == 0) {
            setCount(count);
            return;
        }

        if (this.count == count) {
            return;
        }

        // Don't animate if there are still over MAX_VISIBLE_TABS tabs open.
        if (this.count > MAX_VISIBLE_TABS && count > MAX_VISIBLE_TABS) {
            this.count = count;
            return;
        }

        adjustTextSize(count);

        text.setPadding(0, 0, 0, 0);
        text.setText(formatForDisplay(count));
        this.count = count;

        // Cancel previous animations if necessary.
        if (animationSet.isRunning()) {
            animationSet.cancel();
        }
        // Trigger animations.
        animationSet.start();
    }

    public void setCount(int count) {
        adjustTextSize(count);

        text.setPadding(0, 0, 0, 0);
        text.setText(formatForDisplay(count));
        this.count = count;
    }

    private void tintDrawables(ColorStateList menuIconColor) {
        final Drawable tabCounterBox = DrawableUtils.loadAndTintDrawable(getContext(), R.drawable.tab_counter_box, menuIconColor.getDefaultColor());
        box.setImageDrawable(tabCounterBox);
        box.setImageTintList(menuIconColor);

        final Drawable tabCounterBar = DrawableUtils.loadAndTintDrawable(getContext(), R.drawable.tab_counter_bar, menuIconColor.getDefaultColor());
        bar.setImageDrawable(tabCounterBar);
        bar.setImageTintList(menuIconColor);

        text.setTextColor(menuIconColor);
    }

    private AnimatorSet createAnimatorSet() {
        final AnimatorSet animatorSet = new AnimatorSet();
        createBoxAnimatorSet(animatorSet);
        createBarAnimatorSet(animatorSet);
        createTextAnimatorSet(animatorSet);
        return animatorSet;
    }

    private void createBoxAnimatorSet(@NonNull AnimatorSet animatorSet) {
        // The first animator, fadeout in 33 ms (49~51, 2 frames).
        final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(box, "alpha", 1.0f, 0.0f).setDuration(33);

        // Move up on y-axis, from 0.0 to -5.3 in 50ms, with fadeOut (49~52, 3 frames).
        final ObjectAnimator moveUp1 = ObjectAnimator.ofFloat(box, "translationY", 0.0f, -5.3f).setDuration(50);

        // Move down on y-axis, from -5.3 to -1.0 in 116ms, after moveUp1 (52~59, 7 frames).
        final ObjectAnimator moveDown2 = ObjectAnimator.ofFloat(box, "translationY", -5.3f, -1.0f).setDuration(116);

        // FadeIn in 66ms, with moveDown2 (52~56, 4 frames).
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(box, "alpha", 0.01f, 1.0f).setDuration(66);

        // Move down on y-axis, from -1.0 to 2.7 in 116ms, after moveDown2 (59~66, 7 frames).
        final ObjectAnimator moveDown3 = ObjectAnimator.ofFloat(box, "translationY", -1.0f, 2.7f).setDuration(116);

        // Move up on y-axis, from 2.7 to 0 in 133ms, after moveDown3 (66~74, 8 frames).
        final ObjectAnimator moveUp4 = ObjectAnimator.ofFloat(box, "translationY", 2.7f, 0.0f).setDuration(133);

        // Scale up height from 2% to 105% in 100ms, after moveUp1 and delay 16ms (53~59, 6 frames).
        final ObjectAnimator scaleUp1 = ObjectAnimator.ofFloat(box, "scaleY", 0.02f, 1.05f).setDuration(100);
        scaleUp1.setStartDelay(16); // delay 1 frame after moveUp1

        // Scale down height from 105% to 99% in 116ms, after scaleUp1 (59~66, 7 frames).
        final ObjectAnimator scaleDown2 = ObjectAnimator.ofFloat(box, "scaleY", 1.05f, 0.99f).setDuration(116);

        // Scale up height from 99% to 100% in 133ms, after scaleDown2 (66~74, 8 frames).
        final ObjectAnimator scaleUp3 = ObjectAnimator.ofFloat(box, "scaleY", 0.99f, 1.00f).setDuration(133);

        animatorSet.play(fadeOut).with(moveUp1);
        animatorSet.play(moveUp1).before(moveDown2);
        animatorSet.play(moveDown2).with(fadeIn);
        animatorSet.play(moveDown2).before(moveDown3);
        animatorSet.play(moveDown3).before(moveUp4);

        animatorSet.play(moveUp1).before(scaleUp1);
        animatorSet.play(scaleUp1).before(scaleDown2);
        animatorSet.play(scaleDown2).before(scaleUp3);
    }

    private void createBarAnimatorSet(@NonNull AnimatorSet animatorSet) {
        final Animator firstAnimator = animatorSet.getChildAnimations().get(0);

        // Move up on y-axis, from 0 to -7.0 in 100ms, with firstAnimator (49~55, 6 frames).
        final ObjectAnimator moveUp1 = ObjectAnimator.ofFloat(bar, "translationY", 0.0f, -7.0f).setDuration(100);

        // Fadeout in 66ms, after firstAnimator with delay 32ms (54~58, 4 frames).
        final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(bar, "alpha", 1.0f, 0.0f).setDuration(66);
        fadeOut.setStartDelay(16 * 3); // delay 3 frames after firstAnimator

        // Move down on y-axis, from -7.0 to 0 in 16ms, after fadeOut (58~59 1 frame).
        final ObjectAnimator moveDown2 = ObjectAnimator.ofFloat(bar, "translationY", -7.0f, 0.0f).setDuration(16);

        // Scale up width from 31% to 100% in 166ms, after moveDown2 with delay 176ms (70~80, 10 frames).
        final ObjectAnimator scaleUp1 = ObjectAnimator.ofFloat(bar, "scaleX", 0.31f, 1.0f).setDuration(166);
        scaleUp1.setStartDelay(16 * 11); // delay 11 frames after moveDown2

        // FadeIn in 166ms, with scaleUp1 (70~80, 10 frames).
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(bar, "alpha", 0.0f, 1.0f).setDuration(166);
        fadeIn.setStartDelay(16 * 11); // delay 11 frames after moveDown2

        animatorSet.play(firstAnimator).with(moveUp1);
        animatorSet.play(firstAnimator).before(fadeOut);
        animatorSet.play(fadeOut).before(moveDown2);

        animatorSet.play(moveDown2).before(scaleUp1);
        animatorSet.play(scaleUp1).with(fadeIn);
    }

    private void createTextAnimatorSet(@NonNull AnimatorSet animatorSet) {
        final Animator firstAnimator = animatorSet.getChildAnimations().get(0);

        // Fadeout in 100ms, with firstAnimator (49~51, 2 frames).
        final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(text, "alpha", 1.0f, 0.0f).setDuration(33);

        // FadeIn in 66 ms, after fadeOut with delay 96ms (57~61, 4 frames).
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(text, "alpha", 0.0f, 1.0f).setDuration(66);
        fadeIn.setStartDelay(16 * 6); // delay 6 frames after fadeOut

        // Move down on y-axis, from 0 to 4.4 in 66ms, with fadeIn (57~61, 4 frames).
        final ObjectAnimator moveDown = ObjectAnimator.ofFloat(text, "translationY", 0.0f, 4.4f).setDuration(66);
        moveDown.setStartDelay(16 * 6); // delay 6 frames after fadeOut

        // Move up on y-axis, from 0 to 4.4 in 66ms, after moveDown (61~69, 8 frames).
        final ObjectAnimator moveUp = ObjectAnimator.ofFloat(text, "translationY", 4.4f, 0.0f).setDuration(66);

        animatorSet.play(firstAnimator).with(fadeOut);
        animatorSet.play(fadeOut).before(fadeIn);
        animatorSet.play(fadeIn).with(moveDown);
        animatorSet.play(moveDown).before(moveUp);
    }

    private String formatForDisplay(int count) {
        if (count > MAX_VISIBLE_TABS) {
            return SO_MANY_TABS_OPEN;
        }
        return NumberFormat.getInstance().format(count);
    }

    private void adjustTextSize(int newCount) {
        final float newRatio = (newCount <= MAX_VISIBLE_TABS && newCount >= 10) ? TWO_DIGITS_SIZE_RATIO : ONE_DIGIT_SIZE_RATIO;

        if (newRatio != currentTextRatio) {
            currentTextRatio = newRatio;
            text.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    text.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    final int sizeInPixel = (int) (box.getWidth() * newRatio);
                    if (sizeInPixel > 0) {
                        // Only apply the size when we calculate a valid value.
                        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeInPixel);
                    }
                }
            });
        }
    }

    @Override
    public void setNightMode(boolean isNight) {
        super.setNightMode(isNight);
        tintDrawables(menuIconColor);
        bar.setNightMode(isNight);
        box.setNightMode(isNight);
        text.setNightMode(isNight);
    }
}
