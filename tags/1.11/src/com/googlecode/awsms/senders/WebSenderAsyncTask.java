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

package com.googlecode.awsms.senders;

import java.util.concurrent.LinkedBlockingQueue;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.googlecode.awsms.R;
import com.googlecode.awsms.senders.vodafone.VodafoneWebSender;
import com.googlecode.awsms.ui.ComposeActivity;

/**
 * Asynchronous task which sends an SMS and displays notifications.
 * 
 * @author Andrea De Pasquale
 */
// TODO better if this becomes a background bindable task
public class WebSenderAsyncTask extends AsyncTask<Void, Object, Void> {
    
    static final String TAG = "WebSenderAsyncTask";

    Context context;
    NotificationManager notificationManager;
    int notificationID;
    SharedPreferences preferences;
    LinkedBlockingQueue<WebSMS> smsQueue;
    VodafoneWebSender vodafoneWebSender;

    // XXX remove
    LinkedBlockingQueue<String> captchaQueue;
    Dialog captchaDialog;

    static final int PROGRESS_SENDING = 1;
    static final int PROGRESS_SENT = 2;
    static final int PROGRESS_CAPTCHA = 3;
    static final int PROGRESS_SUCCESS = 4;
    static final int PROGRESS_ERROR = 5;
    
    public WebSenderAsyncTask(Context c) {
	context = c;
	notificationManager = (NotificationManager) context
		.getSystemService(Context.NOTIFICATION_SERVICE);
	preferences = 
	    PreferenceManager.getDefaultSharedPreferences(context);
	smsQueue = new LinkedBlockingQueue<WebSMS>(20);
	captchaQueue = new LinkedBlockingQueue<String>(5);
	vodafoneWebSender = new VodafoneWebSender(context);
    }
    
    public void send(WebSMS sms) {
//	ContentValues outbox = new ContentValues();
//	outbox.put("address", sms.getReceiver());
//	outbox.put("body", sms.getMessage());
//	context.getContentResolver()
//		.insert(Uri.parse("content://sms/outbox"), outbox);

	smsQueue.add(sms);
    }
    
    // XXX remove
    public void submitCaptcha() {
	TextView captchaText = (TextView) 
        	captchaDialog.findViewById(R.id.CaptchaDialogText);
        String captcha = captchaText.getText().toString();
        captchaQueue.add(captcha);
	captchaDialog.dismiss();
    }

    @Override
    protected Void doInBackground(Void... params) {
	
	try {
	    vodafoneWebSender.preSend();
	} catch (Exception e) {
	    // don't care here
	}
	
	while (true) {
	    WebSMS sms = null;
	    try { sms = smsQueue.take(); } 
	    catch (Exception e) { }
	    if (sms == null) continue;
	    
	    try {
		
		if (preferences.getBoolean("NotifyStatus", true))
		    publishProgress(PROGRESS_SENDING, sms.getReceiverName());

		while (!vodafoneWebSender.send(sms)) {
		    publishProgress(PROGRESS_CAPTCHA, sms.getCaptchaArray());
		    sms.setCaptcha(captchaQueue.take());
		}
		
		if (preferences.getBoolean("NotifyStatus", true))
		    publishProgress(PROGRESS_SENT);
		
		if (preferences.getBoolean("NotifySuccess", true))
		    publishProgress(PROGRESS_SUCCESS, sms.getReceiverName());
		
		if (preferences.getBoolean("SaveSMS", true)) {
		    ContentValues sent = new ContentValues();
		    sent.put("address", sms.getReceiverNumber());
		    sent.put("body", sms.getMessage());
		    context.getContentResolver()
			.insert(Uri.parse("content://sms/sent"), sent);
		}
		
	    } catch (Exception e) {
		publishProgress(PROGRESS_ERROR, e.getMessage());
		ContentValues failed = new ContentValues();
		failed.put("address", sms.getReceiverNumber());
		failed.put("body", sms.getMessage());
		context.getContentResolver()
			.insert(Uri.parse("content://sms/failed"), failed);
	    }
	}
    }

