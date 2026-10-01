package io.github.transpass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 35})
public final class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private EditText input;
    private TextView result;
    private TextView hiddenHint;
    private Button toggleButton;
    private Button copyButton;

    @Before
    public void createActivity() {
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        bindViews();
    }

    @After
    public void destroyActivity() {
        controller.close();
    }

    @Test
    public void startsWithEmptyInputHiddenResultAndDisabledActions() {
        assertEquals("", input.getText().toString());
        assertResultHidden();
        assertFalse(toggleButton.isEnabled());
        assertFalse(copyButton.isEnabled());
    }

    @Test
    public void immediatelyMasksEveryTypedCharacterWithLiteralAsterisks() {
        input.requestFocus();
        String typed = "Пароль9!*";
        for (int i = 0; i < typed.length(); i++) {
            input.append(typed.substring(i, i + 1));
            CharSequence displayed = input.getTransformationMethod()
                    .getTransformation(input.getText(), input);
            assertEquals(i + 1, displayed.length());
            for (int character = 0; character < displayed.length(); character++) {
                assertEquals('*', displayed.charAt(character));
            }
            assertEquals("*********".substring(0, i + 1), displayed.toString());
            assertEquals("*", displayed.subSequence(i, i + 1).toString());
        }
        assertEquals(typed, input.getText().toString());
        assertResultHidden();
    }

    @Test
    public void revealsConvertedTextAndHidesItAgain() {
        input.setText("Привет ЁХЪЖЭБЮ, Latin 123! 🙂");
        assertResultHidden();
        assertTrue(toggleButton.isEnabled());

        toggleButton.performClick();

        assertEquals(View.VISIBLE, result.getVisibility());
        assertEquals("Ghbdtn ~{}:\"<>, Latin 123! 🙂", result.getText().toString());
        assertEquals(View.GONE, hiddenHint.getVisibility());
        assertEquals(activity.getString(R.string.hide_result), toggleButton.getText().toString());

        toggleButton.performClick();

        assertResultHidden();
        assertTrue(copyButton.isEnabled());
    }

    @Test
    public void editingHidesAndDiscardsThePreviouslyRevealedResult() {
        input.setText("пароль");
        toggleButton.performClick();
        assertEquals("gfhjkm", result.getText().toString());

        input.append("Ё");

        assertResultHidden();
        toggleButton.performClick();
        assertEquals("gfhjkm~", result.getText().toString());

        input.setText("");

        assertResultHidden();
        assertFalse(toggleButton.isEnabled());
        assertFalse(copyButton.isEnabled());
    }

    @Test
    public void copiesTheCurrentConversionWhileHiddenAndMarksItSensitive() {
        input.setText("Привет Ёж 123! 🙂");
        assertResultHidden();
        assertTrue(copyButton.isEnabled());

        copyButton.performClick();

        ClipboardManager clipboard = clipboard();
        assertTrue(clipboard.hasPrimaryClip());
        ClipData clip = clipboard.getPrimaryClip();
        assertNotNull(clip);
        assertEquals(1, clip.getItemCount());
        assertEquals("Ghbdtn ~; 123! 🙂", clip.getItemAt(0).getText().toString());
        assertNotNull(clip.getDescription().getExtras());
        assertTrue(clip.getDescription().getExtras()
                .getBoolean("android.content.extra.IS_SENSITIVE"));
        assertResultHidden();

        input.setText("Новый");
        copyButton.performClick();

        assertEquals("Yjdsq", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
        assertResultHidden();
    }

    @Test
    public void emptyInputCannotReplaceTheExistingClipboard() {
        clipboard().setPrimaryClip(ClipData.newPlainText("Existing", "existing clipboard"));

        // Also exercise the handler directly: View.performClick() bypasses touch enablement.
        copyButton.performClick();

        assertEquals("existing clipboard",
                clipboard().getPrimaryClip().getItemAt(0).getText().toString());
        assertResultHidden();
    }

    @Test
    public void pauseHidesTheResultAndStopClearsTheInput() {
        input.setText("пароль");
        toggleButton.performClick();

        controller.pause();

        assertResultHidden();
        assertEquals("пароль", input.getText().toString());

        controller.stop();

        assertResultHidden();
        assertEquals("", input.getText().toString());
        assertFalse(toggleButton.isEnabled());
        assertFalse(copyButton.isEnabled());

        controller.restart().start().resume();

        assertEquals("", input.getText().toString());
        assertResultHidden();
    }

    @Test
    public void recreationDoesNotRestoreInputOrRevealedResult() {
        input.setText("пароль");
        toggleButton.performClick();
        Bundle savedState = new Bundle();
        // Save while the secret is still present, before onStop has cleared it.
        controller.saveInstanceState(savedState).pause().stop().destroy();

        controller = Robolectric.buildActivity(MainActivity.class).setup(savedState);
        bindViews();

        assertEquals("", input.getText().toString());
        assertResultHidden();
        assertFalse(toggleButton.isEnabled());
        assertFalse(copyButton.isEnabled());
    }

    @Test
    public void protectsTheWindowAndDisablesInputSavingAutofillAndLearning() {
        assertTrue((activity.getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SECURE) != 0);
        assertFalse(input.isSaveEnabled());
        assertFalse(result.isSaveEnabled());
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO, input.getImportantForAutofill());
        assertEquals(InputType.TYPE_TEXT_VARIATION_PASSWORD,
                input.getInputType() & InputType.TYPE_MASK_VARIATION);
        assertTrue((input.getImeOptions() & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0);
    }

    @Test
    public void accessibilityRecognizesTheMaskedInputAsAPassword() {
        input.setText("пароль");
        input.requestFocus();
        input.selectAll();

        AccessibilityNodeInfo info = input.createAccessibilityNodeInfo();

        assertTrue(info.isPassword());
        assertEquals("******", info.getText().toString());
        assertFalse(info.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY));
        assertFalse(info.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT));
    }

    private void assertResultHidden() {
        assertEquals(View.GONE, result.getVisibility());
        assertEquals("", result.getText().toString());
        assertEquals(View.VISIBLE, hiddenHint.getVisibility());
        assertEquals(activity.getString(R.string.show_result), toggleButton.getText().toString());
    }

    private ClipboardManager clipboard() {
        return (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private void bindViews() {
        activity = controller.get();
        input = null;
        result = null;
        hiddenHint = null;
        toggleButton = null;
        copyButton = null;
        List<View> views = new ArrayList<>();
        collectViews(activity.findViewById(android.R.id.content), views);
        List<TextView> hiddenTextViews = new ArrayList<>();
        for (View view : views) {
            if (view instanceof EditText) {
                input = (EditText) view;
            } else if (view instanceof Button) {
                Button button = (Button) view;
                if (button.getText().toString().equals(activity.getString(R.string.show_result))) {
                    toggleButton = button;
                } else if (button.getText().toString()
                        .equals(activity.getString(R.string.copy_result))) {
                    copyButton = button;
                }
            } else if (view instanceof TextView) {
                TextView text = (TextView) view;
                if (text.getVisibility() == View.GONE) {
                    hiddenTextViews.add(text);
                } else if (text.getText().toString()
                        .equals(activity.getString(R.string.result_hidden))) {
                    hiddenHint = text;
                }
            }
        }
        assertNotNull(input);
        assertNotNull(toggleButton);
        assertNotNull(copyButton);
        assertNotNull(hiddenHint);
        assertEquals("Exactly one hidden result view", 1, hiddenTextViews.size());
        result = hiddenTextViews.get(0);
    }

    private static void collectViews(View view, List<View> views) {
        views.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int child = 0; child < group.getChildCount(); child++) {
                collectViews(group.getChildAt(child), views);
            }
        }
    }
}
