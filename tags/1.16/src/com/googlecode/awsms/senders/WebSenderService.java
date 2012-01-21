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

import com.googlecode.awsms.R;
import com.googlecode.awsms.app.ComposeActivity;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Background service used to schedule the sending.
 * 
 * @author Andrea De Pasquale
 */
public class WebSenderService extends Service {

  static final String TAG = "WebSenderService";
  private final IBinder binder = new WebSenderServiceBinder();

  public class WebSenderServiceBinder extends Binder {
    public WebSenderService getService() {
      return WebSenderService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "onBind()");
    return binder;
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate()");
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
  }

  public void send(WebSMS sms) {
    new AsyncTask<WebSMS, Void, WebSMS>() {
      @Override
      protected void onPreExecute() {
        Notification notification = new Notification(R.drawable.ic_notify,
            "Invio in corso", System.currentTimeMillis());
        Intent intent = new Intent(WebSenderService.this, ComposeActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notification.setLatestEventInfo(WebSenderService.this,
            WebSenderService.this.getString(R.string.ApplicationLabel),
            "Invio in corso",
            PendingIntent.getActivity(WebSenderService.this, 0, intent, 0));
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        WebSenderService.this.startForeground(12345, notification);
      }

      @Override
      protected WebSMS doInBackground(WebSMS... params) {
        WebSMS sms = params[0];
        Log.d(TAG, "this.send()");
        Log.d(TAG, sms.getReceiverNumber());
        Log.d(TAG, sms.getMessage());
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        return null;
      }

      @Override
      protected void onPostExecute(WebSMS result) {
        WebSenderService.this.stopForeground(true);
      }
    }.execute(sms);
  }

  public void login() {
    // TODO pre-authenticate for a faster sending
  }
}