    @Override
    protected void onProgressUpdate(Object... progress) {
	if (progress.length == 0) return;
	
	switch ((Integer) progress[0]) {
	case PROGRESS_SENDING:
	    Notification nSending = createNotification(
		    "Invio del messaggio per " + 
		    ((String) progress[1]) + " in corso");
	    nSending.flags |= Notification.FLAG_ONGOING_EVENT;
	    nSending.flags |= Notification.FLAG_NO_CLEAR;
	    int queueSize = smsQueue.size() + 1;
	    if (queueSize > 1) nSending.number = queueSize;
	    notificationManager.notify(notificationID, nSending);
	    break;
	    
	case PROGRESS_CAPTCHA:
	    Notification nCaptcha = createNotification(
		    context.getString(R.string.CaptchaNotification));
	    nCaptcha.flags |= Notification.FLAG_AUTO_CANCEL;
	    nCaptcha.defaults |= Notification.DEFAULT_SOUND;
	    nCaptcha.defaults |= Notification.DEFAULT_VIBRATE;
	    notificationManager.notify(notificationID, nCaptcha);

	    captchaDialog = new Dialog(context);
	    captchaDialog.setContentView(R.layout.captcha);
	    captchaDialog.setTitle(R.string.CaptchaDialogTitle);
	    
	    ImageView captchaImage = 
		(ImageView) captchaDialog.findViewById(R.id.CaptchaDialogImage);
	    byte[] captchaArray = (byte[]) progress[1];
	    Bitmap captchaBitmap = BitmapFactory.decodeByteArray(
		    captchaArray, 0, captchaArray.length);
	    captchaImage.setImageBitmap(captchaBitmap);
	    
	    TextView captchaText =
		(TextView) captchaDialog.findViewById(R.id.CaptchaDialogText);
	    captchaText.setOnEditorActionListener(new OnEditorActionListener() {
		public boolean onEditorAction(
			TextView v, int actionId, KeyEvent event) {
		    if (event != null) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			    submitCaptcha();
	    		}
		    }
		    return false;
		}
	    });
	    captchaText.requestFocus();
	    
	    Button captchaButton = 
		(Button) captchaDialog.findViewById(R.id.CaptchaDialogButton);	    
	    captchaButton.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	            submitCaptcha();
	        }
	    });
	    
	    captchaDialog.setCancelable(true);
	    captchaDialog.setOnCancelListener(new OnCancelListener() {
		public void onCancel(DialogInterface dialog) {
		    submitCaptcha();
		}
	    });
	    
	    captchaDialog.show();
	    break;
	    
	case PROGRESS_SENT:
	    notificationManager.cancel(notificationID);
	    break;
	    
	case PROGRESS_SUCCESS:
	    Notification nSuccess = createNotification(
		    "Messaggio per " + ((String) progress[1])  + " inviato");
	    nSuccess.flags |= Notification.FLAG_AUTO_CANCEL;
	    // TODO notification opens receiver SMS thread 
	    notificationManager.notify(notificationID++, nSuccess);
	    break;
	    
	case PROGRESS_ERROR:
	    Notification nError = createNotification((String) progress[1]);
	    nError.flags |= Notification.FLAG_AUTO_CANCEL;
	    nError.defaults |= Notification.DEFAULT_SOUND;
	    nError.defaults |= Notification.DEFAULT_VIBRATE;
	    // TODO notification opens ComposeActivity
	    notificationManager.notify(notificationID++, nError);
	    break;
	    
	default:
	    return;
	}
    }
    
    private Notification createNotification(String title) {
	Notification notification = new Notification(R.drawable.ic_notify,
		title, System.currentTimeMillis());
	Intent intent = new Intent(context, ComposeActivity.class);
	intent.setAction(Intent.ACTION_MAIN);
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
		Intent.FLAG_ACTIVITY_SINGLE_TOP |
		Intent.FLAG_ACTIVITY_CLEAR_TOP);
	notification.setLatestEventInfo(context, 
		context.getString(R.string.ApplicationLabel), title, 
		PendingIntent.getActivity(context, 0, intent, 0));
	return notification;
    }
}
