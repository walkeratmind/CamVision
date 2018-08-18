package com.example.rakesh.camvision;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.rakesh.camvision.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 3.0f;
    private static final int ALPHA = 120;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;

    private Context context;

    FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);

        this.context = context;

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);
        mIdPaint.setAlpha(ALPHA);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
        mBoxPaint.setAlpha(ALPHA);
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
        canvas.drawText("happiness: " + String.format("%.2f", face.getIsSmilingProbability())
                , x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability())
                , x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint);
        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability())
                , x - ID_X_OFFSET * 2, y - ID_Y_OFFSET * 2, mIdPaint);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        giveInfo();
    }
    public void giveInfo() {

        View view = LayoutInflater.from(context).inflate(R.layout.activity_main, null);
        TextView textView = view.findViewById(R.id.face_updates);

        String data=textView.getText().toString();

        int len = data.length();

        String line =mFace.getId()+" "+ getUpdates();

        if(len>60){

            String partial_data = data.substring(len-30,len);

            if(partial_data.contains(line)){

//do nothing

            }else{

                textView.append("\nUserId:"+line);

            }
        }else{

            textView.append("\nUserId:"+line);

        }

        final ScrollView mScrollView= view.findViewById(R.id.scroll_view);

        mScrollView.postDelayed(new Runnable() {

            @Override

            public void run() {

                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);

            }

        }, 600);

    }
    private String getPrediction(float eulerY, float eulerZ) {

        String feature="";

        if(eulerZ<5f && eulerZ >=0f){

            if(eulerY>0f && eulerY<60f){

                feature="Facing straight right";

            }else{

                feature="no tilt";

            }

        }else if(eulerZ>5f && eulerZ<45f){

            if(eulerY>0f && eulerY<=60f){

                feature="facing slightly right up";

            }else {

                feature="Face Slightly tilted to right";

            }

        }else if(eulerZ>45f){

            if(eulerY>60f && eulerY!=0){

                feature="Facing right up";

            }else{

                feature="Face tilted to right";

            }

        }else if(eulerZ<0f && eulerZ >-5f){

            if(eulerY>-60f && eulerY!=0){

                feature="Facing right";

            }else{

                feature="no tilt";

            }

        }else if(eulerZ<-5f && eulerZ>-45f){

            if(eulerY>-60f && eulerY!=0){

                feature="Facing Left up";

            }else{

                feature="Face Slightly tilted to left";

            }

        }else{

            if(eulerY>-6f && eulerY!=0){

                feature="Facing Left up";

            }else{

                feature="Face tilted to left";

            }

        }

        return feature;

    }
    private String getUpdates(){

        final double SMILING_PROB_THRESHOLD = .15;

        final double EYE_OPEN_PROB_THRESHOLD = .5;


        String update;

        boolean smiling = mFace.getIsSmilingProbability() > SMILING_PROB_THRESHOLD;

        boolean leftEyeClosed = mFace.getIsLeftEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;

        boolean rightEyeClosed = mFace.getIsRightEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;

        if(smiling) {

            if (leftEyeClosed && !rightEyeClosed) {

                update="Left Wink";

            } else if(rightEyeClosed && !leftEyeClosed){

                update = "Right WInk";

            } else if (leftEyeClosed){

                update = "Closed Eye Smile";

            } else {

                update = "Smile";

            }

        } else {

            if (leftEyeClosed && !rightEyeClosed) {

                update = "Left Wink Frawn";

            } else if(rightEyeClosed && !leftEyeClosed){

                update = "Right Wink Frawn";

            } else if (leftEyeClosed){

                update = "Closed Eye Frawn";

            } else {

                update = "Frawn";

            }

        }

        return update;

    }

}
