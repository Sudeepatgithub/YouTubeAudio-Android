package com.example.youtubeapidemo;

import android.content.Context;
import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by sudeep.srivastava on 5/12/2016.
 */
public class HitApi extends AsyncTask<String, Void, String> {

    private myInterface onSetDataListener;
    Context context;
    public HitApi(myInterface onSetDataListener, Context context) {
        this.context=context;
        if(onSetDataListener!=null) {
            this.onSetDataListener = onSetDataListener;
        }
    }

    @Override
    protected String doInBackground(String... params) {
        String urlStr=params[0];
        String dataReturn="";
        try
        {
            URL url=new URL(urlStr);
            HttpURLConnection httpURLConnection=(HttpURLConnection)url.openConnection();
            InputStream in = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            int data = inputStreamReader.read();
            while (data != -1) {
                dataReturn +=(char)data;
                data = inputStreamReader.read();
            }
        } catch (Exception e) {

        }
      //  Log.v("JSON",dataReturn);
        return dataReturn;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        onSetDataListener.onDataSet(s);
    }

}
