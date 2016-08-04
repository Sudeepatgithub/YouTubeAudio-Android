package com.example.youtubeapidemo;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;


public class MainActivity extends ActionBarActivity implements myInterface {

    Button searchButton;
    ActionBar actionBar;
    DownloadManager mManager;
    private EditText searchInput;
    private ListView videosFound;
    private Handler handler;
    private List<VideoItem> searchResults;
    String keywordSearch;
    private ProgressDialog progressBar;
    Button downloadButton;
    private TextView downloadStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchInput = (EditText) findViewById(R.id.search_input);
        searchButton = (Button) findViewById(R.id.search);
        videosFound = (ListView) findViewById(R.id.videos_found);
        progressBar=new ProgressDialog(this);
        downloadStatus=(TextView)findViewById(R.id.downloadStatus);
        handler = new Handler();
        actionBar = getSupportActionBar();
        addClickListener();
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                  /* Write your logic here that will be executed when user taps next button */
                    searchButton.performClick();
                    handled = true;
                }
                return handled;
            }
        });



        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try  {
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e) {

                }
                progressBar.setTitle("Wait");
                progressBar.setMessage("Fetching data from YouTube");
                progressBar.show();
                searchOnYoutube(searchInput.getText().toString());
                keywordSearch=searchInput.getText().toString();

            }
        });


        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                DownloadManager.Query query = new DownloadManager.Query();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    Cursor cursor = mManager.query(query);
                    query.setFilterById(downloadId);
                    downloadButton.setVisibility(View.INVISIBLE);
                    if (cursor.moveToFirst()) {
                        if (cursor.getCount() > 0) {
                            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                String file = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                                Toast.makeText(getApplicationContext(), "Download Success", Toast.LENGTH_SHORT).show();
                                downloadStatus.setTextColor(Color.parseColor(""));
                                downloadStatus.setText("Downloaded");
                            } else {
                                int message = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                                Toast.makeText(getApplicationContext(), "Download Failed", Toast.LENGTH_SHORT).show();
                                // So something here on failed.
                            }
                        }
                    }
                        }
            }

        };

        registerReceiver(receiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                }

    private void searchOnYoutube(final String keywords) {
        new Thread() {
            @Override
            public void run() {
                YoutubeConnector yc = new YoutubeConnector(MainActivity.this);
                searchResults = yc.search(keywords);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateVideosFound();
                    }
                });
            }
        }.start();
    }

    private void updateVideosFound() {
        ArrayAdapter<VideoItem> adapter = new ArrayAdapter<VideoItem>(getApplicationContext(), R.layout.video_item, searchResults) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.video_item, parent, false);
                }
                actionBar.setTitle(keywordSearch.toUpperCase());
                searchInput.setText("");
                progressBar.hide();
                ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
                TextView title = (TextView) convertView.findViewById(R.id.video_title);
            //    TextView description = (TextView) convertView.findViewById(R.id.video_description);
                VideoItem searchResult = searchResults.get(position);
                Picasso.with(getApplicationContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
                title.setText(searchResult.getTitle());

                return convertView;
            }
        };
        videosFound.setAdapter(adapter);

    }

    private void addClickListener() {

        videosFound.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                downloadButton=(Button)(getViewByPosition(position,videosFound)).findViewById(R.id.downloadButton);
                new HitApi(MainActivity.this,getApplicationContext()).execute("http://www.youtubeinmp3.com/fetch/?format=JSON&video=" + "http://www.youtube.com/watch?v=" + searchResults.get(position).getId() + "&start=2");
            }
        });

    }

    public void playMusic(String url, String FileName) {

        File direct = new File(Environment.getExternalStorageDirectory()
                + "/YT_Downloads");

        if (!direct.exists()) {
            direct.mkdirs();
        }

        mManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        final DownloadManager.Request mRqRequest = new DownloadManager.Request(uri);

        mRqRequest.setDestinationInExternalPublicDir("/YT_Downloads", FileName + ".mp3");
        mRqRequest.setDescription("Downloading..");
        final long id= mManager.enqueue(mRqRequest);
        Toast.makeText(this, "Audio will now be downloaded", Toast.LENGTH_SHORT).show();
        downloadButton.setVisibility(View.VISIBLE);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.remove(id);
            }
        });

    }

    @Override
    public void onDataSet(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            String link = jsonObj.getString("link");
            Log.i("link", link);
            playMusic(jsonObj.getString("link"), jsonObj.getString("title"));
        } catch (JSONException e) {
            Toast.makeText(this,"Download Failed",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.action_settings:
                startActivity(new Intent(this,infoScreen.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }
}
