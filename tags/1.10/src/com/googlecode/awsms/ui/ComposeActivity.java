/*
 * Copyright 2010-2011 Andrea De Pasquale
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

import java.net.URLDecoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Annotation;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.awsms.R;
import com.googlecode.awsms.senders.WebSMS;
import com.googlecode.awsms.senders.WebSenderAsyncTask;
import com.googlecode.awsms.senders.WebSenderHelper;
import com.googlecode.awsms.senders.vodafone.VodafoneWebSenderHelper;

/**
 * Activity used to compose a message and send it by pressing a button. 
 * 
 * @author Andrea De Pasquale
 */
public class ComposeActivity extends Activity {

    static final String TAG = "ComposeActivity";
    
    WebSenderAsyncTask webSenderAsyncTask;
    SharedPreferences sharedPreferences;
    WebSenderHelper webSenderHelper;
    ConnectivityManager connectivityManager;
    
    ReceiverAdapter receiverAdapter;
    AutoCompleteTextView receiverText;

    EditText messageText;
    TextView messageLength;
    Button messageSend;

    static final int SETTINGS_DIALOG = 0;
    static final int COUNTER_DIALOG = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.compose);

	webSenderAsyncTask = new WebSenderAsyncTask(ComposeActivity.this);
	webSenderAsyncTask.execute();
	
	sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	webSenderHelper = new VodafoneWebSenderHelper(this);
	connectivityManager = (ConnectivityManager)
		getSystemService(Context.CONNECTIVITY_SERVICE);
	
	receiverText = (AutoCompleteTextView) findViewById(R.id.ReceiverText);
	messageText = (EditText) findViewById(R.id.MessageText);
	messageLength = (TextView) findViewById(R.id.MessageLength);
	messageSend = (Button) findViewById(R.id.MessageSend);

	receiverAdapter = new ReceiverAdapter(this);
	receiverText.setAdapter(receiverAdapter);
	
	messageText.addTextChangedListener(new TextWatcher() {
	    public void beforeTextChanged(
		    CharSequence s, int start, int count, int after) { }

	    public void afterTextChanged(Editable s) { }

	    public void onTextChanged(
		    CharSequence s, int start, int before, int count) {
		updateLength(s.toString().replaceAll("\\s+$", "")
			.replaceAll("\\s{2,}", " ").length());
	    }
	});

	updateLength(0);

	messageSend.setOnClickListener(new OnClickListener() {
	    public void onClick(View v) {
		// if for some reason it isn't running, start it again
		if (webSenderAsyncTask.getStatus() != Status.RUNNING) {
		    webSenderAsyncTask = 
			new WebSenderAsyncTask(ComposeActivity.this);
		    webSenderAsyncTask.execute();
		}
		
		// detect if there's no connection available
		if (connectivityManager.getActiveNetworkInfo() == null) {
		    Toast.makeText(ComposeActivity.this, 
	    		R.string.NetworkUnavailable, 
	    		Toast.LENGTH_SHORT).show();
		    return;
		}
		
    		try {
    		    // schedule the message for sending
    		    WebSMS sms = new WebSMS();
    		    sms.setReceiver(getReceiver());
    		    sms.setReceiverName(getReceiverName());
    		    sms.setMessage(getMessage());
    		    webSenderAsyncTask.send(sms);
    		    Toast.makeText(ComposeActivity.this,
    			R.string.SMSValid, Toast.LENGTH_SHORT).show();
    		    clearFields(false);
    		} catch (Exception e) {
    		    Toast.makeText(ComposeActivity.this, 
    			e.getMessage(), Toast.LENGTH_SHORT).show();
    		}
	    }
	});

	// display a warning if username or password are empty
	if (webSenderHelper.getUsername().equals("") ||
	    webSenderHelper.getPassword().equals("")) {
	    showDialog(SETTINGS_DIALOG);
	}

	// check if application was started with an intent
	Intent intent = getIntent();
	if (savedInstanceState == null && intent != null) {

	    if (intent.getAction().equals(Intent.ACTION_SEND)) {
		String message = intent.getStringExtra(Intent.EXTRA_TEXT);
		messageText.setText(message);
		receiverText.requestFocus();
	    }

	    if (intent.getAction().equals(Intent.ACTION_SENDTO)) {
		String receiver = URLDecoder.decode(intent.getDataString())
			.replaceAll("[^0-9\\+]*", "");
		Cursor cursor = 
		    receiverAdapter.runQueryOnBackgroundThread(receiver);
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
		    receiverText.setText(
			    receiverAdapter.convertToString(cursor));
		} else {
		    receiverText.setText(receiver);
		}
		messageText.requestFocus();
	    }
	}
    }

    @Override
    public void onBackPressed() {
	// prevent onDestroy() call
	moveTaskToBack(true);
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
	case R.id.ClearMenuItem:
	    clearFields(true);
	    return true;

	case R.id.CounterMenuItem:
	    showDialog(COUNTER_DIALOG);
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
	case SETTINGS_DIALOG:
	    AlertDialog.Builder settingsBuilder = new AlertDialog.Builder(this);
	    settingsBuilder.setTitle(R.string.SettingsDialogTitle);
	    settingsBuilder.setMessage(R.string.SettingsDialogMessage);
	    settingsBuilder.setPositiveButton(
		    R.string.SettingsDialogPositiveButton,
		    new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    startActivity(new Intent(ComposeActivity.this,
				    SettingsActivity.class));
			}
		    });
	    settingsBuilder.setNegativeButton(
		    R.string.SettingsDialogNegativeButton,
		    new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    removeDialog(SETTINGS_DIALOG);
			}
		    });
	    return settingsBuilder.create();

	case COUNTER_DIALOG:
	    AlertDialog.Builder counterBuilder = new AlertDialog.Builder(this);
	    counterBuilder.setTitle(R.string.CounterDialogTitle);
	    counterBuilder.setMessage("Vodafone: " + 
		    webSenderHelper.getCount() + " / " + 
		    webSenderHelper.getLimit());
	    counterBuilder.setPositiveButton(R.string.CounterDialogButton,
		    new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    removeDialog(COUNTER_DIALOG);
			}
		    });
	    return counterBuilder.create();
	    
	default:
	    return super.onCreateDialog(id);
	}
    }

    private void updateLength(int length) {
	messageLength.setText(webSenderHelper.calcRemaining(length) + 
		" / " + webSenderHelper.calcFragments(length));
    }
    
    private void clearFields(boolean always) {
	String preference = sharedPreferences.getString("ClearSMS", "M");
	if (always || preference.contains("R")) receiverText.setText("");
	if (always || preference.contains("M")) messageText.setText("");
    }
    
    private String getReceiver() throws Exception {
	Spannable r = receiverText.getText();
	String receiver = r.toString();
	
	Annotation[] annotations = r.getSpans(0, r.length(), Annotation.class);
	for (Annotation annotation : annotations) {
	    if (annotation.getKey().equals("receiver")) {
		receiver = annotation.getValue();
	    }
	}
	
	receiver = fixReceiver(receiver);
	if (receiver.length() < 9 || receiver.length() > 10) {
	    throw new Exception(getString(R.string.ReceiverInvalid));
	}
	
	return receiver;
    }
    
    private String getReceiverName() {
	Spannable n = receiverText.getText();
	String name = n.toString();
	
	Annotation[] annotations = n.getSpans(0, n.length(), Annotation.class);
	for (Annotation annotation : annotations) {
	    if (annotation.getKey().equals("receiverName")) {
		name = annotation.getValue();
	    }
	}
	
	return name;
    }
    
    private String fixReceiver(String receiver) {
	final String PREFIX = "39";
	String pZero = "00" + PREFIX;
	String pPlus = "+" + PREFIX;

	receiver = receiver.replaceAll("[^0-9\\+]*", "");
	int lNumber = receiver.length();
	int lZero = pZero.length();
	int lPlus = pPlus.length();

	if (lNumber > lZero && receiver.substring(0, lZero).equals(pZero)) {
	    return receiver.substring(lZero);
	}

	if (lNumber > lPlus && receiver.substring(0, lPlus).equals(pPlus)) {
	    return receiver.substring(lPlus);
	}

	return receiver;
    }

    private String getMessage() throws Exception {
	String message = messageText.getText().toString();
	message = message.replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ");
	
	if (webSenderHelper.calcFragments(message.length()) == 0) {
	    throw new Exception(getString(R.string.MessageInvalid));
	}
	
	return message;
    }
    
}
