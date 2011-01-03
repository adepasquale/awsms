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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.awsms.R;

/**
 * Asynchronous task which sends an SMS and displays notifications.
 * 
 * @author Andrea De Pasquale
 */
public class WebSenderAsyncTask extends AsyncTask<Void, Object, Void> {
    
    static final String TAG = "WebSenderAsyncTask";

    Context context;
    NotificationManager notificationManager;
    SharedPreferences sharedPreferences;
    LinkedBlockingQueue<SMS> smsQueue;
    LinkedBlockingQueue<String> captchaQueue;
    
    VodafoneWebSender vodafoneWebSender = null;
    Dialog captchaDialog;

    public synchronized WebSender getWebSender() {
        return vodafoneWebSender;
    }

    static final int PROGRESS_BEFORE_LOGIN = 1;
    static final int PROGRESS_AFTER_LOGIN = 2;
    static final int PROGRESS_BEFORE_SENDING = 3;
    static final int PROGRESS_AFTER_SENDING = 4;
    static final int PROGRESS_CAPTCHA_DIALOG = 5;
    static final int PROGRESS_SUCCESS = 6;
    static final int PROGRESS_ERROR = 7;
    
    public WebSenderAsyncTask(Context c) {
	context = c;
	notificationManager = (NotificationManager) context
		.getSystemService(Context.NOTIFICATION_SERVICE);
	sharedPreferences = 
	    PreferenceManager.getDefaultSharedPreferences(context);
	smsQueue = new LinkedBlockingQueue<SMS>(20);
	captchaQueue = new LinkedBlockingQueue<String>(5);
    }
    
    public void enqueue(SMS sms) {
	smsQueue.add(sms);
    }

    @Override
    protected Void doInBackground(Void... params) {
	if (sharedPreferences.getBoolean("NotifyStatus", true))
	    publishProgress(PROGRESS_BEFORE_LOGIN);
	
	vodafoneWebSender = new VodafoneWebSender(context);
	
	if (sharedPreferences.getBoolean("NotifyStatus", true))
	    publishProgress(PROGRESS_AFTER_LOGIN);
	
	while (true) {
	    SMS sms = null;
	    try { sms = smsQueue.take(); } 
	    catch (Exception e) { }
	    if (sms == null) continue;
	    
	    try {
		
		if (sharedPreferences.getBoolean("NotifyStatus", true))
		    publishProgress(PROGRESS_BEFORE_SENDING, smsQueue.size()+1);

		String captcha = "";
		while (!vodafoneWebSender.send(sms, captcha)) {
		    publishProgress(PROGRESS_CAPTCHA_DIALOG, 
			    vodafoneWebSender.getCaptchaArray());
		    captcha = captchaQueue.take();
		}
		
		if (sharedPreferences.getBoolean("NotifyStatus", true))
		    publishProgress(PROGRESS_AFTER_SENDING);
		
		if (sharedPreferences.getBoolean("NotifySuccess", false))
		    publishProgress(PROGRESS_SUCCESS);
		
		if (sharedPreferences.getBoolean("SaveSMS", true)) {
		    String[] receivers = sms.getReceivers();
		    String message = sms.getMessage();
		    for (String receiver : receivers) {
			ContentValues out = new ContentValues();
			out.put("address", receiver);
			out.put("body", message);
			context.getContentResolver()
				.insert(Uri.parse("content://sms/sent"), out);
		    }
		}
		
	    } catch (Exception e) {
		publishProgress(PROGRESS_ERROR, e.getMessage());
	    }
	}
    }

    @Override
    protected void onProgressUpdate(Object... progress) {
	if (progress.length == 0) return;
	
	switch ((Integer) progress[0]) {
	case PROGRESS_BEFORE_LOGIN:
	    Notification nbl = createNotification("Autenticazione in corso");
	    nbl.flags |= Notification.FLAG_ONGOING_EVENT;
	    nbl.flags |= Notification.FLAG_NO_CLEAR;
	    notificationManager.notify(0, nbl);
	    break;
	    
	case PROGRESS_AFTER_LOGIN:
	    notificationManager.cancel(0);
	    break;
	    
	case PROGRESS_BEFORE_SENDING:
	    Notification nbs = createNotification("Invio in corso");
	    nbs.flags |= Notification.FLAG_ONGOING_EVENT;
	    nbs.flags |= Notification.FLAG_NO_CLEAR;
	    int queueSize = (Integer) progress[1];
	    if (queueSize > 1) nbs.number = queueSize;
	    notificationManager.notify(0, nbs);
	    break;
	    
	case PROGRESS_CAPTCHA_DIALOG:
	    captchaDialog = new Dialog(context);
	    captchaDialog.setContentView(R.layout.captcha);
	    captchaDialog.setTitle(R.string.CaptchaDialogTitle);
	    
	    ImageView captchaImage = 
		(ImageView) captchaDialog.findViewById(R.id.CaptchaDialogImage);
	    TextView captchaText =
		(TextView) captchaDialog.findViewById(R.id.CaptchaDialogText);
	    Button captchaButton = 
		(Button) captchaDialog.findViewById(R.id.CaptchaDialogButton);
	    
	    byte[] captchaArray = vodafoneWebSender.getCaptchaArray();
	    Bitmap captchaBitmap = BitmapFactory.decodeByteArray(
		    captchaArray, 0, captchaArray.length);
	    captchaImage.setImageBitmap(captchaBitmap);
	    captchaText.requestFocus();
	    captchaButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            TextView captchaText = (TextView) 
	            	captchaDialog.findViewById(R.id.CaptchaDialogText);
	            String captcha = captchaText.getText().toString();
	            Log.i(TAG, captcha);
	            captchaQueue.add(captcha);
	            captchaDialog.dismiss();
	        }
	    });
	    
	    captchaDialog.setCancelable(false);
	    captchaDialog.show();
	    break;
	    
	case PROGRESS_AFTER_SENDING:
	    notificationManager.cancel(0);
	    break;
	    
	case PROGRESS_SUCCESS:
	    Notification ns = createNotification("Messaggio inviato");
	    notificationManager.notify(0, ns);
	    break;
	    
	case PROGRESS_ERROR:
	    Notification ne = createNotification("Errore durante l'invio");
	    ne.flags |= Notification.FLAG_NO_CLEAR;
	    ne.defaults |= Notification.DEFAULT_SOUND;
	    ne.defaults |= Notification.DEFAULT_VIBRATE;
	    notificationManager.notify(0, ne);
	    break;
	    
	default:
	    return;
	}
    }
    
    private Notification createNotification(String content) {
	Notification notification = new Notification(R.drawable.ic_notify_draft,
		content, System.currentTimeMillis());
	// FIXME resume ComposeActivity
	notification.setLatestEventInfo(context, 
		context.getString(R.string.ApplicationLabel), content, 
		PendingIntent.getActivity(context, 0, null, 0));
	return notification;
    }
}
