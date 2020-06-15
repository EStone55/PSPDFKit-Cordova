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
            JSONObject data = new JSONObject();
            data.put("annotationID", annotation.getName());
            EventDispatcher.getInstance().sendEvent("onGenericEvent", data);
        } catch (JSONException ex) {
            // squash
        }
    }

    @Override
    public void onAnnotationUpdated(Annotation annotation) {

    }

    @Override
    public void onAnnotationZOrderChanged(int pageIndex, List<Annotation> oldOrder, List<Annotation> newOrder) {
        
    }

}