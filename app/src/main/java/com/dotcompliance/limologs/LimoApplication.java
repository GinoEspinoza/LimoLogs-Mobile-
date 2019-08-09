package com.dotcompliance.limologs;

import android.support.multidex.MultiDexApplication;

import com.orhanobut.hawk.Hawk;

import org.researchstack.backbone.StorageAccess;
import org.researchstack.backbone.storage.database.AppDatabase;
import org.researchstack.backbone.storage.database.sqlite.DatabaseHelper;
import org.researchstack.backbone.storage.file.EncryptionProvider;
import org.researchstack.backbone.storage.file.FileAccess;
import org.researchstack.backbone.storage.file.PinCodeConfig;
import org.researchstack.backbone.storage.file.SimpleFileAccess;
import org.researchstack.backbone.storage.file.UnencryptedProvider;

import java.util.ArrayList;
import java.util.List;


public class LimoApplication extends MultiDexApplication {

    public static void closeAllActivities() {
        for (LimoBaseActivity limoBaseActivity : activeActivities) {
            limoBaseActivity.finish();
        }
    }

    public static void addActiveActivity(LimoBaseActivity limoBaseActivity) {
        activeActivities.add(limoBaseActivity);
    }

    public static void removeActiveActivity(LimoBaseActivity limoBaseActivity) {
        activeActivities.remove(limoBaseActivity);
    }

    private static List<LimoBaseActivity> activeActivities = new ArrayList<>();

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Customize your pin code preferences
        PinCodeConfig pinCodeConfig = new PinCodeConfig(); // default pin config (4-digit, 1 min lockout)

        // Customize encryption preferences
        EncryptionProvider encryptionProvider = new UnencryptedProvider(); // No pin, no encryption

        // If you have special file handling needs, implement FileAccess
        FileAccess fileAccess = new SimpleFileAccess();

        // If you have your own custom database, implement AppDatabase
        AppDatabase database = new DatabaseHelper(this,
                DatabaseHelper.DEFAULT_NAME,
                null,
                DatabaseHelper.DEFAULT_VERSION);

        Hawk.init(this).build();

        StorageAccess.getInstance().init(pinCodeConfig, encryptionProvider, fileAccess, database);
    }
}
