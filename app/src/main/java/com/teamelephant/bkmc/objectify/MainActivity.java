package com.teamelephant.bkmc.objectify;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import de.crysxd.cameraXTracker.CameraFragment;
import de.crysxd.cameraXTracker.ar.ArOverlayView;
import de.crysxd.cameraXTracker.ar.BoundingBoxArOverlay;
import de.crysxd.cameraXTracker.ar.PathInterpolator;
import de.crysxd.cameraXTracker.ar.PositionTranslator;

public class MainActivity extends AppCompatActivity implements TextCallback {
    private ObjectifyAnalyzer imageAnalyzer = new ObjectifyAnalyzer(this);
    private CameraFragment camera;
    TextView textView;
    String text;
    //private AtomicBoolean isSpeaking = new AtomicBoolean(false);
    private TextToSpeech textToSpeech;
    public MainActivity(){ }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){

                    int ttsLang = textToSpeech.setLanguage(Locale.CANADA);
                    textToSpeech.setVoice(new Voice(null,null, Voice.QUALITY_VERY_HIGH,0,false,null));
                    if(ttsLang == TextToSpeech.LANG_MISSING_DATA || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED){
                        Toast.makeText(getApplicationContext(), "The Language is not supported!",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!",Toast.LENGTH_SHORT).show();
                }
            }
        });
        textView = findViewById(R.id.imageLabel);
        camera = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.cameraFragment);
        BoundingBoxArOverlay boundingBoxArOverlay = new BoundingBoxArOverlay(this, BuildConfig.DEBUG);
        //imageAnalyzer = ViewModelProviders.of(this).get(ObjectifyAnalyzer.class);
        imageAnalyzer = ViewModelProviders.of(this,new AnalyzerFactory(this.getApplication(),this)).get(ObjectifyAnalyzer.class);

        camera.setImageAnalyzer(imageAnalyzer);
        camera.getArOverlayView().observe(camera, new Observer<ArOverlayView>() {
            @Override
            public void onChanged(ArOverlayView arOverlayView) {
                arOverlayView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                        imageAnalyzer.arObjectTracker.pipe(
                                new PositionTranslator(view.getWidth(),view.getHeight()))
                                .pipe(new PathInterpolator())
                                .addTrackingListener(boundingBoxArOverlay);
                    }
                });
                arOverlayView.add(boundingBoxArOverlay);
            }
        });
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
    @Override
   // public void updateText(String text){ ((TextView)findViewById(R.id.testtext)).setText(text); }
    public void updateText(String t){ text = t; textView.setText(t); }
    public void onScreenTap(View view){
        if(!textToSpeech.isSpeaking()){
           // String text = (String)((TextView)findViewById(R.id.testtext)).getText();

            int speechStatus = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null,"");
            //textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "");
            if(speechStatus == TextToSpeech.ERROR){
                Log.e("TTS", "Error in converting Text to Speech!");
            }
        }
    }
}
