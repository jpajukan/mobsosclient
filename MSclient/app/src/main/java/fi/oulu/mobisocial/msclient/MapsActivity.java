package fi.oulu.mobisocial.msclient;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, PostMessageDialogFragment.OnPostMessageListener {

    private GoogleMap mMap;

    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;

    private GoogleApiClient mGoogleApiClient;

    private FragmentManager fManager = getSupportFragmentManager();
    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    // Set user to be always first user
    private int userId = 1;

    @Override
    public FragmentManager getSupportFragmentManager() {
        return super.getSupportFragmentManager();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDrawerList = (ListView) findViewById(R.id.navList);

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        addDrawerItems();

        // https://developer.android.com/training/location/retrieve-current.html#play-services
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        Button button = (Button) findViewById(R.id.postMessageDialogButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showMessagePostDialog();
            }
        });





    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    private void addDrawerItems() {
        String[] osArray = {"Archive", "Help"};
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Oulu and move the camera
        LatLng start = new LatLng(65, 25);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start , 7.0f));

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(getApplicationContext());
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getApplicationContext());
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(getApplicationContext());
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

    }

    private void showMessagePostDialog(){
        PostMessageDialogFragment pmDialog = new PostMessageDialogFragment();
        pmDialog.show(fManager, "postMessageDialog");
    }

    private void putMessageOnMap(double lat, double lon, String title, String message) {
        LatLng gps = new LatLng(lat, lon);
        mMap.addMarker(new MarkerOptions().position(gps).title(title).snippet(message));
    }

    private void removeAllMessagesOnMap() {
        mMap.clear();
    }

    private void getMessagesNearbyAndPutOnMap(Location loc) {
        removeAllMessagesOnMap();

        LatLng test = new LatLng(loc.getLatitude(), loc.getLongitude());
        new HttpRequestGetMessagesNearby().execute(test);

    }

    private void updateMap(){

        Location lastLoc = getLoc();

        if(lastLoc != null) {
            getMessagesNearbyAndPutOnMap(lastLoc);

            LatLng newloc = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newloc , 7.0f));

        }


    }

    private Location getLoc() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            return mLastLocation;
        }

        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //http://stackoverflow.com/questions/43503157/google-api-client-location-updates-not-working-and-lastknownlocation-is-null-aft
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        updateMap();
    }

    @Override
    public void OnPostMessageSubmit(String msg) {
        //post message
        Location curLoc = getLoc();
        LatLng curLatLong = new LatLng(curLoc.getLatitude(), curLoc.getLongitude());

        PostMessageParams p = new PostMessageParams(curLatLong, msg, userId);

        new HttpRequestPostMessage().execute(p);
    }


    private class HttpRequestGetMessagesNearby extends AsyncTask<LatLng,Void,String> {
        protected void onPreExecute() {

        }
        protected String doInBackground(LatLng... params) {
            String querystring = String.format("q={\"lat\":%f, \"long\":%f}", params[0].latitude, params[0].longitude);

            URL url = null;
            StringBuilder result = new StringBuilder();


            try {
                url = new URL("http://10.0.2.2:5000/api/messages?" + querystring);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            InputStream in = null;
            try {
                in = new BufferedInputStream(urlConnection.getInputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            urlConnection.disconnect();

            return result.toString();

        }



        protected void onPostExecute(String result) {
            // http://www.androidhive.info/2012/01/android-json-parsing-tutorial/
            JSONObject myJson = null;
            try {
                myJson = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            try {
                JSONArray messages = myJson.getJSONArray("objects");
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject msg = messages.getJSONObject(i);
                    Log.v("testi", msg.toString());

                    String sendername = msg.getString("sendername");
                    String timestamp = msg.getString("timestamp");
                    String text = msg.getString("message");

                    double lon = msg.getDouble("longitude");
                    double lat = msg.getDouble("latitude");

                    String formattedTimestamp = timestampConverter(timestamp);

                    putMessageOnMap(lat, lon, formattedTimestamp + " by " + sendername, text);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private class HttpRequestGetMessages extends AsyncTask<Void,Void,String> {
        protected void onPreExecute() {
            //display progress dialog.

        }
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL("http://10.0.2.2:5000/api/messages/1");
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        String s = convertStreamToString(in);

                        return s;

                    } finally {
                        urlConnection.disconnect();
                    }

                }catch(IOException e){
                    //dostuff
                }
            }catch(MalformedURLException ex){
                //do exception handling here
            }

            return "connection failed";
        }



        protected void onPostExecute(String result) {
            // dismiss progress dialog and update ui

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.common_full_open_on_phone)
                            .setContentTitle("My notification")
                            .setContentText(result);

            // Gets an instance of the NotificationManager service//

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

//When you issue multiple notifications about the same type of event, it’s best practice for your app to try to update an existing notification with this new information, rather than immediately creating a new notification. If you want to update this notification at a later date, you need to assign it an ID. You can then use this ID whenever you issue a subsequent notification. If the previous notification is still visible, the system will update this existing notification, rather than create a new one. In this example, the notification’s ID is 001//

            mNotificationManager.notify(001, mBuilder.build());
        }

        //from stackoverflow http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
        private String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }


        //http://stackoverflow.com/questions/12069669/how-can-you-pass-multiple-primitive-parameters-to-asynctask
        private class PostMessageParams {
            LatLng gps;
            String message;
            int senderid;

            PostMessageParams(LatLng location, String msg, int sender) {
                this.gps = location;
                this.message = msg;
                this.senderid = sender;
            }
        }

        private class HttpRequestPostMessage extends AsyncTask<PostMessageParams,Void,String> {
            protected String doInBackground(PostMessageParams... params) {
                URL url = null;
                try {
                    url = new URL("http://10.0.2.2:5000/api/messages");

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("sender", params[0].senderid);
                    payload.put("message", params[0].message);
                    payload.put("latitude", params[0].gps.latitude);
                    payload.put("longitude", params[0].gps.longitude);

                    OutputStreamWriter writer= new OutputStreamWriter(urlConnection.getOutputStream());
                    writer.write(payload.toString());
                    writer.flush();
                    writer.close();

                    return urlConnection.getResponseMessage();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                return "failed";
            }



        protected void onPostExecute(String s) {
            // update messages on map
            updateMap();
        }



        //from stackoverflow http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
        private String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    // https://developer.android.com/training/implementing-navigation/nav-drawer.html
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    /** Swaps fragments in the main content view */
    private void selectItem(int position){
        //mDrawerLayout.closeDrawer(mDrawerList);

        Intent intent;

        switch (position) {
            case 0:
                intent = new Intent(getApplicationContext(),ArchiveActivity.class);
                startActivity(intent);
                break;
            case 1:
                intent= new Intent(getApplicationContext(),HelpActivity.class);
                startActivity(intent);
            default:
                break;
        }
    }

    private String timestampConverter(String time){
        //http://stackoverflow.com/questions/15730298/java-format-yyyy-mm-ddthhmmss-sssz-to-yyyy-mm-dd-hhmmss
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = null;
        try {
            d = sdf.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String formattedTime = output.format(d);

        return formattedTime;
    }
}
