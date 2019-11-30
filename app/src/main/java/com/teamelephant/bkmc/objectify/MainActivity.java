package com.teamelephant.bkmc.objectify;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.view.View;

import de.crysxd.cameraXTracker.CameraFragment;
import de.crysxd.cameraXTracker.ar.ArOverlayView;
import de.crysxd.cameraXTracker.ar.BoundingBoxArOverlay;
import de.crysxd.cameraXTracker.ar.PathInterpolator;
import de.crysxd.cameraXTracker.ar.PositionTranslator;

public class MainActivity extends AppCompatActivity {
    private ObjectifyAnalyzer imageAnalyzer = new ObjectifyAnalyzer();
    private CameraFragment camera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.cameraFragment);
        BoundingBoxArOverlay boundingBoxArOverlay = new BoundingBoxArOverlay(this, BuildConfig.DEBUG);
        imageAnalyzer = ViewModelProviders.of(this).get(ObjectifyAnalyzer.class);

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
}
