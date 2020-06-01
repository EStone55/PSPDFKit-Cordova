package com.pspdfkit.cordova.event;

import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.ui.special_mode.manager.AnnotationManager.OnAnnotationSelectedListener;
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController;
import com.pspdfkit.cordova.event.EventDispatcher;
import org.json.JSONObject;
import org.json.JSONException;
import android.util.Log;

public class AnnotationSelectedListener implements OnAnnotationSelectedListener {

    public static final String LOG_TAG = "AnnotationSelectedListener";


    public AnnotationSelectedListener() {

    }

    public void onAnnotationSelected(Annotation annotation, boolean annotationCreated) {
        Log.i(LOG_TAG, annotation);
        try {
            EventDispatcher.getInstance().sendEvent("onAnnotationSelected", new JSONObject(annotation.toInstantJson()));
        } catch(JSONException ex) {
            // squash
        }
    }

    public boolean onPrepareAnnotationSelection(AnnotationSelectionController controller, Annotation annotation,
            boolean annotationCreated) {
        return true;
    }

}