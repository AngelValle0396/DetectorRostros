package com.example.practica1;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

import com.google.android.gms.vision.face.FaceDetector;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.face.Face;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.android.gms.vision.Frame;
import com.google.api.services.vision.v1.model.Landmark;
import com.google.api.services.vision.v1.model.Position;
import com.google.api.services.vision.v1.model.TextAnnotation;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Vision vision;
    Bitmap bitmap;
    Bitmap tempBitmap;
    Canvas tempCanvas;
    ImageView imagen;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                        new AndroidJsonFactory(),  null);
        visionBuilder.setVisionRequestInitializer(new
                VisionRequestInitializer("AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs"));
        vision = visionBuilder.build();

    }

    public Image getImageToProcess(){
        imagen = (ImageView)findViewById(R.id.imgImgToProcess);
        BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable(); 
        bitmap = drawable.getBitmap();
       
        bitmap = scaleBitmapDown(bitmap, 1200);
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();   
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
         
        byte[] imageInByte = stream.toByteArray();
        
        Image inputImage = new Image();   
        inputImage.encodeContent(imageInByte);    
        return inputImage;
    }

    public BatchAnnotateImagesRequest setBatchRequest(String TipoSolic, Image inputImage){ 
        Feature desiredFeature = new Feature();  
        desiredFeature.setType(TipoSolic);   

        AnnotateImageRequest request = new AnnotateImageRequest();
        request.setImage(inputImage);   
        request.setFeatures(Arrays.asList(desiredFeature));


        BatchAnnotateImagesRequest batchRequest =  new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Arrays.asList(request));    
        return batchRequest;
        }


    public void ProcesarTexto(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("FACE_DETECTION",
                        getImageToProcess());
                try {

                    FaceDetector faceDetector = new
                            FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                            .build();
                    if(!faceDetector.isOperational()){
                        //new AlertDialog.Builder(this).setMessage("Could not set up the face detector!").show();
                        return;
                    }

                    Vision.Images.Annotate  annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response  = annotateRequest.execute();


                    List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations()

                    //final StringBuilder message = new StringBuilder("Se ha encontrado los siguientes Objetos:\n\n");
                   // final TextAnnotation text = response.getResponses().get(0).getFullTextAnnotation();
                    /*List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
                    if (labels != null) {
                        for (EntityAnnotation label : labels)
                               message.append(String.format(Locale.US, "%.2f: %s\n",
                                       label.getScore()*100, label.getDescription()));
                    } else {
                        message.append("No hay ningún Objeto");
                    }*/


                    ;int numberOfFaces = faces.size();
                    String likelihoods = "";
                    //Paint landmarksPaint = new Paint();
                    //landmarksPaint.setStrokeWidth(10);
                    //landmarksPaint.setColor(Color.RED);
                    //landmarksPaint.setStyle(Paint.Style.STROKE);

                    //Create a Canvas object for drawing on
                    // Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                    //tempCanvas = new Canvas(bitmap);
                    //tempCanvas.drawBitmap(bitmap, 0, 0, null);

                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<Face> facese = faceDetector.detect(frame);

                    for(int i=0; i<numberOfFaces; i++)
                    {
                        likelihoods += "\n Rostro " + i + "  "  +  faces.get(i).getJoyLikelihood();
                    //List<Landmark> listapuntos = faces.get(i).getLandmarks();
                    //detectFace(listapuntos,landmarksPaint);
                        detectFace(bitmap,facese);
                    }

                    final String message =   "Esta imagen tiene " + numberOfFaces + " rostros " + likelihoods;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imagen.setImageDrawable(new BitmapDrawable(getResources(),bitmap));
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.txtResult);
                            //imageDetail.setText(text.getText());
                            imageDetail.setText(message.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        });
    }

    private void detectFace(Bitmap myBitmap,SparseArray<Face> faces ){
        //Draw Rectangles on the Faces
        //for(int l=0; l<landmarks.size(); l++){
          //  Position pos = landmarks.get(l).getPosition();
           // tempCanvas.drawPoint(pos.getX(), pos.getY(), landmarksPaint);
        //}
//Create a Paint object for drawing with
             // imagen.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        //tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(bitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);
        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
            int originalWidth = bitmap.getWidth();   
            int originalHeight = bitmap.getHeight();
            int resizedWidth = maxDimension;    
            int resizedHeight = maxDimension;   
            if (originalHeight > originalWidth) {
                 resizedHeight = maxDimension;
                 resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
            } else if (originalWidth > originalHeight) {
                resizedWidth = maxDimension;
                resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
            } else if (originalHeight == originalWidth) {
                resizedHeight = maxDimension;     
                resizedWidth = maxDimension;    
            }   
             return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
            }
}