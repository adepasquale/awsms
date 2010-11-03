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

package com.googlecode.awsms.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.googlecode.awsms.AndroidWebSMS;
import com.googlecode.awsms.R;

/**
 * Activity used to compose a message and send it by pressing a button. 
 * If needed, also displays an additional view to manually decode a CAPTCHA.
 * 
 * @author Andrea De Pasquale
 */
public class ComposeActivity extends Activity {
	
	private AndroidWebSMS androidWebSMS;
	
	// TODO dynamically load contacts using MultiAutoCompleteTextView
// 	MultiAutoCompleteTextView receiverText;
	private EditText receiverText;
	private EditText messageText;
	private TextView messageCounter;
	private Button messageSend;
	private LinearLayout captchaLayout;
	private ImageView captchaImage;
	private EditText captchaText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// TODO try nextFocus* on res/main.xml
// 		receiverText = (MultiAutoCompleteTextView) findViewById(R.id.ReceiverText);
		receiverText = (EditText) findViewById(R.id.ReceiverText);
		messageText = (EditText) findViewById(R.id.MessageText);
		messageCounter = (TextView) findViewById(R.id.MessageCounter);
		messageSend = (Button) findViewById(R.id.MessageSend);
		captchaLayout = (LinearLayout) findViewById(R.id.Captcha);
		captchaImage = (ImageView) findViewById(R.id.CaptchaImage);
		captchaText = (EditText) findViewById(R.id.CaptchaText);
		
		androidWebSMS = AndroidWebSMS.getApplication();
		androidWebSMS.setComposeActivity(this);

// 		receiverText.setAdapter();
// 		receiverText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        messageText.addTextChangedListener(new TextWatcher() {
			public void beforeTextChanged(
					CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {}
        	public void onTextChanged(
        			CharSequence s, int start, int before, int count) {
        		androidWebSMS.updateMessageCounter(s.length());
			}
		});
		
        androidWebSMS.updateMessageCounter(0);
		
		messageSend.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				androidWebSMS.sendWebSMS();
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.layout.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.SettingsMenuItem:
            startActivity(new Intent(this, SettingsActivity.class));
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	public EditText getReceiverText() {
		return receiverText;
	}

	public EditText getMessageText() {
		return messageText;
	}

	public TextView getMessageCounter() {
		return messageCounter;
	}

	public Button getMessageSend() {
		return messageSend;
	}

	public LinearLayout getCaptchaLayout() {
		return captchaLayout;
	}

	public ImageView getCaptchaImage() {
		return captchaImage;
	}

	public EditText getCaptchaText() {
		return captchaText;
	}

}
