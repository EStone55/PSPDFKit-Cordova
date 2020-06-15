package com.pspdfkit.cordova.event;

import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.ui.special_mode.manager.AnnotationManager.OnAnnotationSelectedListener;
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController;
import com.pspdfkit.cordova.event.EventDispatcher;
import org.json.JSONObject;
import org.json.JSONException;
import android.util.Log;

/**
 * ML specific code. Not in the orignal plugin
 * Description: EventListener for when an annotation is selected
 */
public class AnnotationSelectedListener implements OnAnnotationSelectedListener {

    public static final String LOG_TAG = "AnnotationSelectedListener";

    private JSONObject annotation = new JSONObject(); // keep track of a JSONObject of the annotation currently selected

    public AnnotationSelectedListener() {

    }

    @Override
    public void onAnnotationSelected(Annotation annotation, boolean annotationCreated) {
        try {
            this.annotation = new JSONObject(annotation.toInstantJson());
        } catch (JSONException ex) {
            // squash
        }
    }

    @Override
    public boolean onPrepareAnnotationSelection(AnnotationSelectionController controller, Annotation annotation,
            boolean annotationCreated) {
        return true;
    }

    public JSONObject getAnnotation() {
        return this.annotation;
    }
 
}