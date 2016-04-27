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
import android.support.v4.widget.TextViewCompat;
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
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private CallbackManager callbackManager;
    private JSONArray reports;
    private Profile profile;
    private RecyclerView mRecyclerView;
    private ReportAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Initializing Facebook's SDK.
         *
         * Later used for Login and report sharing.
         */
        FacebookSdk.sdkInitialize(MainActivity.this);
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().logInWithReadPermissions(MainActivity
                .this, Arrays.asList("public_profile"));

        profile = Profile.getCurrentProfile();
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

        TextView textView = (TextView) navigationView.findViewById(R.id.firstName);
        textView.setText(profile.getFirstName());

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
                ArrayList<Long> jsonValues = new ArrayList<Long>();
                for (int i = 0; i < reports.length(); i++) {
                    try {
                        jsonValues.add(reports.getJSONObject(i).getLong("date"));
                        Collections.sort(jsonValues);
                    } catch (JSONException e) {
                        Log.d("json", e.toString());
                    }
                }
                reports = new JSONArray(jsonValues);
                mAdapter.setReports(reports);
                mAdapter.notifyDataSetChanged();
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

        switch (id) {
            case R.id.nav_profile:
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                return true;
            default:
                return true;
        }
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
    private class LoadTimeline extends AsyncTask<Void, Void, JSONArray> {

        Context context;

        public LoadTimeline(Context context) {
            this.context = context;
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            JSONArray jsonArray = new JSONArray();
            SyncManager s = new SyncManager(context);
            Query query = s.getDatabase().createAllDocumentsQuery();
            try {
                QueryEnumerator queryEnumerator = query.run();
                Iterator<QueryRow> rowIterator = queryEnumerator.iterator();
                for (int i = 0;rowIterator.hasNext(); i++) {
                    QueryRow row = rowIterator.next();
                    Document document = s.getDocument(row.getDocumentId());
                    jsonArray.put(i, new JSONObject(document.getProperties()));
                }
            } catch (CouchbaseLiteException e) {
                Log.d("couchbase", e.toString());
            } catch (JSONException e) {
                Log.d("couchbase", e.toString());
            }
            return jsonArray;
        }
    }

    class JSONComparator implements Comparator<JSONObject> {

        @Override
        public int compare(JSONObject a, JSONObject b) {
            long valA = 0;
            long valB = 0;

            try {
                valA = a.getLong("date");
                valB = b.getLong("date");
            } catch (JSONException e) {
                Log.d("json", e.toString());
            }

            if(valA > valB)
                return 1;
            else if(valA < valB)
                return -1;
            else
                return 0;
        }
    }
}
