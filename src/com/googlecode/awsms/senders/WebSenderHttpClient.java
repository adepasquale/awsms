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

import java.io.InputStream;
import java.security.KeyStore;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import android.content.Context;

import com.googlecode.awsms.R;

public class WebSenderHttpClient extends DefaultHttpClient {

    final Context context;

    public WebSenderHttpClient(Context context) {
	this.context = context;
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
	SchemeRegistry registry = new SchemeRegistry();
	registry.register(new Scheme("http", PlainSocketFactory
		.getSocketFactory(), 80));
	// Register for port 443 our SSLSocketFactory 
	// with our keystore to the ConnectionManager
	registry.register(new Scheme("https", newSslSocketFactory(), 443));
	return new SingleClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory newSslSocketFactory() {
	try {
	    // Get an instance of the Bouncy Castle KeyStore format
	    KeyStore trusted = KeyStore.getInstance("BKS");
	    // Get the raw resource, which contains the keystore with
	    // your trusted certificates (root and any intermediate certs)
	    InputStream in = 
		context.getResources().openRawResource(R.raw.keystore);
	    
	    try {
		// Initialize the keystore with the provided trusted
		// certificates. Also provide the password of the keystore
		trusted.load(in, "storepass".toCharArray());
	    } finally {
		in.close();
	    }
	    
	    // Pass the keystore to the SSLSocketFactory. The factory is
	    // responsible for the verification of the server certificate.
	    SSLSocketFactory sf = new SSLSocketFactory(trusted);
	    // Hostname verification from certificate
	    // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
	    sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
	    return sf;
	    
	} catch (Exception e) {
	    throw new AssertionError(e);
	}
    }
}