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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.googlecode.awsms.AndroidWebSMS;
import com.googlecode.awsms.R;
import com.googlecode.awsms.db.ReceiverAdapter;
import com.googlecode.awsms.db.SmslogDatabase;
import com.googlecode.awsms.senders.WebSender;

/**
 * Activity used to compose a message and send it by pressing a button. 
 * If needed, also displays an additional view to manually decode a CAPTCHA.
 * 
 * @author Andrea De Pasquale
 */
public class ComposeActivity extends Activity {

	AndroidWebSMS androidWebSMS;
	
 	AutoCompleteTextView receiverText;
	EditText messageText;
	TextView messageCounter;
	Button messageSend;
	LinearLayout captchaLayout;
	ImageView captchaImage;
	EditText captchaText;
	
	static final int INFO_DIALOG = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		receiverText = (AutoCompleteTextView) findViewById(R.id.ReceiverText);
		messageText = (EditText) findViewById(R.id.MessageText);
		messageCounter = (TextView) findViewById(R.id.MessageCounter);
		messageSend = (Button) findViewById(R.id.MessageSend);
		captchaLayout = (LinearLayout) findViewById(R.id.Captcha);
		captchaImage = (ImageView) findViewById(R.id.CaptchaImage);
		captchaText = (EditText) findViewById(R.id.CaptchaText);
		
		androidWebSMS = AndroidWebSMS.getApplication();
		androidWebSMS.setComposeActivity(this);
		
 		receiverText.setAdapter(new ReceiverAdapter(this));

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
				messageText.setText(messageText.getText().toString()
						.replaceAll("\\s+$", ""));
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
	    case R.id.InfoMenuItem:
	    	showDialog(INFO_DIALOG);
	    	return true;
	    case R.id.ClearMenuItem:
            receiverText.setText("");
            messageText.setText("");
            captchaText.setText("");
    		captchaLayout.setVisibility(View.GONE);
	        return true;
	    case R.id.SettingsMenuItem:
            startActivity(new Intent(this, SettingsActivity.class));
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case INFO_DIALOG:
			WebSender webSender = androidWebSMS.getWebSender();
			SmslogDatabase smslogDatabase = androidWebSMS.getDatabase();
						
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.InfoDialogTitle);
			builder.setMessage(
					webSender.getName() + ": " +
					smslogDatabase.query(webSender.getName()) + " / " +
					webSender.getDailyLimit());
			builder.setNeutralButton(R.string.InfoDialogButton, 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						removeDialog(INFO_DIALOG);
					}
				});
			return builder.create();
			
		default:
			return super.onCreateDialog(id);
		}
	}

	public AutoCompleteTextView getReceiverText() {
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
