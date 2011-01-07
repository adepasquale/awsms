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

package com.googlecode.awsms.senders.tim;

import android.content.Context;

import com.googlecode.awsms.senders.WebSMS;
import com.googlecode.awsms.senders.WebSender;

public class TimWebSender extends WebSender {
    
    static final String TAG = "TimWebSender";

//    POST https://www.tim.it/authfe/login.do
//    login : 3398765432
//    password : blablabla
//    urlOk : https://www.tim.it/servizitim/mac/redirezionaservizi.do?id_Servizio=6994
//    urlKo : 
//    portale : timPortale
    
//    https://www.tim.it/timcaptcha/captcha.jpg 

    
    public TimWebSender(Context context) {
	super(context);
	// TODO 
    }

    @Override
    public void preSend() throws Exception {
	// TODO 	
    }
    
    @Override
    public boolean send(WebSMS sms) throws Exception {
	// TODO 
	return false;
    }

}
