package com.teamelephant.bkmc.objectify;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.crysxd.cameraXTracker.ThreadedImageAnalyzer;
import de.crysxd.cameraXTracker.ar.ArObject;
import de.crysxd.cameraXTracker.ar.ArObjectTracker;

public class ObjectifyAnalyzer extends ViewModel implements ThreadedImageAnalyzer{
    public ArObjectTracker arObjectTracker = new ArObjectTracker();
    private AtomicBoolean isBusy = new AtomicBoolean(false);
    private AtomicBoolean alsoBusy = new AtomicBoolean(false);
    private HandlerThread handlerThread = new HandlerThread("ObjectifyAnalyzer");
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    FirebaseVisionObjectDetectorOptions options =
            new FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()
                    .build();
    FirebaseVisionObjectDetector objectDetector =
            FirebaseVision.getInstance().getOnDeviceObjectDetector(options);
    FirebaseVisionImageLabeler objectLabeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();
    ExecutorService executor = Executors.newFixedThreadPool(10);
    TextCallback textCallback = null;


    public ObjectifyAnalyzer(TextCallback callback){
        this.textCallback = callback;
        handlerThread.start();
    }
    @Override
    public Handler getHandler(){
        return new Handler(handlerThread.getLooper());
    }
    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }
    @Override
    public void analyze(ImageProxy imageProxy, int degrees){
        if(imageProxy != null && isBusy.compareAndSet(false,true)){
            int rotation = degreesToFirebaseRotation(degrees);
            FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(imageProxy.getImage(), rotation);
            Size imageSize = new Size(imageProxy.getWidth(),imageProxy.getHeight());
            objectDetector.processImage(image).addOnCompleteListener(
                    new OnCompleteListener<List<FirebaseVisionObject>>() {
                        @Override
                        public void onComplete(@NonNull Task<List<FirebaseVisionObject>> task) {
                            isBusy.set(false);
                            if(task.getException() != null){
                                Log.e(null, task.getException().toString());
                                return;
                            }
                            FirebaseVisionObject object = (!task.getResult().isEmpty()) ? task.getResult().get(0) : null;
                            ArObject arObject = (object != null) ? new ArObject(object.getTrackingId(), new RectF(object.getBoundingBox()),imageSize,degrees) : null;
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    arObjectTracker.processObject(arObject);
                                }
                            });
                            if(object != null && alsoBusy.compareAndSet(false, true)){
                                executor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Rect rect = object.getBoundingBox();
                                        Bitmap bitmap = Bitmap.createBitmap(image.getBitmap(),  rect.left, rect.top,rect.width(),rect.height());

                                        objectLabeler.processImage(FirebaseVisionImage.fromBitmap(bitmap))
                                                .addOnCompleteListener(
                                                        new OnCompleteListener<List<FirebaseVisionImageLabel>>() {
                                                            @Override
                                                            public void onComplete(Task<List<FirebaseVisionImageLabel>> detectedObjects) {
                                                                // Task completed successfully
                                                                alsoBusy.set(false);
                                                                if (detectedObjects.getException() != null) {
                                                                    Log.e(null, detectedObjects.getException().toString());
                                                                    return;
                                                                }
                                                                FirebaseVisionImageLabel label = null;
                                                                if(!detectedObjects.getResult().isEmpty()){
                                                                    label = detectedObjects.getResult().get(0);
                                                                }
                                                                if(label != null) {
                                                                    if(label.getConfidence()>0.5){
                                                                    String result = label.getText();
                                                                    if(textCallback != null){ textCallback.updateText(result); }
                                                                    Log.e("CameraXDemo", "######## id: " + result);
                                                                    Log.e("CameraXDemo", "----------------------------");

                                                                }
                                                                }
                                                            }
                                                        });
                                    }
                                });
                            }
                        }
                    }
            );
        }
    }
}
