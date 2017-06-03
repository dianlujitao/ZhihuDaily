package org.explosion.zhihudaily.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.StringCallback;

import org.explosion.zhihudaily.R;
import org.explosion.zhihudaily.bean.Theme;
import org.explosion.zhihudaily.ui.fragment.StoryListFragment;

import java.lang.ref.WeakReference;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

import static java.lang.Math.max;
import static org.explosion.zhihudaily.helper.WebUtils.getLatestStoryListURL;
import static org.explosion.zhihudaily.helper.WebUtils.getThemeDescURL;
import static org.explosion.zhihudaily.helper.WebUtils.getThemeListURL;
import static org.explosion.zhihudaily.helper.ParseJSON.getThemesList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";

    boolean doubleBackToExitPressedOnce;

    private List<Theme> themes;
    private int[] themeIdx;

    private Menu navMenu;

    private static final int UPDATE_DRAWER_MENU = 10000;
    private static class MyHandler extends Handler {
        WeakReference<MainActivity> mActivity;
        MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            switch (msg.what) {
                case UPDATE_DRAWER_MENU:
                    activity.updateDrawerMenu();
                    break;
                default:
                    break;
            }
        }
    }
    MyHandler handler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navMenu = navigationView.getMenu();

        MenuItem item = navigationView.getMenu().getItem(0);
        if (item != null) {
            onNavigationItemSelected(item);
        }
        retrieveDrawerMenu();
    }

    private void retrieveDrawerMenu() {
        OkGo.get(getThemeListURL())
                .tag(this)
                .cacheKey("cacheKey")
                .cacheMode(CacheMode.DEFAULT)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(String s, Call call, Response response) {
                        themes = getThemesList(s);
                        if (themes != null) {
                            Message msg = new Message();
                            msg.what = UPDATE_DRAWER_MENU;
                            handler.sendMessage(msg);
                        }
                    }
                });
    }

    private void updateDrawerMenu() {
        int maxId = 0;
        for (int i = 0; i < themes.size(); i++) {
            Theme theme = themes.get(i);
            navMenu.add(0, theme.getId(), Menu.NONE, theme.getName());
            maxId = max(maxId, theme.getId());
        }
        themeIdx = new int[maxId + 1];
        for (int i = 0; i < themes.size(); i++) {
            themeIdx[themes.get(i).getId()] = i;
        }
        navMenu.setGroupCheckable(0, true, true);
    }

    @Override
    protected void onResume() {
        doubleBackToExitPressedOnce = false;
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            doubleBackToExitPressedOnce = true;
            Toast.makeText(this, R.string.double_click_to_exit, Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        String tag, url, title;
        boolean isTheme;

        if (id == R.id.nav_home) {
            url = getLatestStoryListURL();
            tag = "story_home";
            title = "首页";
            isTheme = false;
        } else {
            url = getThemeDescURL(id);
            tag = themes.get(themeIdx[id]).toString();
            title = themes.get(themeIdx[id]).getName();
            isTheme = true;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.story_list_fl, StoryListFragment.newInstance(url, isTheme), tag)
                .commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        item.setChecked(true);
        setTitle(title);
        return true;
    }
}
