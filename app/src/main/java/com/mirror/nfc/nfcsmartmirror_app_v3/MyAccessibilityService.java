package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

//import static android.support.v7.appcompat.R.id.info;

/**
 * Created by fwille on 16.06.17.
 */

public class MyAccessibilityService extends AccessibilityService {


    AccessibilityServiceInfo info = new AccessibilityServiceInfo();


    @Override
    public void onInterrupt() {

    }

    @Override
    public void onServiceConnected() {
        // Set the type of events that this service wants to listen to.  Others
        // won't be passed to this service.
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED;

        // If you only want this service to work with specific applications, set their
        // package names here.  Otherwise, when the service is activated, it will listen
        // to events from all applications.
        info.packageNames = new String[]
                {"com.example.android.myFirstApp", "com.example.android.mySecondApp"};

        // Set the type of feedback your service will provide.
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;

        // Default services are invoked only if no package-specific ones are present
        // for the type of AccessibilityEvent generated.  This service *is*
        // application-specific, so the flag isn't necessary.  If this was a
        // general-purpose service, it would be worth considering setting the
        // DEFAULT flag.
    }
        @Override
        public void onAccessibilityEvent(AccessibilityEvent event) {
            final int eventType = event.getEventType();
            String eventText = null;
            switch(eventType) {
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    eventText = "Focused: ";
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    eventText = "Focused: ";
                    break;
            }

            eventText = eventText + event.getContentDescription();

            // Do something nifty with this text, like speak the composed string
            // back to the user.
            //speakToUser(eventText);

        }



    }
