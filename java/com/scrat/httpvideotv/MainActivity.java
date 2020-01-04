package com.scrat.httpvideotv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class MainActivity extends Activity implements MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private final ArrayList<String> videoList = new ArrayList<>();
    private int ndx = 0;
    private VideoView videoView;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        setContentView(R.layout.mainlayout);
        videoView = findViewById(R.id.videosurface);
        videoView.setOnCompletionListener(this);
        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);
        createPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            while (isNetworkAvailable()) sleep(100);
            videoView.resume();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.suspend();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    private void onPlayFile() {
        if (ndx > videoList.size() - 1) ndx = 0;
        try {
            new URL(videoList.get(ndx));
            videoView.setVideoURI(Uri.parse(videoList.get(ndx)));
            videoView.setVideoPath(videoList.get(ndx));
            ndx++;
        } catch (Exception e) {
            ndx++;
            onPlayFile();
        }
    }

    private void createPlayer() {
        releasePlayer();
        new LoaderPlayList(PreferenceManager.getDefaultSharedPreferences(mContext).getString("url","http://192.168.0.210/playlist.pl")).execute();
    }

    private void releasePlayer() {
        videoView.requestFocus();
        videoView.stopPlayback();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        releasePlayer();
        try {
            while (isNetworkAvailable()) sleep(100);
            onPlayFile();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        videoView.requestFocus();
        videoView.seekTo(0);
        videoView.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        releasePlayer();
        try {
            while (isNetworkAvailable()) sleep(100);
            onPlayFile();
        } catch (Exception ignored) {
        }
        return true;
    }

    @SuppressLint("StaticFieldLeak")
    private class LoaderPlayList extends AsyncTask<Void, Void, Void> {
        final ArrayList<String> tempVideoList = new ArrayList<>();
        private String path_url;

        LoaderPlayList(String path) {
            path_url = path;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                while (isNetworkAvailable())  {
                    sleep(100);
                }
                URL url = new URL(path_url);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String line;
                while ((line = in.readLine()) != null) tempVideoList.add(line);
                in.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            videoList.clear();
            videoList.addAll(tempVideoList);
            ndx = 0;
            if (videoList.size()>0) onPlayFile(); else createPlayer();
        }
    }
}
