package cn.caldow.codescanner;

import android.content.Context;

import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {
    private static Context ctx = null;

    Utils(Context s) {
        ctx = s;
    }

    public void AddUrlLog(String url) {
        try {
            // 打开一个名为filename的文本文件，并将内容追加到文件末尾
            String filename = "urls.txt";
            FileWriter writer = new FileWriter(new File(ctx.getExternalFilesDir(null), filename), true);
            // 写入内容到文件末尾
            writer.write("\n" + url);
            // 关闭文件
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
