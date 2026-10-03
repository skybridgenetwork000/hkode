package com.hkode.h3nrican3.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CustomToast {

    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }

    public enum ThemeMode {
        AUTO, DARK, LIGHT
    }

    private static ThemeMode themeMode = ThemeMode.AUTO;

    public static void setThemeMode(ThemeMode mode) {
        themeMode = mode;
    }

    private static final String FONT_TITLE_PATH = "fonts/outfit_bold.ttf";
    private static final String FONT_MESSAGE_PATH = "fonts/outfit_nomal.ttf";

    private static CustomToast currentActiveToast = null;

    private final Activity activity;
    private final String title;
    private final String message;
    private final ToastType type;

    private FrameLayout parentLayout;
    private LinearLayout toastCard;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isDismissed = false;

    private CustomToast(Activity activity, String title, String message, ToastType type) {
        this.activity = activity;
        this.title = title;
        this.message = message;
        this.type = type;
    }

    public static void showSuccess(Activity activity, String title, String message) {
        show(activity, title, message, ToastType.SUCCESS);
    }

    public static void showError(Activity activity, String title, String message) {
        show(activity, title, message, ToastType.ERROR);
    }

    public static void showWarning(Activity activity, String title, String message) {
        show(activity, title, message, ToastType.WARNING);
    }

    public static void showInfo(Activity activity, String title, String message) {
        show(activity, title, message, ToastType.INFO);
    }

    public static void show(Activity activity, String title, String message, ToastType type) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (currentActiveToast != null) {
            currentActiveToast.dismiss(false);
        }

        CustomToast newToast = new CustomToast(activity, title, message, type);
        currentActiveToast = newToast;
        newToast.createAndShowLayout();
    }

    private boolean isDarkMode(Context context) {
        try {
            String saved = ThemeManager.INSTANCE.getSavedTheme(context);
            if (ThemeManager.THEME_DARK.equals(saved)) {
                return true;
            } else if (ThemeManager.THEME_LIGHT.equals(saved)) {
                return false;
            }
        } catch (Exception ignored) {
        }

        switch (themeMode) {
            case DARK:
                return true;
            case LIGHT:
                return false;
            case AUTO:
            default:
                int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    private void createAndShowLayout() {
        ViewGroup rootContainer = activity.findViewById(android.R.id.content);
        if (rootContainer == null) return;

        Context context = activity;
        float density = context.getResources().getDisplayMetrics().density;
        boolean isDark = isDarkMode(context);

        parentLayout = new FrameLayout(context);
        FrameLayout.LayoutParams parentParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        parentParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        parentParams.topMargin = (int) (52 * density);
        parentParams.leftMargin = (int) (16 * density);
        parentParams.rightMargin = (int) (16 * density);
        parentLayout.setLayoutParams(parentParams);

        toastCard = new LinearLayout(context);
        toastCard.setOrientation(LinearLayout.HORIZONTAL);
        toastCard.setGravity(Gravity.TOP);

        int padPx = (int) (16 * density);
        toastCard.setPadding(padPx, padPx, padPx, padPx);

        int maxWidth = (int) (380 * density);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER_HORIZONTAL;
        toastCard.setLayoutParams(cardParams);
        toastCard.setClipToOutline(true);

        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setShape(GradientDrawable.RECTANGLE);
        bgDrawable.setCornerRadius(24 * density);

        if (isDark) {
            bgDrawable.setColor(Color.parseColor("#171717"));
            bgDrawable.setStroke((int) (1 * density), Color.parseColor("#4D333333"));
        } else {
            bgDrawable.setColor(Color.parseColor("#FBFBFC"));
            bgDrawable.setStroke((int) (1 * density), Color.parseColor("#1A000000"));
        }
        toastCard.setBackground(bgDrawable);
        toastCard.setElevation(isDark ? (16 * density) : (10 * density));

        ImageView iconView = new ImageView(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int) (24 * density),
                (int) (24 * density)
        );
        iconParams.rightMargin = (int) (12 * density);
        iconView.setLayoutParams(iconParams);
        iconView.setImageDrawable(new ToastIconDrawable(context, type, isDark));
        toastCard.addView(iconView);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        textContainer.setLayoutParams(textContainerParams);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(isDark ? Color.WHITE : Color.parseColor("#18181B"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        titleView.setLetterSpacing(-0.015f);

        try {
            Typeface customTitleFont = Typeface.createFromAsset(context.getAssets(), FONT_TITLE_PATH);
            titleView.setTypeface(customTitleFont, Typeface.BOLD);
        } catch (Exception e) {
            titleView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        textContainer.addView(titleView);

        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextColor(isDark ? Color.parseColor("#A1A1AA") : Color.parseColor("#52525B"));
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        messageView.setLineSpacing(0, 1.15f);

        try {
            Typeface customMessageFont = Typeface.createFromAsset(context.getAssets(), FONT_MESSAGE_PATH);
            messageView.setTypeface(customMessageFont, Typeface.NORMAL);
        } catch (Exception e) {
            messageView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }

        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        msgParams.topMargin = (int) (4 * density);
        messageView.setLayoutParams(msgParams);
        textContainer.addView(messageView);

        toastCard.addView(textContainer);
        parentLayout.addView(toastCard);
        rootContainer.addView(parentLayout);

        toastCard.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                toastCard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (toastCard.getWidth() > maxWidth) {
                    FrameLayout.LayoutParams kp = (FrameLayout.LayoutParams) toastCard.getLayoutParams();
                    kp.width = maxWidth;
                    toastCard.setLayoutParams(kp);
                }
            }
        });

        parentLayout.setAlpha(0f);
        parentLayout.setTranslationY(-200 * density);
        parentLayout.setScaleX(0.7f);
        parentLayout.setScaleY(0.7f);

        parentLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss(true);
            }
        }, 4000);
    }

    private void dismiss(boolean animate) {
        if (isDismissed) return;
        isDismissed = true;

        if (currentActiveToast == this) {
            currentActiveToast = null;
        }

        if (parentLayout == null || parentLayout.getParent() == null) return;

        if (animate) {
            float density = activity.getResources().getDisplayMetrics().density;
            parentLayout.animate()
                    .alpha(0f)
                    .translationY(-100 * density)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            cleanup();
                        }
                    }).start();
        } else {
            cleanup();
        }
    }

    private void cleanup() {
        handler.removeCallbacksAndMessages(null);
        if (parentLayout != null && parentLayout.getParent() != null) {
            ((ViewGroup) parentLayout.getParent()).removeView(parentLayout);
        }
    }

    private static class ToastIconDrawable extends Drawable {
        private final Paint paint;
        private final ToastType type;
        private final float density;
        private final boolean isDark;

        public ToastIconDrawable(Context context, ToastType type, boolean isDark) {
            this.type = type;
            this.isDark = isDark;
            this.density = context.getResources().getDisplayMetrics().density;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float cx = bounds.exactCenterX();
            float cy = bounds.exactCenterY();
            float r = 10 * density;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2 * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            switch (type) {
                case SUCCESS:
                    paint.setColor(Color.parseColor(isDark ? "#8C9B21" : "#657302"));
                    canvas.drawCircle(cx, cy, r, paint);
                    canvas.drawLine(cx - (3.5f * density), cy + (0.5f * density), cx - (1f * density), cy + (3f * density), paint);
                    canvas.drawLine(cx - (1f * density), cy + (3f * density), cx + (4.5f * density), cy - (2.5f * density), paint);
                    break;

                case ERROR:
                    paint.setColor(Color.parseColor(isDark ? "#EF4444" : "#DC2626"));
                    canvas.drawCircle(cx, cy, r, paint);
                    canvas.drawLine(cx - (3f * density), cy - (3f * density), cx + (3f * density), cy + (3f * density), paint);
                    canvas.drawLine(cx + (3f * density), cy - (3f * density), cx - (3f * density), cy + (3f * density), paint);
                    break;

                case WARNING:
                    paint.setColor(Color.parseColor(isDark ? "#EAB308" : "#D97706"));
                    canvas.drawCircle(cx, cy, r, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(cx - (0.75f * density), cy - (4f * density), cx + (0.75f * density), cy + (1.5f * density), paint);
                    canvas.drawCircle(cx, cy + (4f * density), 1.0f * density, paint);
                    break;

                case INFO:
                    paint.setColor(Color.parseColor(isDark ? "#3B82F6" : "#2563EB"));
                    canvas.drawCircle(cx, cy, r, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx, cy - (4f * density), 1.0f * density, paint);
                    canvas.drawRect(cx - (0.75f * density), cy - (1.5f * density), cx + (0.75f * density), cy + (4f * density), paint);
                    break;
            }
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
