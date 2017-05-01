package fi.oulu.mobisocial.msclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchiveActivity extends AppCompatActivity {

    // https://www.learn2crack.com/2013/11/listview-from-json-example.html
    // http://techlovejump.com/android-poplulating-list-view-from-json/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        new HttpRequestGetArchive().execute();


    }

    private class HttpRequestGetArchive extends AsyncTask<Void,Void,String> {
        protected void onPreExecute() {
            //display progress dialog.

        }
        protected String doInBackground(Void... params) {
            String querystring = "q={\"user_id\":1}";


            StringBuilder result = new StringBuilder();

            URL url = null;
            try {
                url = new URL("http://10.0.2.2:5000/api/messages_found?" + querystring);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                urlConnection.disconnect();

                return result.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "Request failed";
        }



        protected void onPostExecute(String result) {
            Log.v("sdf", result);
            try{
                JSONObject jsonResponse = new JSONObject(result);
                JSONArray jsonMainNode = jsonResponse.getJSONArray("objects");

                for(int i = 0; i<jsonMainNode.length();i++){
                    JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                    JSONObject message = jsonChildNode.getJSONObject("message");
                    String time = message.getString("timestamp");
                    String sendername = message.getString("sendername");
                    String text = message.getString("message");
                    messageList.add(createMessage(sendername, time, text));

                    Log.v("sdf", text);
                }
            }
            catch(JSONException e){
                e.printStackTrace();
            }



            ListView listView = (ListView) findViewById(R.id.archive_list_view);
            SimpleAdapter simpleAdapter = new SimpleAdapter(getApplicationContext(), messageList, android.R.layout.simple_list_item_1, new String[] {"item"}, new int[] {android.R.id.text1});
            listView.setAdapter(simpleAdapter);
        }
    }

    List<Map<String,String>> messageList = new ArrayList<Map<String,String>>();

    private HashMap<String, String>createMessage(String user,String time, String text){
        HashMap<String, String> message = new HashMap<String, String>();
        message.put("item", user + "\n" + time + "\n" + text);
        return message;
    }
}
