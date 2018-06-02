package com.example.quinnm.socialmap;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.quinnm.socialmap.api.model.Message;
import com.example.quinnm.socialmap.api.model.User;
import com.example.quinnm.socialmap.api.service.MessageClient;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.MapView;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The main map view after the user has logged in.
 * Loads map from Mapbox
 * Single tap to activate toolbar, appear on top right.
 *
 * @author Keir Armstrong, Quinn Milinois
 * @since May 13, 2018
 *
 * REFERENCES:
 *  Mapbox API Reference
 *      https://www.mapbox.com/android-docs/api/map-sdk/6.1.3/index.html
 */
public class MainActivity extends AppCompatActivity implements NewMessageDialogFragment.NewMessageDialogListener{
    private static final String TAG = "MainActivity";

    private static final int REQUEST_ADD_MESSAGE_DIALOGFRAGMENT = 2;
    private static final int REQUEST_VIEW_FIRENDS_ACTIVITY = 3;
    private static final int REQUEST_VIEW_PROFILE_ACTIVITY = 4;

    private boolean toolbarVisible = false;
    private ImageButton _newMessageButton, _viewFriendsButton, _viewMyProfileButton;

    private static final User DEFAULT_USER = new User("root","root");

    private MapView mapView;
    private MapboxMap mapboxMap;
    private static final String MARKER_SOURCE = "markers-source";
    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
    private static final String MARKER_IMAGE = "custom-marker";
    private MapboxMap.OnMapClickListener addNewMarkerListener;
    private boolean addMarkerMode = false;
    private LatLng currentPoint;
    private ArrayList<LatLng> allMarkers = new ArrayList<>();

    private static final int REQUEST_FINE_LOCATION_PERMISSION = 1;
    private LocationManager locationManager;
    private String locationProvider;
    private Location lastKnownLocation;

    private String userID = ((ApplicationStore) this.getApplication()).getUserId();
    private String userName = ((ApplicationStore) this.getApplication()).getUsername();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get toolbar buttons
        _newMessageButton = findViewById(R.id.btn_new_message);
        _viewFriendsButton = findViewById(R.id.btn_view_friends);
        _viewMyProfileButton = findViewById(R.id.btn_view_my_profile);

        Mapbox.getInstance(this, getString(R.string.mapbox_token));
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                MainActivity.this.mapboxMap = mapboxMap;

//                TODO DEBUG THIS
//                getMessages();


                mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        // prevent toolbar toggling when adding marker
                        if (!addMarkerMode) {
                            setToolbarVisibility();
                        }
                    }
                });


            }
        });

        // wait for user to choose where to create new message and marker
        _newMessageButton.setOnClickListener(
                (View v) -> {
                    addMarkerMode = true;
                    Toast.makeText(MainActivity.this, "Choose a point to add a new message", Toast.LENGTH_LONG).show();
                    mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(@NonNull LatLng point) {
                            currentPoint = point;
                            onCreateNewMessage();
                            mapboxMap.removeOnMapClickListener(this);
                        }
                    });
                }
        );

        _viewFriendsButton.setOnClickListener(
                (View v) -> showFriendsListDialog()
        );

        _viewMyProfileButton.setOnClickListener(
                (View v) -> showMyProfileDialog()
        );
    }

    public void setToolbarVisibility() {
        if (toolbarVisible) {
            _newMessageButton.setVisibility(View.INVISIBLE);
            _viewFriendsButton.setVisibility(View.INVISIBLE);
            _viewMyProfileButton.setVisibility(View.INVISIBLE);
        }
        else {
            _newMessageButton.setVisibility(View.VISIBLE);
            _viewFriendsButton.setVisibility(View.VISIBLE);
            _viewMyProfileButton.setVisibility(View.VISIBLE);
        }
        toolbarVisible = !toolbarVisible;
    }

    public void onCreateNewMessage() {
        // TODO: DISABLE OUTSIDE AREA UNTIL CANCEL/CONFIRM
        FragmentManager fm = getSupportFragmentManager();
        NewMessageDialogFragment newMessageDialogFragment = NewMessageDialogFragment.newInstance("Some Title");
        newMessageDialogFragment.show(fm, "NewMessageDialogFragment");
    }

    public void showFriendsListDialog() {
        Toast.makeText(MainActivity.this, "friends list", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), ViewFriendsActivity.class);
        MainActivity.this.startActivity(intent);
    }

    public void showMyProfileDialog() {
        Toast.makeText(MainActivity.this, "your info", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), ViewProfileActivity.class);
        MainActivity.this.startActivity(intent);
    }

    public void getMessages() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("localhost:8000/socialmap/api/")
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

        MessageClient client = retrofit.create(MessageClient.class);
        Call<Message> call = client.getMessages(DEFAULT_USER);

        call.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                Log.d(TAG,"Message response" + response.toString());
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Toast.makeText(getBaseContext(), t.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
    public void addMessages(String userID, String Username, String Body, String Location){
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("localhost:8000/socialmap/api/")
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

        Message testMessage = new Message(userID,Username,Body,Location);

        MessageClient client = retrofit.create(MessageClient.class);
        Call<Message> call = client.addMessage(testMessage);

        call.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {

            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Toast.makeText(getBaseContext(), t.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

//msg=
//    {
//        "token": token,    # Type: string
//        "username": username,
//        "data":  data,    # Object containing data
//        "body": body    # Type: string
//    }


    @Override
    public void OnFinishNewMessage(String messageText) {
//        String userID;
//        String userName;
        Toast.makeText(MainActivity.this, "Creating new marker at: " + currentPoint.toString(), Toast.LENGTH_LONG).show();
        mapboxMap.addMarker(new MarkerOptions()
                .position(currentPoint)
                .snippet(messageText)
        );
        addMessages(userID,userName,messageText,currentPoint.toString());

        currentPoint = null;
        addMarkerMode = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
