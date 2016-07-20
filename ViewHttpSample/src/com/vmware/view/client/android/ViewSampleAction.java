package com.vmware.view.client.android;


import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;

/*
 * this is a sample list action just for demo
 */

class ViewSampleAction extends ViewActionBase {

	public ViewActionInfo.ViewListInfoResponse list(String path, String by,
			String order) {
		ViewActionInfo.ViewListInfoResponse ret = new ViewActionInfo.ViewListInfoResponse();

		if (null != path && path.length() > 0) {
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair(ViewActionBase.Key_Method,
					Value_Method));
			params.add(new BasicNameValuePair(ViewActionBase.Key_Path, path));

			if (null != by && by.length() > 0) {
				params.add(new BasicNameValuePair(Key_By, by));
			}

			if (null != order && order.length() > 0) {
				params.add(new BasicNameValuePair(Key_Order, order));
			}

			// build url
			String url = ViewActionBase.ViewRequestUrl + "/" + Component_Name
					+ "?" + buildParams(params);
			
			//user init http post here
			HttpGet httpget = new HttpGet(url);
			//or a http get
			//HttpPost httpPost = new HttpPost(url);
			ViewActionBase.ViewRawHTTPResponse response = sendHttpRequest(httpget);

			if (null != response) {
				ret.status.message = response.message;

				if (null != response.response) {
					ret = parseListResponse(response.response);
				}
			}
		}

		return ret;
	}

	// value of method
	private final static String Value_Method = "list";

	// key : by
	private final static String Key_By = "by";

	// key : order
	private final static String Key_Order = "order";
}

