package io.makerforce.ambrose.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ImageButton backButton;
    
    private Handler handler;
    private Runnable runnable;
    private int timeOutSec = 180;

    private List<AppItem> appItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setAppIdleTimeout();

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new StaggeredGridLayoutManager(3, 1);
        mRecyclerView.setLayoutManager(mLayoutManager);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int granted = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                Log.d("AllApps", "Requires permission");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                Log.d("AllApps", "Permission already granted");
            }
        } else {
            int granted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                Log.d("AllApps", "Requires permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                Log.d("AllApps", "Permission already granted");
            }
        }

        updateAppListData(appItems);
        mAdapter = new AppListAdapter(appItems, getApplicationContext());
        mRecyclerView.setAdapter(mAdapter);

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        });
    }

    private void updateAppListData(List<AppItem> appItems){
        List<AppItem> listViewItems = readList();
        if (listViewItems == null) {
            listViewItems = new ArrayList<>();
        }

        appItems.clear();
        appItems.addAll(listViewItems);
    }

    private List<AppItem> readList() {
        File sdcard = Environment.getExternalStorageDirectory();
        PackageManager packageManager = getApplicationContext().getPackageManager();

        String json = null;
        try {
            InputStream is = new FileInputStream(new File(sdcard, "apps.json"));
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e("AllApps", ex.toString());
            return null;
        }

        List<AppItem> list = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray apps = obj.getJSONArray("apps");

            for (int i = 0; i < apps.length(); i++) {
                JSONObject app = apps.getJSONObject(i);

                String packageName = app.getString("package");
                String title = app.has("title") ? app.getString("title") : "Unknown";
                String description = app.has("description") ? app.getString("description") : "";
                String icon = app.has("icon") ? app.getString("icon") : "default.png";
                String color = app.has("color") ? app.getString("color") : "#ffffff";
                String type = app.has("type") ? app.getString("type") : "NORMAL_ICON";
                //boolean featured = app.has("featured") ? app.getBoolean("featured") : false;

                Drawable iconDrawable = null; // = getFullResIcon(Resources.getSystem(), android.R.mipmap.sym_def_app_icon);
                Intent pi = packageManager.getLaunchIntentForPackage(packageName);
                if (pi != null) {
                    ResolveInfo ri = packageManager.resolveActivity(pi, PackageManager.MATCH_ALL);
                    iconDrawable = getFullResIcon(packageName, ri.activityInfo.getIconResource());
                }
                try {
                    iconDrawable = Drawable.createFromStream(new FileInputStream(new File(sdcard, icon)), icon);
                    Log.d("AllApps", icon);
                } catch (FileNotFoundException e) {
                    // do nothing
                }

                if (title == "Unknown" && pi != null) {
                    try {
                        title = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0));
                    } catch (PackageManager.NameNotFoundException e) {
                        // do nothing
                    }
                }


                int typeInt = AppItem.NORMAL_ICON;
                switch (type) {
                    case "NORMAL_ICON":
                        typeInt = AppItem.NORMAL_ICON;
                        break;
                    case "SIMPLE_ICON":
                        typeInt = AppItem.SIMPLE_ICON;
                        break;
                    case "BANNER_IMAGE":
                        typeInt = AppItem.BANNER_IMAGE;
                        break;
                }

                int colorInt = -1;
                try {
                    colorInt = Color.parseColor(color);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }

                AppItem a = new AppItem(packageName, title, description, iconDrawable, typeInt, colorInt);
                list.add(a);
            }
        } catch (JSONException ex) {
            Log.e("AllApps", ex.toString());
            return null;
        }

        return list;
    }

    private Drawable getFullResIcon(String packageName, int iconRes) {
        try {
            return getFullResIcon(getApplicationContext().getPackageManager().getResourcesForApplication(packageName), iconRes);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
    private Drawable getFullResIcon(Resources resources, int iconRes) {
        try {
            return resources.getDrawableForDensity(iconRes, DisplayMetrics.DENSITY_XHIGH);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateAppListData(appItems);
                    mAdapter.notifyDataSetChanged();
                } else {
                    Log.d("AllApps", "Permission denied");
                }
                return;
        }
    }
    
    private void setAppIdleTimeout() {

        handler = new Handler();
        runnable = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // Navigate to main activity
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                        finish();
                        overridePendingTransition(0,0);
                    }
                });
            }
        };
        handler.postDelayed(runnable, timeOutSec * 1000);
    }

    //reset timer on user interaction and in onResume
    public void resetAppIdleTimeout() {
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, timeOutSec * 1000);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        resetAppIdleTimeout();
    }

    @Override
    public void onUserInteraction() {
        // TODO Auto-generated method stub
        resetAppIdleTimeout();
        super.onUserInteraction();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }

}
