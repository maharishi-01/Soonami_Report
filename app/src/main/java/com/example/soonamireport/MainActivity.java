package com.example.soonamireport;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String USGS_REQUEST_URL ="https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2012-01-01&endtime=2012-12-01&minmagnitude=8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TsunamiAsyncTask task = new TsunamiAsyncTask();
        task.execute();
    }

    /**
     * Update the screen to display information from the given {@link Event}.
     */
    private void updateUi(Event earthquake) {
        // Display the earthquake title in the UI
        TextView titleTextView = (TextView) findViewById(R.id.title);
        titleTextView.setText(earthquake.title);

        // Display the earthquake date in the UI
        TextView dateTextView = (TextView) findViewById(R.id.date);
        dateTextView.setText(getDateString(earthquake.time));

        // Display whether or not there was a tsunami alert in the UI
        TextView tsunamiTextView = (TextView) findViewById(R.id.tsunami_alert);
        tsunamiTextView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    /**
     * Returns a formatted date and time string for when the earthquake happened.
     */
    private String getDateString(long timeInMilliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(timeInMilliseconds);
    }

    /**
     * Return the display string for whether or not there was a tsunami alert for an earthquake.
     */
    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    private class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {
        protected Event doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(USGS_REQUEST_URL);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                // TODO Handle the IOException
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            Event earthquake = extractFeatureFromJson(jsonResponse);

            // Return the {@link Event} object as the result fo the {@link TsunamiAsyncTask}

            return earthquake;
        }

        @Override
        protected void onPostExecute(Event earthquake) {
            if (earthquake == null) {
                return;
            }

            updateUi(earthquake);
        }


      private URL createUrl(String stringUrl)
      {
          URL url=null;
          try{
              url=new URL(stringUrl);
          }

          catch (MalformedURLException exception){

              Log.e(LOG_TAG,"Error ith creating URL",exception);
              return null;
          }
       return url;

      }

      private String makeHttpRequest(URL url) throws IOException{
            String jsonResponse="";
          HttpURLConnection urlConnection=null;
          InputStream inputStream=null;
          try{

              urlConnection=(HttpURLConnection)url.openConnection();
              urlConnection.setRequestMethod("GET");
              urlConnection.setReadTimeout(100000);
              urlConnection.setConnectTimeout(15000);
              urlConnection.connect();
              inputStream=urlConnection.getInputStream();
              jsonResponse=readFromStream(inputStream);
          }
          catch (IOException e) {
          }finally {
              if (urlConnection!=null)
              {
                  urlConnection.disconnect();
              }
              if (inputStream!=null){
                  inputStream.close();
              }
          }
          return jsonResponse;

      }

      private  String readFromStream(InputStream inputStream)throws IOException{
            StringBuilder output=new StringBuilder();
            if(inputStream!=null){
                InputStreamReader inputStreamReader=new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader=new BufferedReader(inputStreamReader);
                String line=reader.readLine();
                while (line!=null){

                    output.append(line);
                    line=reader.readLine();
                }
            }
            return output.toString();

      }

      private Event extractFeatureFromJson(String earthquakeJSON){

            try{
                JSONObject baseJsonResponse=new JSONObject(earthquakeJSON);
                JSONArray featureArray=baseJsonResponse.getJSONArray("features");
                if(featureArray.length()>0){
                    JSONObject firstFeature=featureArray.getJSONObject(0);

                    JSONObject properties = firstFeature.getJSONObject("properties");

                    // Extract out the title, time, and tsunami values
                    String title = properties.getString("title");
                    long time = properties.getLong("time");
                    int tsunamiAlert = properties.getInt("tsunami");

                    // Create a new {@link Event} object
                    return new Event(title, time, tsunamiAlert);
                }

            } catch (JSONException e){
                Log.e(LOG_TAG,"problem prasing the earthquake JSON results",e);
            }
            return null;
      }

    }

}
