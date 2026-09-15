package zixing.bluetooth.unlocker.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import zixing.bluetooth.unlocker.R;

public final class AppDialogs {
    private AppDialogs() { }
    public static MaterialAlertDialogBuilder builder(Context context) {
        return new MaterialAlertDialogBuilder(context, R.style.UnlockerDialog);
    }
    public static final class Choice {
        public final String title, detail;
        public final boolean selected, destructive;
        public final Runnable action;
        public Choice(String title, String detail, boolean selected, boolean destructive, Runnable action) {
            this.title = title; this.detail = detail; this.selected = selected;
            this.destructive = destructive; this.action = action;
        }
    }
    public static void choices(Context context, String title, String subtitle, List<Choice> choices) {
        int pad = dp(context, 20);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, dp(context, 4), pad, dp(context, 8));
        TextView description = new TextView(context);
        description.setText(subtitle);
        description.setTextColor(context.getColor(R.color.text_second_color_primary));
        description.setTextSize(13);
        description.setPadding(0, 0, 0, dp(context, 8));
        content.addView(description);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(content);
        AlertDialog dialog = builder(context).setTitle(title).setView(scroll).setNegativeButton("关闭", null).create();
        for (Choice choice : choices) {
            MaterialCardView card = new MaterialCardView(context);
            card.setRadius(dp(context, 14));
            card.setCardElevation(0);
            card.setStrokeWidth(dp(context, 1));
            int accent = context.getColor(choice.destructive ? R.color.accent_red : R.color.purple_500);
            card.setStrokeColor(choice.selected ? accent : context.getColor(R.color.dialog_outline));
            card.setCardBackgroundColor(context.getColor(choice.selected ? R.color.dialog_selected : R.color.white));
            card.setRippleColor(ColorStateList.valueOf(context.getColor(R.color.dialog_selected)));
            card.setClickable(true); card.setFocusable(true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(context, 8);
            content.addView(card, params);
            LinearLayout text = new LinearLayout(context);
            text.setOrientation(LinearLayout.VERTICAL);
            text.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
            TextView heading = new TextView(context);
            heading.setText(choice.title + (choice.selected ? "  ✓" : ""));
            heading.setTextSize(16); heading.setTypeface(null, Typeface.BOLD); heading.setTextColor(accent);
            text.addView(heading);
            TextView detail = new TextView(context);
            detail.setText(choice.detail); detail.setTextSize(13);
            detail.setTextColor(context.getColor(R.color.text_second_color_primary));
            detail.setPadding(0, dp(context, 5), 0, 0);
            text.addView(detail); card.addView(text);
            card.setOnClickListener(v -> { dialog.dismiss(); choice.action.run(); });
        }
        dialog.show();
    }
    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
