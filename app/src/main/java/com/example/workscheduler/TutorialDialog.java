package com.example.workscheduler;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class TutorialDialog extends Dialog {

    private WebView webView;

    public TutorialDialog(Context context) {
        super(context, android.R.style.Theme_Light_NoTitleBar_Fullscreen);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        webView = new WebView(getContext());
        setContentView(webView);
        
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl("file:///android_asset/tutorial.html");
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void closeTutorial() {
            // Must run on UI thread
            if (getOwnerActivity() != null) {
                getOwnerActivity().runOnUiThread(() -> dismiss());
            } else {
                webView.post(() -> dismiss());
            }
        }
    }
}
