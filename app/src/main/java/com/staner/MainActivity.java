package com.staner;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TabHost;

import com.staner.database.DataBaseController;
import com.staner.model.MediaFileInfo;
import com.staner.model.PlaylistModel;
import com.staner.music.player.NotificationPlayerService;
import com.staner.tab.album.AlbumTab;
import com.staner.tab.other.OtherTab;
import com.staner.tab.playlist.PlaylistTab;
import com.staner.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the core of the Staner App. This activity:
 * 1. Instantiate the whole interfaces, and consequently the possibility the communication with the fragments.
 * 2. Is responsible to manage the thread that will handle all the functions with the audios: play, pause, prev, next, stop...
 * 3. Is responsible to manage the NotificationPlayerService, the service that communicate with this activity with the propose to handle the main thread while in background.
 * 4. Load image from galley.
 *
 * Created by Haroldo Shigueaki Teruya.
 */
public class MainActivity extends AppCompatActivity implements
        TabHost.OnTabChangeListener,
        NotificationPlayerService.NotificationPlayerServiceListener
{
    //=================================================================================================
    //=========================================== ATTRIBUTES ==========================================
    //=================================================================================================

    // class name
    public static final String TAG = MainActivity.class.getName();

    // tab host
    private static final int ANIMATION_TIME = 200;
    private TabHost tabHost;
    private View previousView;
    private View currentView;
    private int currentTab;

    // service notification
    private NotificationPlayerService notificationPlayerService;
    private boolean bound = false;

    // the entire data
    private List<MediaFileInfo> musicList = new ArrayList<>();
    private List<List<MediaFileInfo>> playlistList = new ArrayList<>();

    // dialog fragment used to load image from th gallery
    private DialogFragment dialogFragment = null;

    private PlayerController playerController = null;

    /**
     * This service is responsible to connect and disconnect the notification player music.
     */
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            NotificationPlayerService.LocalBinder binder = (NotificationPlayerService.LocalBinder) service;
            notificationPlayerService = binder.getService();
            bound = true;
            notificationPlayerService.setCallbacks(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            bound = false;
        }
    };

    //=================================================================================================
    //============================================ CONSTRUCTOR ========================================
    //=================================================================================================

    //=================================================================================================
    //============================================= METHODS ===========================================
    //=================================================================================================

    /**
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        getSupportFragmentManager().beginTransaction().add(R.id.music_fragment_content, new SplashScreenFragment()).addToBackStack(SplashScreenFragment.TAG).commit();

        playerController = new PlayerController(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume");
        stopService();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.d(TAG, "onStop");
        startService();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopService();
    }

    /**
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    /*
     *
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    /**
     *
     */
    public void createTab(List<MediaFileInfo> mediaFileInfoList, List<List<MediaFileInfo>> playlistList)
    {
        setMusicList(mediaFileInfoList);
        setPlaylistList(playlistList);

        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabHost.TabSpec albumTab = tabHost.newTabSpec("1");
        TabHost.TabSpec playlistTab = tabHost.newTabSpec("2");
        TabHost.TabSpec otherTab = tabHost.newTabSpec("3");

        albumTab.setIndicator(AlbumTab.tabHeader(this));
        albumTab.setContent(new AlbumTab(this));

        playlistTab.setIndicator(PlaylistTab.tabHeader(this));
        playlistTab.setContent(new PlaylistTab(this));

        otherTab.setIndicator(OtherTab.tabHeader(this));
        otherTab.setContent(new OtherTab(this));

        tabHost.addTab(albumTab);
        tabHost.addTab(playlistTab);
        tabHost.addTab(otherTab);
        tabHost.setOnTabChangedListener(this);

        this.previousView = tabHost.getCurrentView();
    }

    /**
     *
     */
    @Override
    public void onTabChanged(String tabId)
    {
        currentView = tabHost.getCurrentView();
        if (tabHost.getCurrentTab() > currentTab)
        {
            previousView.setAnimation(setProperties(new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f)));
            currentView.setAnimation(setProperties(new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f)));
        }
        else
        {
            previousView.setAnimation(setProperties(new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f)));
            currentView.setAnimation(setProperties(new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f)));
        }
        previousView = currentView;
        currentTab = tabHost.getCurrentTab();

        getSupportFragmentManager().popBackStackImmediate();
    }

    /**
     *
     */
    private Animation setProperties(Animation animation)
    {
        animation.setDuration(ANIMATION_TIME);
        animation.setInterpolator(new AccelerateInterpolator());
        return animation;
    }

    public void startService()
    {
        // if( ) we have to put a condition here, startService only if we have a song playing.
        {
            Intent serviceIntent = new Intent(MainActivity.this, NotificationPlayerService.class);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            serviceIntent.setAction(NotificationPlayerService.Constants.ACTION.STARTFOREGROUND_ACTION);
            startService(serviceIntent);
        }
    }

    private void stopService()
    {
        if (bound)
        {
            notificationPlayerService.setCallbacks(null);
            unbindService(serviceConnection);
            bound = false;
            stopService(new Intent(MainActivity.this, NotificationPlayerService.class));
        }
    }

    /**
     * this functions is called only by EditPlayListDialogFragment and CreatePlayListDialogFragment.
     * This method call onActivityResult
     */
    public void loadImageFromGallery(DialogFragment dialogFragment)
    {
        this.dialogFragment = dialogFragment;
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            Uri chosenImageUri = data.getData();
            try
            {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), chosenImageUri);
                if( bitmap != null )
                {
                    ImageView imageView = ((ImageView)dialogFragment.getView().findViewById(R.id.imageview));
                    imageView.setTag(chosenImageUri);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageBitmap(Util.getThumbnailFromImage(bitmap));
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void play(String playlistName, int musicId)
    {
        playerController.play(playlistName, musicId);
    }

    @Override
    public void resume()
    {
        playerController.resume();
    }

    @Override
    public void pause()
    {
        playerController.pause();
    }

    @Override
    public void prev()
    {
        playerController.prev();
    }

    @Override
    public void next()
    {
        playerController.next();
    }

    @Override
    public void stop()
    {
        playerController.stop();
    }

    /**
     * Return the music object by name.
     *
     * @param name String
     * @return MediaFileInfo | null
     */
    public MediaFileInfo getMusicByName(String name)
    {
        for( MediaFileInfo music : musicList )
        {
            if( music.getFileName().equalsIgnoreCase(name) )
            {
                return music;
            }
        }
        return null;
    }

    /**
     * Return the playlist that already exist by name.
     *
     * @param name String
     * @return List<MediaFileInfo> | null
     */
    public List<MediaFileInfo> getPlaylistByName(String name)
    {
        for( List<MediaFileInfo> mediaFileInfoList : playlistList )
        {
            if( mediaFileInfoList.get(0).getFilePlaylist().equalsIgnoreCase(name))
            {
                return mediaFileInfoList;
            }
        }
        return null;
    }

    /**
     * Add an music in a playlist by name of the music.
     * This method get the {@link MediaFileInfo} of the music by name and add in the playlist.
     * This method update the current playlistList and DataBase.
     *
     * @param playlistId int
     * @param name name
     */
    public void addMusicInPlaylist(int playlistId, String name)
    {
        for( List<MediaFileInfo> mediaFileInfoList : playlistList)
        {
            if( mediaFileInfoList.get(0).getId() == playlistId )
            {
                MediaFileInfo music = getMusicByName(name);
                if( music != null )
                {
                    // update here the playlistList
                    mediaFileInfoList.add(music);

                    // update here the DataBase
                    DataBaseController dataBaseController = new DataBaseController(this);
                    dataBaseController.insertMusicInPlaylist(name, playlistId);
                }
                return;
            }
        }
    }

    /**
     * Update an playlist of the list of playlist.
     *
     * @param playlistModel PlaylistModel
     */
    public void updatePlaylist(PlaylistModel playlistModel)
    {
        for ( List<MediaFileInfo> mediaFileInfoList : playlistList )
        {
            if( mediaFileInfoList.get(0).getId() == playlistModel.getId() )
            {
                mediaFileInfoList.get(0).setFilePlaylist(playlistModel.getName());
                mediaFileInfoList.get(0).setFileAlbumArt(Util.convertBitmapToByte(playlistModel.getArt()));
            }
        }
    }

    /**
     * Remove an playlist of the list of playlist.
     * Remove an playlist of the database.
     *
     * @param id int
     */
    public void removePlaylist(int id)
    {
        DataBaseController dataBaseController = new DataBaseController(this);
        dataBaseController.removePlaylistById(id);
        dataBaseController.close();

        for ( int i = 0; i <  playlistList.size(); i++  )
        {
            if( playlistList.get(i).get(0).getId() == id )
            {
                playlistList.remove(i);
            }
        }
    }

    //=================================================================================================
    //============================================== EVENTS ===========================================
    //=================================================================================================

    //=================================================================================================
    //=================================== SETTERS, GETTERS & OTHERS ===================================
    //=================================================================================================

    public List<MediaFileInfo> getMusicList()
    {
        return musicList;
    }

    public void setMusicList(List<MediaFileInfo> musicList)
    {
        this.musicList = musicList;
    }

    public void setPlaylistList(List<List<MediaFileInfo>> playlistList)
    {
        this.playlistList = playlistList;
    }

    public void addPlaylist(List<MediaFileInfo> mediaFileInfoList)
    {
        this.playlistList.add(mediaFileInfoList);
    }

    public List<List<MediaFileInfo>> getPlaylistList()
    {
        return playlistList;
    }

    //=================================================================================================
    //============================================== CLASS ============================================
    //=================================================================================================
}