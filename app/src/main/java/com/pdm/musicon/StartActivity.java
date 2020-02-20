package com.pdm.musicon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {
    private RelativeLayout relativeLayout;
    private AnimationDrawable animationDrawable;

    public static final String EXTRA_MESSAGE =
            "com.pdm.musicon.extra.MESSAGE";

    private EditText mMessageEditText;

    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        mMessageEditText = (EditText) findViewById(R.id.editText_start);

        relativeLayout = (RelativeLayout) findViewById(R.id.startLayout);
        animationDrawable = (AnimationDrawable) relativeLayout.getBackground();
        animationDrawable.setEnterFadeDuration(5000);
        animationDrawable.setExitFadeDuration(2000);
    }

    public void launchSecondActivity(View view) {

        Intent intent = new Intent(this, MainActivity.class);
        String userName = mMessageEditText.getText().toString();

        if (userName.matches("")) {
            mMessageEditText.setError("Type a name");
        } else {
            intent.putExtra(EXTRA_MESSAGE, userName);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animationDrawable != null && !animationDrawable.isRunning()) {
            animationDrawable.start();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (animationDrawable != null && animationDrawable.isRunning()) {
            animationDrawable.stop();
        }
    }

}
