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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Background service used to schedule the sending.
 *  
 * @author Andrea De Pasquale
 */
// FIXME use this bindable service to send
public class WebSenderService extends Service {

    static final String TAG = "WebSenderService";

    public class WebSenderServiceBinder extends Binder {
	public WebSenderService getService() {
	    return WebSenderService.this;
	}
    }

    @Override
    public IBinder onBind(Intent intent) {
	Log.i(TAG, "onBind()");
	return null;
    }

}
