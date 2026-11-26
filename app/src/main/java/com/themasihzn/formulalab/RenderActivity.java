package com.themasihzn.formulalab;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * formulaLab LaTeX renderer.
 *
 * Ways in:
 *  - launcher icon: type LaTeX, it renders live
 *  - intent action com.themasihzn.formulalab.RENDER, extra "latex" (or EXTRA_TEXT)
 *  - link formulalab://render?latex=...  or  formulalab://render?name=<file name>
 *    (this is what the KOReader Lua home screen sends)
 *  - share text into the app
 *  - "Open with formulaLab" on any file: the file NAME is rendered as LaTeX
 *
 * Rendering is offline: KaTeX is bundled in assets/katex.
 */
public class RenderActivity extends Activity {

    public static final String ACTION_RENDER = "com.themasihzn.formulalab.RENDER";
    public static final String EXTRA_LATEX = "latex";

    private static final int TEAL = 0xFF4FD1C5;
    private static final int MUTED = 0xFF7A8C8B;

    private WebView web;
    private EditText input;
    private TextView source;
    private boolean pageReady = false;
    private String pending = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        setContentView(buildLayout());
        if (savedInstanceState == null) handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private View buildLayout() {
        int pad = dp(10);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setFitsSystemWindows(true);

        input = new EditText(this);
        input.setId(View.generateViewId());
        input.setHint("LaTeX, e.g. \\partial_t(\\text{Entropy}) \\geq 0");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEAL);
        input.setTypeface(Typeface.MONOSPACE);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setPadding(pad, pad, pad, pad);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { render(s.toString()); }
        });
        root.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        source = new TextView(this);
        source.setTextColor(MUTED);
        source.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        source.setPadding(pad, 0, pad, 0);
        source.setVisibility(View.GONE);
        root.addView(source);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                if (pending != null) {
                    String p = pending;
                    pending = null;
                    render(p);
                }
            }
        });
        web.loadUrl("file:///android_asset/render.html");
        root.addView(web, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    private void handle(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String latex = null;
        String label = null;

        if (ACTION_RENDER.equals(action)) {
            latex = intent.getStringExtra(EXTRA_LATEX);
            if (latex == null) latex = intent.getStringExtra(Intent.EXTRA_TEXT);
        } else if (Intent.ACTION_SEND.equals(action)) {
            latex = intent.getStringExtra(Intent.EXTRA_TEXT);
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            Uri uri = intent.getData();
            if ("formulalab".equals(uri.getScheme())) {
                latex = uri.getQueryParameter("latex");
                String name = uri.getQueryParameter("name");
                if (latex == null && name != null) {
                    latex = LatexName.fromFileName(name);
                    label = name;
                }
            } else {
                String name = displayName(uri);
                latex = LatexName.fromFileName(name);
                label = name;
            }
        }

        if (latex != null) {
            input.setText(latex);          // triggers render()
            input.setSelection(input.getText().length());
        }
        if (label != null) {
            source.setText("from file: " + label);
            source.setVisibility(View.VISIBLE);
        } else {
            source.setVisibility(View.GONE);
        }
    }

    private String displayName(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(
                    uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    String n = c.getString(0);
                    if (n != null) return n;
                }
            } catch (Exception ignored) {
                // fall through to the path segment
            }
        }
        String last = uri.getLastPathSegment();
        return last != null ? last : "";
    }

    private void render(String tex) {
        if (!pageReady) {
            pending = tex;
            return;
        }
        web.evaluateJavascript("show(" + JSONObject.quote(tex) + ")", null);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
