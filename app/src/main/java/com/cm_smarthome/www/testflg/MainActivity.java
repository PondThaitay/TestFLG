package com.cm_smarthome.www.testflg;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.widget.LoginButton.UserInfoChangedCallback;
import com.facebook.widget.ProfilePictureView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

public class MainActivity extends FragmentActivity {

    Context context = this;

    private LoginButton loginBtn;
    private Button postImageBtn;
    private Button updateStatusBtn;

    GoogleMap mMap;
    Marker mMarker;
    LocationManager lm;
    double lat, lng;

    private TextView userName;

    private UiLifecycleHelper uiHelper;

    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private static String message = "ทดสอบ Post ผ่านแอพ ทดลอง";

    ImageView imageView;
    private static final int SELECT_PICTURE = 1;
    private String selectedImagePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.profile_image);
        mMap = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);

        userName = (TextView) findViewById(R.id.user_name);

        loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
        loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    userName.setText(user.getName());
                    ProfilePictureView pictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
                    pictureView.setProfileId(user.getId());
                    Picasso.with(context).load("https://graph.facebook.com/" + user.getId() + "/picture?type=large").into(imageView);
                    //imageView.setImageResource(R.mipmap.ic_launcher);
                    Log.e("user_id", user.getId() + user.getLocation());
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                    userName.setText("You are not logged");
                }
            }
        });

        postImageBtn = (Button) findViewById(R.id.post_image);
        postImageBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }
        });

        updateStatusBtn = (Button) findViewById(R.id.update_status);
        updateStatusBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                postStatusMessage();
            }
        });

        buttonsEnabled(false);

    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    LocationListener listener = new LocationListener() {
        public void onLocationChanged(Location loc) {
            LatLng coordinate = new LatLng(loc.getLatitude(), loc.getLongitude());
            lat = loc.getLatitude();
            lng = loc.getLongitude();

            if (mMarker != null)
                mMarker.remove();

            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    coordinate, 16));
        }


        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            if (state.isOpened()) {
                buttonsEnabled(true);
                Log.d("FacebookSampleActivity", "Facebook session opened");
            } else if (state.isClosed()) {
                buttonsEnabled(false);
                Log.d("FacebookSampleActivity", "Facebook session closed");
            }
        }
    };

    public void buttonsEnabled(boolean isEnabled) {
        postImageBtn.setEnabled(isEnabled);
        updateStatusBtn.setEnabled(isEnabled);
    }

    public void postImage(Bitmap input) {
        if (checkPermissions()) {
            Bitmap img = input;
            Request uploadRequest = Request.newUploadPhotoRequest(
                    Session.getActiveSession(), img, new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            Toast.makeText(context, "Photo uploaded successfully", Toast.LENGTH_LONG).show();
                        }
                    });
            uploadRequest.executeAsync();
        } else {
            requestPermissions();
        }
    }

    public void postStatusMessage() {
        if (checkPermissions()) {
            Request request = Request.newStatusUpdateRequest(
                    Session.getActiveSession(), message,
                    new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            if (response.getError() == null)
                                Toast.makeText(context, "Status updated successfully", Toast.LENGTH_LONG).show();
                        }
                    });
            request.executeAsync();
        } else {
            requestPermissions();
        }
    }

    public boolean checkPermissions() {
        Session s = Session.getActiveSession();
        if (s != null) {
            return s.getPermissions().contains("publish_actions");
        } else
            return false;
    }

    public void requestPermissions() {
        Session s = Session.getActiveSession();
        if (s != null)
            s.requestNewPublishPermissions(new Session.NewPermissionsRequest(
                    this, PERMISSIONS));
    }

    @Override
    public void onResume() {
        super.onResume();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isNetwork =
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean isGPS =
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (isNetwork) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER
                    , 5000, 10, listener);
            Location loc = lm.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                lat = loc.getLatitude();
                lng = loc.getLongitude();
            }
        }

        if (isGPS) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER
                    , 5000, 10, listener);
            Location loc = lm.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);
            if (loc != null) {
                lat = loc.getLatitude();
                lng = loc.getLongitude();
            }
        }

        uiHelper.onResume();
        buttonsEnabled(Session.getActiveSession().isOpened());
    }

    @Override
    public void onPause() {
        super.onPause();
        lm.removeUpdates(listener);
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);
                System.out.println("Image Path : " + selectedImagePath);
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);
                postImage(bitmap);
                //img.setImageURI(selectedImageUri);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        uiHelper.onSaveInstanceState(savedState);
    }

   /* public Bitmap getImageProfileFacebook(String user_id) {
        String imageURL;
        Bitmap bitmap = null;
        imageURL = "http://graph.facebook.com/" + user_id + "/picture?type=small";

        try {
            bitmap = BitmapFactory.decodeStream((InputStream) new URL(imageURL).getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }*/
}