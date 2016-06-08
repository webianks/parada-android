package br.com.mossteam.parada;

import android.app.Activity;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

import com.facebook.login.widget.LoginButton;

/**
 * Created by willian on 5/2/16.
 */
public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {

    Activity activity;
    Instrumentation instrumentation;
    LoginButton loginButton;

    public LoginActivityTest(Class activityClass) {
        super(activityClass);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        activity = getActivity();
        instrumentation = getInstrumentation();
        loginButton = (LoginButton) activity.findViewById(R.id.login_button);
    }

    public void testFacebookLogin() {
        assertTrue(loginButton.performClick());
    }
}