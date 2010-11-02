/*
 * Copyright 2010 Andrea De Pasquale
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.awsms;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.view.View;

import com.googlecode.awsms.senders.SenderAsyncTask;
import com.googlecode.awsms.senders.VodafoneItalyWebSender;
import com.googlecode.awsms.senders.WebSender;
import com.googlecode.awsms.ui.ComposeActivity;

/**
 * Android Web SMS application controller. It manages UI activities and web 
 * senders tasks, also storing and retrieving their settings.
 * 
 * @author Andrea De Pasquale
 */
public class AndroidWebSMS extends Application {

	private static AndroidWebSMS androidWebSMS;
	private SharedPreferences sharedPreferences;
	private ComposeActivity composeActivity;
	private WebSender webSender;

    @Override
    public void onCreate() {
        super.onCreate();
        androidWebSMS = this;
        
        sharedPreferences = 
        	PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        webSender = new VodafoneItalyWebSender();
    }

    public static AndroidWebSMS getApplication() {
        return androidWebSMS;
    }
    
    public WebSender getWebSender() {
    	return webSender;
    }
    
    public void sendWebSMS() {
    	new SenderAsyncTask().execute();
    }
    
	public ComposeActivity getComposeActivity() {
		return composeActivity;
	}

	public void setComposeActivity(ComposeActivity composeActivity) {
		this.composeActivity = composeActivity;
	}
	
	public void updateMessageCounter(int messageTextLength) {
		int[] info = webSender.getInformation(messageTextLength);
		composeActivity.getMessageSend().setEnabled(info[0] != 0);
		composeActivity.getMessageCounter().setText(
				Integer.toString(info[1]) +	" / " + Integer.toString(info[0]));
	}
	
	public String getUsername() {
		return sharedPreferences.getString("VodafoneItalyUsername", "");
	}
	
	public String getPassword() {
		return sharedPreferences.getString("VodafoneItalyPassword", "");
	}
	
	public String getReceiver() {
		return composeActivity.getReceiverText().getText().toString();
	}
	
	public String getMessage() {
		return composeActivity.getMessageText().getText().toString();
	}

	public String getCaptcha() {
		return composeActivity.getCaptchaText().getText().toString();
	}

	public void showCaptchaLayout(Bitmap captchaImage) {
		composeActivity.getCaptchaImage().setImageBitmap(captchaImage);
		composeActivity.getCaptchaLayout().setVisibility(View.VISIBLE);
	}
	
	public void hideCaptchaLayout() {
		composeActivity.getCaptchaLayout().setVisibility(View.GONE);
		composeActivity.getCaptchaText().setText("");
	}

}
