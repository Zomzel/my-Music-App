package com.example.music_zhoumingzheng;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检查用户是否同意隐私政策
                SharedPreferences preferences = getSharedPreferences("appPreferences", MODE_PRIVATE);
                boolean agreedToTerms = preferences.getBoolean("isAgreed", false);

                if (agreedToTerms) {
                    // 用户已同意隐私政策，跳转到主页
                    startHomeActivity();
                } else {
                    // 用户未同意隐私政策，显示隐私政策对话框
                    showPrivacyDialog();
                }
            }
        }, 2000); // 2秒延迟
    }

    private void showPrivacyDialog() {
        // 创建自定义对话框布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_privacy_policy, null);

        // 获取布局中的控件
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        Button btnDecline = dialogView.findViewById(R.id.btn_decline);
        Button btnAccept = dialogView.findViewById(R.id.btn_accept);

        // 设置点击事件
        // 设置弹窗内容，带有可点击的链接
        SpannableString message = new SpannableString("欢迎使用音乐社区，我们将严格遵守相关法律和隐私政策保护您的个人隐私，请您阅读并同意《用户协议》与《隐私政策》");

        ClickableSpan userAgreementSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Toast.makeText(MainActivity.this, "用户协议", Toast.LENGTH_SHORT).show();
            }
        };

        ClickableSpan privacyPolicySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Toast.makeText(MainActivity.this, "隐私政策", Toast.LENGTH_SHORT).show();
            }
        };

        message.setSpan(userAgreementSpan, message.toString().indexOf("《用户协议》"), message.toString().indexOf("《用户协议》") + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.setSpan(privacyPolicySpan, message.toString().indexOf("《隐私政策》"), message.toString().indexOf("《隐私政策》") + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 显示弹窗内容并启用点击
        dialogMessage.setText(message);
        dialogMessage.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false) // 禁止点击外部关闭对话框
                .create();

        btnDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 退出应用
                finish();
            }
        });

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 保存用户同意的选择
                SharedPreferences sharedPreferences = getSharedPreferences("appPreferences", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isAgreed", true);
                editor.apply();

                // 进入首页
                startHomeActivity();
                dialog.dismiss();
            }
        });

        // 设置对话框的背景为圆角
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog);

        dialog.show();
    }

    private void startHomeActivity(){
        /**
         * 进入首页*/
        //设置动画
        Intent intent = new Intent(MainActivity.this, HomePageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ActivityOptions options = ActivityOptions.makeCustomAnimation(MainActivity.this, R.anim.fade_in, R.anim.fade_out);
        startActivity(intent,options.toBundle());
    }
}

