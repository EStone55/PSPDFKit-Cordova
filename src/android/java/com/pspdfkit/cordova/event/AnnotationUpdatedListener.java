package com.pspdfkit.cordova.event;

import com.pspdfkit.annotations.AnnotationProvider.OnAnnotationUpdatedListener;

import java.util.List;

import com.pspdfkit.annotations.Annotation;
import org.json.JSONObject;
import org.json.JSONException;
import com.pspdfkit.cordova.event.EventDispatcher;

public class AnnotationUpdatedListener implements OnAnnotationUpdatedListener {

    public AnnotationUpdatedListener() {

    }

    @Override
    public void onAnnotationCreated(Annotation annotation) {

    }

    @Override
    public void onAnnotationRemoved(Annotation annotation) {
        try {
            JSONObject removedAnnotation = new JSONObject(annotation.toInstantJson());
            EventDispatcher.getInstance().sendEvent("onAnnotationRemoved", removedAnnotation);
        } catch (JSONException ex) {
            try {
                JSONObject error = new JSONObject();
                error.put("message", ex.getMessage());
                EventDispatcher.getInstance().sendEvent("onGenericEvent", error);
            } catch {
                // squash
            }
        }
    }

    @Override
    public void onAnnotationUpdated(Annotation annotation) {

    }

    @Override
    public void onAnnotationZOrderChanged(int pageIndex, List<Annotation> oldOrder, List<Annotation> newOrder) {
        
    }

}