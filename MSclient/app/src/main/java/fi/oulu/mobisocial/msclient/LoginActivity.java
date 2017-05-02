package fi.oulu.mobisocial.msclient;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button button = (Button) findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Normally this would call login-API with given parameters from text fields
                //Code is supporting real logins, but fake login is used by default
                new HttpRequestPostLogin().execute("example1", "example1p");
            }
        });
    }

    private void startMainApp(){
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        startActivity(intent);
    }

    private class HttpRequestPostLogin extends AsyncTask<String,Void,String> {
        protected void onPreExecute() {

        }

        protected String doInBackground(String... params) {
            //from http://stackoverflow.com/questions/9767952/how-to-add-parameters-to-httpurlconnection-using-post
            try {
                URL url = new URL("http://10.0.2.2:5000/login");
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);

                    JSONObject cred = new JSONObject();
                    cred.put("username", params[0]);
                    cred.put("password", params[1]);

                    OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                    writer.write(cred.toString());

                    writer.flush();
                    writer.close();

                    return urlConnection.getResponseMessage();

                } catch (IOException e) {
                    //dostuff
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException ex) {
                //do exception handling here
            }

            return "connection failed";
        }


        protected void onPostExecute(String s) {
            if(s == "OK") {
                //Normally would start app here
                //startMainApp();
            }
            startMainApp();
        }
    }


}
