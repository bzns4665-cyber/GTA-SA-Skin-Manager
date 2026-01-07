package com.gtasa.skinmanager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_DFF_FILE = 1;
    private static final int PICK_TXD_FILE = 2;
    
    private EditText editCharacterName;
    private TextView tvDffPath, tvTxdPath, tvStatus;
    private Button btnSelectDff, btnSelectTxd, btnInstall;
    
    private Uri dffFileUri, txdFileUri;
    private File gtaDir;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkPermissions();
        setupClickListeners();
    }
    
    private void initViews() {
        editCharacterName = findViewById(R.id.editCharacterName);
        tvDffPath = findViewById(R.id.tvDffPath);
        tvTxdPath = findViewById(R.id.tvTxdPath);
        tvStatus = findViewById(R.id.tvStatus);
        btnSelectDff = findViewById(R.id.btnSelectDff);
        btnSelectTxd = findViewById(R.id.btnSelectTxd);
        btnInstall = findViewById(R.id.btnInstall);
        
        // Set default character
        editCharacterName.setText("andre");
    }
    
    private void setupClickListeners() {
        btnSelectDff.setOnClickListener(v -> selectFile(PICK_DFF_FILE));
        btnSelectTxd.setOnClickListener(v -> selectFile(PICK_TXD_FILE));
        btnInstall.setOnClickListener(v -> installSkin());
    }
    
    private void selectFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            
            if (requestCode == PICK_DFF_FILE) {
                dffFileUri = fileUri;
                tvDffPath.setText("DFF: " + fileUri.getLastPathSegment());
            } else if (requestCode == PICK_TXD_FILE) {
                txdFileUri = fileUri;
                tvTxdPath.setText("TXD: " + fileUri.getLastPathSegment());
            }
        }
    }
    
    private void installSkin() {
        String characterName = editCharacterName.getText().toString().trim().toLowerCase();
        
        if (characterName.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال اسم الشخصية", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (dffFileUri == null || txdFileUri == null) {
            Toast.makeText(this, "الرجاء اختيار ملفات DFF و TXD", Toast.LENGTH_SHORT).show();
            return;
        }
        
        tvStatus.setText("جاري التثبيت...");
        
        new Thread(() -> {
            try {
                // Find GTA SA directory
                File gtaSADir = findGTASADirectory();
                if (gtaSADir == null) {
                    runOnUiThread(() -> {
                        tvStatus.setText("خطأ: لم يتم العثور على مجلد اللعبة");
                        Toast.makeText(this, "تأكد من تثبيت GTA SA", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                // Process IMG file
                File imgFile = new File(gtaSADir, "texdb/gta3.img");
                if (!imgFile.exists()) {
                    runOnUiThread(() -> {
                        tvStatus.setText("خطأ: ملف gta3.img غير موجود");
                    });
                    return;
                }
                
                // Read DFF and TXD files
                byte[] dffData = FileUtils.readFileFromUri(this, dffFileUri);
                byte[] txdData = FileUtils.readFileFromUri(this, txdFileUri);
                
                // Replace in IMG file
                IMGArchive imgArchive = new IMGArchive(imgFile);
                boolean dffReplaced = imgArchive.replaceFile(characterName + ".dff", dffData);
                
                // Replace TXD
                File txdFile = new File(gtaSADir, "texdb/" + characterName + ".txd");
                boolean txdReplaced = FileUtils.writeFile(txdFile, txdData);
                
                runOnUiThread(() -> {
                    if (dffReplaced && txdReplaced) {
                        tvStatus.setText("✓ تم التثبيت بنجاح!");
                        Toast.makeText(this, "تم تغيير سكن " + characterName + " بنجاح", Toast.LENGTH_LONG).show();
                    } else {
                        tvStatus.setText("خطأ في التثبيت");
                        Toast.makeText(this, "فشل في استبدال الملفات", Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvStatus.setText("خطأ: " + e.getMessage());
                    Toast.makeText(this, "حدث خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private File findGTASADirectory() {
        // Common paths for GTA SA Android
        String[] possiblePaths = {
            "/storage/emulated/0/Android/obb/com.rockstargames.gtasa",
            "/sdcard/Android/obb/com.rockstargames.gtasa",
            Environment.getExternalStorageDirectory() + "/Android/obb/com.rockstargames.gtasa"
        };
        
        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "تم منح الإذن", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "الإذن مطلوب للعمل", Toast.LENGTH_LONG).show();
            }
        }
    }
}