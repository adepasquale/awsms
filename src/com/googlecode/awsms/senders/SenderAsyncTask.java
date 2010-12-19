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
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
		progressDialog = null;
	}

	@Override
	protected void onPreExecute() {
		progressDialog = ProgressDialog.show(androidWebSMS.getComposeActivity(),
				androidWebSMS.getString(R.string.ProgressDialogTitle), 
				androidWebSMS.getString(R.string.ProgressDialogMessage), 
				true, true);
		progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				SenderAsyncTask.this.cancel(true);
			}
		});
	}
	
	@Override
	protected String doInBackground(Void... params) {
		WebSender webSender = androidWebSMS.getWebSender();
		String username = androidWebSMS.getUsername();
		String password = androidWebSMS.getPassword();
		String receiver = androidWebSMS.getReceiver();
		String message = androidWebSMS.getMessage();
		String captcha = androidWebSMS.getCaptcha();

		try {
			webSender.send(username, password, receiver, message, captcha);
			publishProgress();
			return "message sent";
		} catch (Exception e) {
			// FIXME restore string messages
			if (e.getMessage().equals("need captcha"))
				publishProgress(webSender.getCaptchaArray());
			return e.getMessage();
		}
	}
	
	@Override
    protected void onProgressUpdate(byte[]... progress) {
        if (progress.length > 0) { // need CAPTCHA
			androidWebSMS.showCaptchaLayout(progress[0]);
        } else { // successfully sent message
        	androidWebSMS.hideCaptchaLayout();
        	androidWebSMS.saveWebSMS();
        	androidWebSMS.resetEditText();
        }
    }
	
	@Override
	protected void onPostExecute(String result) {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		Toast.makeText(androidWebSMS.getComposeActivity(), 
				result, Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onCancelled() {
		Toast.makeText(androidWebSMS.getComposeActivity(), 
				R.string.ProgressDialogCanceled, 
				Toast.LENGTH_LONG).show();
	}
	
}
