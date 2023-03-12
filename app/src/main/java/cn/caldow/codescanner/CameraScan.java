package cn.caldow.codescanner;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanBase;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;

public class CameraScan extends Activity {
    private static final int REQUEST_CODE_SCAN_ONE = 9999;

    private Utils utils = null;

    private int onTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        utils = new Utils(getApplicationContext());
        onTimes = 0;

        ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (onTimes != 0) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            finish();

        }
        onTimes++;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Log.w("标志位", "1");
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            // 导入图片扫描返回结果
            int errorCode = data.getIntExtra(ScanUtil.RESULT_CODE, ScanUtil.SUCCESS);
            if (errorCode == ScanUtil.SUCCESS) {
                HmsScan res = data.getParcelableExtra(ScanUtil.RESULT);
                Log.w("标志位", "2");

                if (res != null) {
                    playInfo();
                    String result = res.originalValue;
                    Log.w("tag_result", result);
                    utils.AddUrlLog(result);

                    Intent home = new Intent(Intent.ACTION_MAIN);
                    home.addCategory(Intent.CATEGORY_HOME);
                    startActivity(home);
                    finish();

                    if (result.startsWith("https://qr.alipay.com/")) {
                        System.out.println("跳转到支付宝付款");
                        Intent jump = new Intent(Intent.ACTION_VIEW);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String uri;
                                try {
                                    uri = "alipays://platformapi/startapp?saId=10000007&qrcode=" + URLEncoder.encode(result, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    throw new RuntimeException(e);
                                }
                                jump.setData(Uri.parse(uri));
                                startActivity(jump);
                            }
                        });
                        finish();
                    } else if (result.startsWith("wxp:/")) {
                        System.out.println("跳转到微信支付");
                        Intent jump = new Intent(Intent.ACTION_VIEW);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String uri = null;
                                uri = "weixin://";
                                jump.setData(Uri.parse(uri));
                                startActivity(jump);
                            }
                        });
                        finish();
                    } else if (result.contains("https://") || result.contains("http://")) {
                        if (result.startsWith("https://mp.weixin.qq.com")) {
                            Toast.makeText(getApplicationContext(), "当前不支持微信文章扫码", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        if (result.startsWith("http://app.littleswan.com/")) {
                            Intent jump = new Intent(Intent.ACTION_VIEW);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String uri = null;
                                    try {
                                        uri = "alipays://platformapi/startapp?saId=10000007&qrcode=" + URLEncoder.encode(result, "UTF-8");
                                    } catch (UnsupportedEncodingException e) {
                                        throw new RuntimeException(e);
                                    }
                                    jump.setData(Uri.parse(uri));
                                    startActivity(jump);
                                }
                            });
                            finish();
                            return;
                        }
                        if (result.startsWith("https://passport.bilibili.com") && result.contains("login")) {
                            Intent jump = new Intent(Intent.ACTION_VIEW);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String uri = null;
                                    try {
                                        uri = "bilibili://browser/?url=" + URLEncoder.encode(result, "UTF-8");
                                    } catch (UnsupportedEncodingException e) {
                                        throw new RuntimeException(e);
                                    }
                                    jump.setData(Uri.parse(uri));
                                    startActivity(jump);
                                }
                            });
                            finish();
                            return;
                        }
                        if (result.startsWith("http://txz.qq.com/")) {
                            Intent jump = new Intent(Intent.ACTION_VIEW);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String uri;
                                    uri = "mqqapi://qrcode/scan_qrcode?version=1&src_type=app&url=" + result;
                                    jump.setData(Uri.parse(uri));
                                    startActivity(jump);
                                }
                            });
                            finish();
                            return;
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Uri uri = Uri.parse(result);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }
                        });
                        finish();
                    } else {
                        if (!getCodeTypeifQR(res.getScanType())) {
//                            Toast.makeText(getApplicationContext(), "暂不支持商品条码", Toast.LENGTH_LONG).show();
//                            finish();
//                            Intent jump = new Intent(Intent.ACTION_VIEW);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    String uri = null;
//                                    uri = "openjd://virtual?params={\"category\":\"jump\",\"des\":\"saoasao\"}";
//                                    jump.setData(Uri.parse(uri));
//                                    startActivity(jump);
//                                }
//                            });
//                            finish();

                            Intent jp = new Intent(this, WebBrowser.class);
                            jp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            jp.putExtra("url", "https://www.gds.org.cn/#/barcodeList/index?type=barcode&keyword=" + result);
                            startActivity(jp);
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "暂不支持此类型二维码", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        }
    }

    private void playInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.w("状态", "声音开始");
                MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.qrcode_completed);

                mediaPlayer.start(); // 开始播放音频
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 音频播放完成后的回调
                        mediaPlayer.release(); // 释放 MediaPlayer 对象
                    }
                });
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.w("状态", "震动开始");
                // 线程执行的代码
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(50);
            }
        }).start();

    }

    private boolean getCodeTypeifQR(int tp) {
        int[] arr = {HmsScanBase.AZTEC_SCAN_TYPE, HmsScanBase.DATAMATRIX_SCAN_TYPE, HmsScanBase.PDF417_SCAN_TYPE};
        for (int j : arr) {
            if (j == tp) {
                return true;
            }
        }
        return false;
    }
}