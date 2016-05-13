package br.com.mossteam.parada;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.widget.ProfilePictureView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import br.com.mossteam.parada.adapter.ReportAdapter;
import br.com.mossteam.parada.db.SyncManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private AccessToken token;
    private AccessTokenTracker tracker;
    // private CallbackManager callbackManager;
    private JSONArray reports;
    private Profile profile;
    private ProfileTracker profileTracker;
    private RecyclerView mRecyclerView;
    private ReportAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(MainActivity.this);
        /*callbackManager = CallbackManager.Factory.create();
        ArrayList<String> permissions  = new ArrayList<String>();
        permissions.add("email");
        LoginManager.getInstance().logInWithReadPermissions(
                MainActivity.this, permissions);*/
        tracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {

            }
        };
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(
                    Profile oldProfile,
                    Profile currentProfile) {

            }
        };
        profile = Profile.getCurrentProfile();
        token = AccessToken.getCurrentAccessToken();
        if(token != null) {
            SyncManager s = new SyncManager(MainActivity.this);
            s.push(token.getToken());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NewReportActivity.class));
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final View headerView = navigationView.getHeaderView(0);
        if(profile != null) {
            ProfilePictureView pictureView = (ProfilePictureView) headerView.findViewById(R.id.user_profile_pic);
            pictureView.setProfileId(profile.getId());
            TextView textView = (TextView) headerView.findViewById(R.id.user_name);
            textView.setText(profile.getName());
            GraphRequest request = GraphRequest.newMeRequest(token, new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {
                    try {
                        TextView textView = (TextView) headerView.findViewById(R.id.user_email);
                        textView.setText(response.getJSONObject().getString("email"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            Bundle bundle = new Bundle();
            bundle.putString("fields", "email");
            request.setParameters(bundle);
            request.executeAsync();
        }
        else {
            TextView textView = (TextView) headerView.findViewById(R.id.user_name);
            textView.setText(R.string.sign_in);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        mTextView = (TextView) findViewById(R.id.no_reports);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadTimeline();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ReportAdapter(MainActivity.this, reports);
        mRecyclerView.setAdapter(mAdapter);
        reloadTimeline();
        if(reports.length() == 0) {
            swipeRefreshLayout.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        reloadTimeline();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tracker.stopTracking();
        profileTracker.stopTracking();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_sort:
                break;
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        Intent intent = null;

        switch (id) {
            /*case R.id.nav_profile:
                return true;*/
            case R.id.nav_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.nav_send_feedback:
                intent = new Intent(MainActivity.this, FeedbackActivity.class);
                startActivity(intent);
                return true;
            case R.id.nav_help:
                intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void reloadTimeline() {
        LoadTimeline timeline = new LoadTimeline(MainActivity.this);
        timeline.execute();
        try {
            reports = timeline.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        mAdapter.setReports(reports);
        mAdapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void logOut() {
        LoginManager.getInstance().logOut();
    }

    class LoadTimeline extends AsyncTask<Void, Void, JSONArray> {

        Context context;

        public LoadTimeline(Context context) {
            this.context = context;
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            ArrayList<JSONObject> list = new ArrayList<JSONObject>();
            JSONArray array = new JSONArray();
            SyncManager s = new SyncManager(context);
            Query query = s.getDatabase().createAllDocumentsQuery();
            try {
                QueryEnumerator queryEnumerator = query.run();
                Iterator<QueryRow> rowIterator = queryEnumerator.iterator();
                for (int i = 0;rowIterator.hasNext(); i++) {
                    QueryRow row = rowIterator.next();
                    Document document = s.getDocument(row.getDocumentId());
                    array.put(i, new JSONObject(document.getProperties()));
                }
            } catch (CouchbaseLiteException e) {
                Log.d("couchbase", e.toString());
            } catch (JSONException e) {
                Log.d("couchbase", e.toString());
            }

            for(int i = 0; i < array.length(); i++) {
                try {
                    list.add(array.getJSONObject(i));
                } catch (JSONException e) {
                    Log.e("json", e.toString());
                }
            }
            Collections.sort(list, new JSONComparator());

            return new JSONArray(list);
        }
    }

    class JSONComparator implements Comparator<JSONObject> {

        @Override
        public int compare(JSONObject j1, JSONObject j2) {
            String s1 = null;
            String s2 = null;

            try {
                s1 = j1.getString("bus_code");
                s2 = j2.getString("bus_code");
            } catch (JSONException e) {
                Log.e("json", e.toString());
            }

            return s1.compareTo(s2);
        }
    }
}
