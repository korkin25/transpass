package io.github.transpass;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public final class MainActivity extends Activity {
    private static final int MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT;
    private static final int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;

    private EditText input;
    private TextView result;
    private TextView hiddenHint;
    private Button toggleButton;
    private Button copyButton;
    private boolean resultVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(getColor(R.color.background));
        scroll.setSaveEnabled(false);
        scroll.setVerticalScrollBarEnabled(false);

        // Android 15 enforces edge-to-edge for target SDK 35. Older releases fit
        // the content inside opaque system bars through the decor view.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(30), dp(24), dp(32));
        scroll.addView(page, new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        TextView brand = label(R.string.brand, 13, R.color.accent, true);
        brand.setLetterSpacing(0.16f);
        page.addView(brand);

        TextView title = label(R.string.title, 30, R.color.text_primary, true);
        LinearLayout.LayoutParams titleParams = margins(WRAP_CONTENT, WRAP_CONTENT, 14, 0);
        page.addView(title, titleParams);

        TextView description = label(R.string.description, 16, R.color.text_secondary, false);
        description.setLineSpacing(dp(3), 1f);
        page.addView(description, margins(WRAP_CONTENT, WRAP_CONTENT, 10, 0));

        TextView example = label(R.string.example, 16, R.color.accent, false);
        example.setTypeface(Typeface.MONOSPACE);
        example.setPadding(dp(16), dp(12), dp(16), dp(12));
        example.setBackground(roundRect(R.color.example_background, 14, R.color.example_border));
        page.addView(example, margins(MATCH_PARENT, WRAP_CONTENT, 24, 0));

        LinearLayout inputCard = card();
        page.addView(inputCard, margins(MATCH_PARENT, WRAP_CONTENT, 24, 0));
        inputCard.addView(label(R.string.input_label, 17, R.color.text_primary, true));
        inputCard.addView(label(R.string.input_note, 13, R.color.text_secondary, false),
                margins(MATCH_PARENT, WRAP_CONTENT, 4, 0));

        input = new EditText(this);
        input.setHint(R.string.input_hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(20);
        input.setTypeface(Typeface.MONOSPACE);
        input.setLetterSpacing(0.09f);
        input.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        input.setMinHeight(dp(64));
        input.setSingleLine(false);
        input.setMaxLines(5);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        input.setTransformationMethod(new AlwaysMaskedTransformation());
        input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        input.setSaveEnabled(false);
        input.setBackground(roundRect(R.color.field_background, 14, R.color.field_border));
        inputCard.addView(input, margins(MATCH_PARENT, WRAP_CONTENT, 16, 0));

        toggleButton = actionButton(R.string.show_result, true);
        toggleButton.setOnClickListener(view -> {
            if (resultVisible) {
                hideResult();
            } else if (input.length() > 0) {
                resultVisible = true;
                result.setVisibility(View.VISIBLE);
                hiddenHint.setVisibility(View.GONE);
                result.setText(KeyboardLayout.convert(input.getText().toString()));
                toggleButton.setText(R.string.hide_result);
            }
        });
        toggleButton.setMinHeight(dp(56));
        page.addView(toggleButton, margins(MATCH_PARENT, WRAP_CONTENT, 20, 0));

        LinearLayout resultCard = card();
        page.addView(resultCard, margins(MATCH_PARENT, WRAP_CONTENT, 24, 0));
        resultCard.addView(label(R.string.result_label, 17, R.color.text_primary, true));

        hiddenHint = label(R.string.result_hidden, 15, R.color.text_secondary, false);
        resultCard.addView(hiddenHint, margins(MATCH_PARENT, WRAP_CONTENT, 12, 0));

        result = label(R.string.result_hidden, 22, R.color.accent, false);
        result.setText(null);
        result.setTypeface(Typeface.MONOSPACE);
        result.setVisibility(View.GONE);
        result.setSaveEnabled(false);
        result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        resultCard.addView(result, margins(MATCH_PARENT, WRAP_CONTENT, 12, 0));

        copyButton = actionButton(R.string.copy_result, false);
        copyButton.setEnabled(false);
        copyButton.setOnClickListener(view -> copyResult());
        copyButton.setMinHeight(dp(52));
        resultCard.addView(copyButton, margins(MATCH_PARENT, WRAP_CONTENT, 18, 0));

        TextView punctuation = label(R.string.punctuation_note, 13, R.color.text_muted, false);
        punctuation.setLineSpacing(dp(2), 1f);
        page.addView(punctuation, margins(MATCH_PARENT, WRAP_CONTENT, 22, 0));

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                hideResult();
                toggleButton.setEnabled(text.length() > 0);
                copyButton.setEnabled(text.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });
        toggleButton.setEnabled(false);
        setContentView(scroll);
    }

    @Override
    protected void onPause() {
        hideResult();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (input != null) {
            input.setText(null);
        }
        super.onStop();
    }

    private void hideResult() {
        if (result == null) {
            return;
        }
        resultVisible = false;
        result.setText(null);
        result.setVisibility(View.GONE);
        hiddenHint.setVisibility(View.VISIBLE);
        toggleButton.setText(R.string.show_result);
        copyButton.setEnabled(input.length() > 0);
    }

    private void copyResult() {
        if (input.length() == 0) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.clipboard_label),
                KeyboardLayout.convert(input.getText().toString()));
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(roundRect(R.color.card_background, 20, R.color.card_border));
        return card;
    }

    private TextView label(int stringId, int sizeSp, int colorId, boolean bold) {
        TextView text = new TextView(this);
        text.setText(stringId);
        text.setTextColor(getColor(colorId));
        text.setTextSize(sizeSp);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return text;
    }

    private Button actionButton(int stringId, boolean primary) {
        Button button = new Button(this);
        button.setText(stringId);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? getColor(R.color.button_text) : getColor(R.color.accent));
        GradientDrawable shape = roundRect(
                primary ? R.color.accent : R.color.field_background,
                14,
                primary ? R.color.accent : R.color.field_border);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(getColor(R.color.ripple)), shape, null));
        return button;
    }

    private GradientDrawable roundRect(int fillId, int radiusDp, int strokeId) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(fillId));
        background.setCornerRadius(dp(radiusDp));
        background.setStroke(dp(1), getColor(strokeId));
        return background;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int topDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(topDp);
        params.bottomMargin = dp(bottomDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** A password transformation that never shows the latest typed character. */
    private static final class AlwaysMaskedTransformation extends PasswordTransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return new MaskedText(source);
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
                int direction, android.graphics.Rect previouslyFocusedRect) {
        }
    }

    private static final class MaskedText implements CharSequence {
        private final CharSequence source;

        private MaskedText(CharSequence source) {
            this.source = source;
        }

        @Override
        public int length() {
            return source.length();
        }

        @Override
        public char charAt(int index) {
            source.charAt(index); // Preserve CharSequence bounds checks.
            return '*';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new MaskedText(source.subSequence(start, end));
        }

        @Override
        public String toString() {
            char[] mask = new char[length()];
            Arrays.fill(mask, '*');
            return new String(mask);
        }
    }
}
