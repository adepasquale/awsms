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

package com.googlecode.awsms.senders;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.Toast;

import com.googlecode.awsms.AndroidWebSMS;
import com.googlecode.awsms.R;

/**
 * Asynchronous task which sends an SMS while displaying a progress dialog.
 * 
 * @author Andrea De Pasquale
 */
public class SenderAsyncTask extends AsyncTask<Void, byte[], String> {
// TODO make this a background service with an outgoing message queue

	private AndroidWebSMS androidWebSMS;
	private ProgressDialog progressDialog;

	public SenderAsyncTask() {
		androidWebSMS = AndroidWebSMS.getApplication();
	}

	@Override
	protected void onPreExecute() {
		progressDialog = ProgressDialog.show(androidWebSMS.getComposeActivity(),
				androidWebSMS.getString(R.string.ProgressDialogTitle), 
				androidWebSMS.getString(R.string.ProgressDialogMessage), 
				true, true);
		// TODO what should we do when the dialog is canceled?
	}
	
	@Override
	protected String doInBackground(Void... params) {
		WebSender webSender = androidWebSMS.getWebSender();
		String username = androidWebSMS.getUsername();
		String password = androidWebSMS.getPassword();
		String receiver = androidWebSMS.getReceiver();
		String message = androidWebSMS.getMessage();
		String captcha = androidWebSMS.getCaptcha();

		switch (webSender.send(username, password, receiver, message, captcha)) {
		case SETTINGS_INVALID: 
			return androidWebSMS.getString(R.string.WebSenderSettingsInvalid);
		case RECEIVER_INVALID:
			return androidWebSMS.getString(R.string.WebSenderReceiverInvalid);
		case MESSAGE_INVALID:
			return androidWebSMS.getString(R.string.WebSenderMessageInvalid);
		case WEBSITE_UNAVAILABLE:
			return androidWebSMS.getString(R.string.WebSenderWebsiteUnavailable);
		case OUT_OF_MESSAGES:
			return androidWebSMS.getString(R.string.WebSenderOutOfMessages);
		case NEED_CAPTCHA:
			publishProgress(webSender.getCaptchaArray());
			return androidWebSMS.getString(R.string.WebSenderNeedCaptcha);
		case MESSAGE_SENT:
			publishProgress();
			return androidWebSMS.getString(R.string.WebSenderMessageSent);
		default:
			return androidWebSMS.getString(R.string.WebSenderUnknownError);
		}
	}
	
	@Override
    protected void onProgressUpdate(byte[]... progress) {
        if (progress.length > 0) {
			androidWebSMS.showCaptchaLayout(progress[0]);
        } else {
        	androidWebSMS.hideCaptchaLayout();
        }
    }
	
	@Override
	protected void onPostExecute(String result) {
		progressDialog.dismiss();
		Toast.makeText(androidWebSMS.getComposeActivity(), 
				result, Toast.LENGTH_LONG).show();
	}
	
}
