package com.github.uiautomator2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class AdbKeyboard extends InputMethodService {
    private static final String TAG = "AdbKeyboard";
    private BroadcastReceiver mReceiver = null;
    private int imeAction;
    private Keyboard keyboard;
    private KeyboardView inputView;

    private final Handler inputHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingInput = null;

    public enum KeyboardAction {
        ADB_KEYBOARD_SMART_ENTER,
        ADB_KEYBOARD_INPUT_TEXT,
        ADB_KEYBOARD_CLEAR_TEXT,
        ADB_KEYBOARD_SET_TEXT,
        ADB_KEYBOARD_INPUT_KEYCODE,
        ADB_KEYBOARD_EDITOR_CODE,
        ADB_KEYBOARD_GET_CLIPBOARD,
        ADB_KEYBOARD_HIDE,
        ADB_KEYBOARD_SHOW;
    }

    @Override
    public View onCreateInputView() {
        View view = getLayoutInflater().inflate(R.layout.input, null);
        inputView = view.findViewById(R.id.keyboard);
        keyboard = new Keyboard(this, R.xml.keyboard);
        inputView.setKeyboard(keyboard);
        inputView.setOnKeyboardActionListener(new MyKeyboardActionListener());

        // 下滑标题栏也可以隐藏输入法
        view.findViewById(R.id.keyboard_header).setOnTouchListener(new View.OnTouchListener() {
            private float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float threshold = 16 * getResources().getDisplayMetrics().density;
                        if (event.getY() - startY > threshold) {
                            hideInputMethod(AdbKeyboard.this);
                        }
                        return true;
                }
                return true;
            }
        });

        return view;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Input created");

        if (mReceiver == null) {
            IntentFilter filter = new IntentFilter();
            for (KeyboardAction ka: KeyboardAction.values()) {
                filter.addAction(ka.toString());
            }
            mReceiver = new InputMessageReceiver();
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(mReceiver, filter);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Input destroyed");
        cancelPendingInput();
        if (mReceiver != null) {
            try {
                unregisterReceiver(mReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    public class InputMessageReceiver extends BroadcastReceiver {
        private String charSequenceToString(CharSequence input) {
            return input == null ? "" : input.toString();
        }
        private String getClipboardText(Context context) {
            final ClipboardManager cm = (ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) {
                Log.e(TAG, "Cannot get an instance of ClipboardManager");
                return null;
            }
            if (!cm.hasPrimaryClip()) {
                return "";
            }
            final ClipData cd = cm.getPrimaryClip();
            if (cd == null || cd.getItemCount() == 0) {
                return "";
            }
            return charSequenceToString(cd.getItemAt(0).coerceToText(context));
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String resultData = this.handleAction(context, intent);
                setResultCode(Activity.RESULT_OK);
                setResultData(resultData);
            } catch (Exception ex) {
                setResult(Activity.RESULT_CANCELED, "error:" + ex.getMessage(), null);
            }
        }

        public String handleAction(Context context, Intent intent) throws Exception {
            String action = intent.getAction();
            String msgText;
            int code;
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) {
                return action;
            }
            KeyboardAction keyAction = KeyboardAction.valueOf(action);
            Log.i(TAG, "KeyboardAction received:" + keyAction);
            switch (keyAction) {
                case ADB_KEYBOARD_INPUT_TEXT:
                    /* test method
                     * TEXT=$(echo -n "Hello World" | base64)
                     * adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text ${TEXT:-"SGVsbG8gd29ybGQ="}
                     * 可选参数 interval，单位 ms，控制每个字符之间的输入间隔，默认 0
                     * text 推荐传 base64；非合法 base64 时按原文处理，因此也可直接传入中文
                     * adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "$TEXT" --ei interval 100
                     * adb shell am broadcast -a ADB_KEYBOARD_INPUT_TEXT --es text "123你好" --ei interval 100
                     */
                    msgText = intent.getStringExtra("text");
                    if (msgText == null) {
                        return action;
                    }
                    inputText(msgText, Math.max(0, intent.getIntExtra("interval", 0)));
                    break;
                case ADB_KEYBOARD_INPUT_KEYCODE:
                    /* test method
                     * Enter code 66
                     * adb shell am broadcast -a ADB_KEYBOARD_INPUT_KEYCODE --ei code 66
                     */
                    code = intent.getIntExtra("code", -1);
                    if (code != -1) {
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
                    }
                    break;
                case ADB_KEYBOARD_CLEAR_TEXT:
                    clearText();
                    break;
                case ADB_KEYBOARD_SET_TEXT:
                    msgText = intent.getStringExtra("text");
                    if (msgText == null) {
                        msgText = "";
                    }
                    ic.beginBatchEdit();
                    clearText();
                    inputText(msgText, Math.max(0, intent.getIntExtra("interval", 0)));
                    ic.endBatchEdit();
                    break;
                case ADB_KEYBOARD_EDITOR_CODE:
                    code = intent.getIntExtra("code", -1);
                    if (code != -1) {
                        ic.performEditorAction(code);
                    }
                    break;
                case ADB_KEYBOARD_GET_CLIPBOARD:
                    Log.i(TAG, "Getting current clipboard content");
                    final String clipboardContent = getClipboardText(context);
                    if (clipboardContent == null) {
                        throw new Exception("clipboard empty");
                    } else {
                        return Base64.encodeToString(
                                clipboardContent.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                    }
                case ADB_KEYBOARD_HIDE:
                    // https://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard-programmatically#:~:text=You%20can%20force%20Android%20to,window%20containing%20your%20focused%20view.&text=This%20will%20force%20the%20keyboard,want%20to%20pass%20in%20InputMethodManager.
                    hideInputMethod(context);
                    break;
                case ADB_KEYBOARD_SHOW:
                    showInputMethod(context);
                    break;
                case ADB_KEYBOARD_SMART_ENTER:
                    switch(imeAction) {
                        case EditorInfo.IME_ACTION_DONE:
                        case EditorInfo.IME_ACTION_GO:
                        case EditorInfo.IME_ACTION_NEXT:
                        case EditorInfo.IME_ACTION_SEARCH:
                        case EditorInfo.IME_ACTION_SEND:
                        case EditorInfo.IME_ACTION_NONE:
                            ic.performEditorAction(imeAction);
                            return convertActionToString(imeAction);
                        default:
                            ic.performEditorAction(KeyEvent.KEYCODE_ENTER);
                            return "Enter";
                    }
            }
            return null;
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        this.imeAction = attribute.imeOptions & EditorInfo.IME_MASK_ACTION;
        String actionName = convertActionToString(imeAction);
        Log.i(TAG, "imeAction: " + imeAction + " " + actionName);
        setEnterKeyLabel(actionName);
        inputView.invalidateAllKeys();
    }

    private void setEnterKeyLabel(String label) {
        List<Keyboard.Key> keys = keyboard.getKeys();
        for(Keyboard.Key key: keys) {
            if(key.codes[0] == -8) {
                key.label = label;
            }
        }
    }

    // Refs: https://www.jianshu.com/p/892168a57fe3
    private class MyKeyboardActionListener implements KeyboardView.OnKeyboardActionListener {
        @Override
        public void onPress(int i) {}

        @Override
        public void onRelease(int i) {}

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            InputConnection ic = getCurrentInputConnection();
            if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                Log.d(TAG, "Keyboard CANCEL not implemented");
            } else if (primaryCode == -10) {
                clearText();
            } else if (primaryCode == -5) {
                changeInputMethod();
                // switchToLastInputMethod();
            } else if (primaryCode == -7) {
                ic.commitText(randomString(1), 1);
            } else if (primaryCode == -8) {
                ic.performEditorAction(imeAction);
            } else {
                Log.w(TAG, "Unknown primaryCode " + primaryCode);
            }
        }

        @Override
        public void onText(CharSequence charSequence) {}

        @Override
        public void swipeLeft() {}

        @Override
        public void swipeRight() {}

        @Override
        public void swipeDown() {
            hideInputMethod(AdbKeyboard.this);
        }

        @Override
        public void swipeUp() {}
    }

    /**
     * text 优先按 base64 解码为 UTF-8；若不是合法 base64 或解码结果不是合法 UTF-8，
     * 则直接当作原始文本使用，方便命令行直接输入中文。
     */
    private static String decodeInputText(String text) {
        try {
            byte[] data = Base64.decode(text, Base64.DEFAULT);
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(data)).toString();
        } catch (IllegalArgumentException | CharacterCodingException e) {
            return text;
        }
    }

    private void inputText(String textExtra, long intervalMs) {
        String text = decodeInputText(textExtra);
        final InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        cancelPendingInput();
        if (intervalMs <= 0 || text.length() <= 1) {
            ic.commitText(text, 1);
            return;
        }
        final String input = text;
        pendingInput = new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= input.length()) {
                    return;
                }
                int codePoint = input.codePointAt(index);
                int charCount = Character.charCount(codePoint);
                ic.commitText(input.substring(index, index + charCount), 1);
                index += charCount;
                if (index < input.length()) {
                    inputHandler.postDelayed(this, intervalMs);
                }
            }
        };
        inputHandler.post(pendingInput);
    }

    private void cancelPendingInput() {
        if (pendingInput != null) {
            inputHandler.removeCallbacks(pendingInput);
            pendingInput = null;
        }
    }

    private void clearText() {
        // Refs: https://stackoverflow.com/questions/33082004/android-custom-soft-keyboard-how-to-clear-edit-text-commited-text
        InputConnection ic = getCurrentInputConnection();
        CharSequence currentText = ic.getExtractedText(new ExtractedTextRequest(), 0).text;
        CharSequence beforCursorText = ic.getTextBeforeCursor(currentText.length(), 0);
        CharSequence afterCursorText = ic.getTextAfterCursor(currentText.length(), 0);
        ic.deleteSurroundingText(beforCursorText.length(), afterCursorText.length());
    }

    private String getText() {
        String text = "";
        try {
            InputConnection ic = getCurrentInputConnection();
            ExtractedTextRequest req = new ExtractedTextRequest();
            req.hintMaxChars = 100000;
            req.hintMaxLines = 10000;
            req.flags = 0;
            req.token = 0;
            text = ic.getExtractedText(req, 0).text.toString();
        } catch (Throwable t) {
        }
        return text;
    }

    private void changeInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showInputMethodPicker();
    }

    public String randomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int num = random.nextInt(62);
            buf.append(str.charAt(num));
        }
        return buf.toString();
    }

    private void showInputMethod(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInputFromInputMethod(Objects.requireNonNull(getWindow().getWindow()).getAttributes().token, 0);
    }

    private void hideInputMethod(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromInputMethod(Objects.requireNonNull(getWindow().getWindow()).getAttributes().token, 0);
    }

    public static String convertActionToString(int action) {
        switch (action) {
            case EditorInfo.IME_ACTION_NONE:
                return "None";
            case EditorInfo.IME_ACTION_GO:
                return "Go";
            case EditorInfo.IME_ACTION_SEARCH:
                return "Search";
            case EditorInfo.IME_ACTION_SEND:
                return "Send";
            case EditorInfo.IME_ACTION_NEXT:
                return "Next";
            case EditorInfo.IME_ACTION_DONE:
                return "Done";
            default:
                return "Enter";
        }
    }
}
