package com.example.miguelamores.item;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);



        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {

        private MediaRecorder mRecorder = null;
        double powerDb = 0;
        Button boton;
        EditText nombre;
        TextView tex;
        Button btnPlay, btnParar;
        int amp;
        Timer myTimer = null;

        GPSTracker gps;

        private CallbackManager mCallbackManager;
        private FacebookCallback<LoginResult> mCallback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = loginResult.getAccessToken();
                Profile profile = Profile.getCurrentProfile();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {

            }
        };

        public PlaceholderFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
            mCallbackManager = CallbackManager.Factory.create();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);






            boton = (Button)rootView.findViewById(R.id.button);
            nombre = (EditText)rootView.findViewById(R.id.editText);
            tex = (TextView)rootView.findViewById(R.id.textView);
            btnPlay = (Button)rootView.findViewById(R.id.btnPlay);
            btnParar = (Button)rootView.findViewById(R.id.btnParar);



            btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    gps = new GPSTracker(rootView.getContext());

                    if (gps.canGetLocation()){
                        double latitude = gps.getLatitude();
                        double longitude = gps.getLongitude();

                        Toast.makeText(
                                getApplicationContext(),
                                "Your location is -\nLat: " + latitude + "\nLong: "
                                        + longitude, Toast.LENGTH_LONG).show();
                    }else {
                        gps.showSettingsAlert();
                    }

                    try {
                        start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //powerDb = (20 * Math.log10((double)mRecorder.getMaxAmplitude()));
                    //tex.setText(String.valueOf(powerDb));
                    if (myTimer != null){
                        myTimer.cancel();
                    }

                    myTimer = new Timer();
                    myTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    powerDb = 0;

                                    tex.setText(String.valueOf(powerDb));
                                    //powerDb = (20 * Math.log10((double) mRecorder.getMaxAmplitude()/2));
                                    powerDb = (20 * Math.log10(mRecorder.getMaxAmplitude()/1));
                                    tex.setText(String.valueOf(String.format("%.2f",powerDb)));
                                }
                            });
                        }
                    }, 0, 300);

                }
            });



            btnParar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stop();

                }
            });




            boton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tex.setText(nombre.getText().toString());
                    Toast.makeText(rootView.getContext(), "hola miguel", Toast.LENGTH_LONG).show();

                }
            });

            LoginButton loggin = (LoginButton)rootView.findViewById(R.id.login_button);
            loggin.setReadPermissions("user_friends");
            //loggin.setFragment(this);
            loggin.registerCallback(mCallbackManager, mCallback);

            return rootView;
        }

        private void loadMic(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }

                }
            }).start();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

        public double getAmplitude() {
            if (mRecorder != null)
                return (mRecorder.getMaxAmplitude());
            else
                return 0;

        }

        public void start() throws IOException {

            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null");
                mRecorder.prepare();
                mRecorder.start();

            }
        }

        public void stop() {
            if (myTimer != null){
                myTimer.cancel();
            }

            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
        }
    }


}
