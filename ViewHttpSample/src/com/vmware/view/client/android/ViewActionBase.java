package com.vmware.view.client.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


class ViewActionBase {
	//
	// set the access token
	//
	public void setAccessToken(String token) {
		mbAccessToken = token;
	}

	//
	// get the access token
	//
	public String getAccessToken() {
		return mbAccessToken;
	}

	//
	// send HTTP Request
	//
	protected ViewRawHTTPResponse sendHttpRequest(HttpRequestBase request) {
		// response
		ViewRawHTTPResponse ret = new ViewRawHTTPResponse();

		if (null != request) {

			// create client
			HttpClient client = ViewHttpClientFactory.makeHttpClient();
			HttpClientParams.setCookiePolicy(client.getParams(),
					CookiePolicy.BROWSER_COMPATIBILITY);

			if (null != client) {

				for (int retries = 0; ret.response == null
						&& retries < Max_Retries; ++retries) {
					try {
						ret.response = client.execute(request);
					} catch (NullPointerException e) {
						ret.message = e.getMessage();
					} catch (ClientProtocolException e) {
						ret.message = e.getMessage();
					} catch (IOException e) {
						ret.message = e.getMessage();
					}

					if (null == ret.response) {
						try {
							Thread.sleep(1000 * (retries + 1));
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							ret.message = e.getMessage();
						}
					}
				}
			}
		}

		return ret;
	}

	//
	// build url
	//
	protected String buildParams(List<NameValuePair> urlParams) {
		String ret = null;

		if (null != urlParams && urlParams.size() > 0) {
			try {
				HttpEntity paramsEntity = new UrlEncodedFormEntity(urlParams, "utf8");
				ret = EntityUtils.toString(paramsEntity);
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return ret;
	}

	/*
	 * parse clouddownload task info
	 */
	protected ViewActionInfo.ViewFileInfo parseCommonFileInfoByJSONObject(
			JSONObject jo) {
		ViewActionInfo.ViewFileInfo ret = new ViewActionInfo.ViewFileInfo();

		if (null != jo) {

			try {
				if (jo.has(Key_MD5)) {
					ret.blockList = jo.getString(Key_MD5);
				}

				if (jo.has(Key_BlockList)) {
					ret.blockList = jo.getString(Key_BlockList);
				}

				if (jo.has(Key_Path)) {
					ret.path = jo.getString(Key_Path);
				}

				if (jo.has(Key_Size)) {
					ret.size = jo.getLong(Key_Size);
				}

				if (jo.has(Key_CTime)) {
					ret.cTime = jo.getLong(Key_CTime);
				}

				if (jo.has(Key_MTime)) {
					ret.mTime = jo.getLong(Key_MTime);
				}

				if (jo.has(Key_IsDir)) {
					int isdir = jo.getInt(Key_IsDir);
					ret.isDir = (0 == isdir ? false : true);
				}

				if (jo.has(Key_HasSubFolder)) {
					int subFolder = jo.getInt(Key_HasSubFolder);
					ret.hasSubFolder = (0 == subFolder ? false : true);
				}
				if (jo.has(Key_FsId)) {
					ret.fsId = jo.getInt(Key_FsId);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return ret;
	}

	/*
	 * parse list response
	 */
	protected ViewActionInfo.ViewListInfoResponse parseListResponse(
			HttpResponse response) {
		ViewActionInfo.ViewListInfoResponse ret = new ViewActionInfo.ViewListInfoResponse();

		if (null != response) {
			try {
				HttpEntity resEntity = response.getEntity();
				String json = EntityUtils.toString(resEntity);

				JSONObject jo = new JSONObject(json);

				if (null != jo) {
					if (jo.has(Key_ErrorCode)) { // get error code
						ret.status.errorCode = jo.getInt(Key_ErrorCode);

						if (jo.has(Key_ErrorMessage)) {
							ret.status.message = jo.getString(Key_ErrorMessage);
						}
					} else {
						ret.status.errorCode = ViewHttpClientErrorCode.No_Error;
						if (jo.has(Key_Files_List)) {
							JSONArray list = jo.getJSONArray(Key_Files_List);
							ret.list = new ArrayList<ViewActionInfo.ViewFileInfo>();

							for (int i = 0; i < list.length(); ++i) {
								ViewActionInfo.ViewFileInfo info = parseCommonFileInfoByJSONObject(list
										.getJSONObject(i));
								ret.list.add(info);
							}
						}
					}
				}

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				ret.status.message = e.getMessage();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				ret.status.message = e.getMessage();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				ret.status.message = e.getMessage();
			}
		}

		return ret;
	}



	/*
	 * the data structure of quota, including the space info
	 */
	protected static class ViewRawHTTPResponse {
		// the response
		public HttpResponse response = null;

		// status message if failed
		public String message = null;
	}

	/*
	 * set maximum retry times
	 */
	static void setMaxRequestRetriesNumber(int maxRetries) {
		if (maxRetries <= 0) {
			return;
		}
		Max_Retries = maxRetries;
	}

	/*
	 * get maximun retry times
	 */
	static int getMaxRequestRetriesNumber() {
		return Max_Retries;
	}

	protected void closeInputStream(InputStream inputStream) {
		try {
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void closeRandomAccessFile(RandomAccessFile file) {
		try {
			if (file != null) {
				file.close();
				file = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	// the access token which is used to access rest API
	private String mbAccessToken = null;
	
	// the request url
	final static String ViewRequestUrl = "https://127.0.0.1/rest/2.0/";

	// the key of method
	final static String Key_Method = "method";

	// key of error code
	final static String Key_ErrorCode = "error_code";

	// key of error message
	final static String Key_ErrorMessage = "error_msg";

	// the key of path
	final static String Key_Path = "path";

	// key of param
	final static String Key_Param = "param";

	// the value of command
	final static String Component_Name = "view";

	// key of md5
	final static String Key_MD5 = "md5";

	// key block list
	final static String Key_BlockList = "block_list";

	// default max retries times
	final static int DEFAULT_MAX_RETRIES = 6;

	// max retries times
	static int Max_Retries = DEFAULT_MAX_RETRIES;

	/**
	 * below is the defines for the sample request
	 */
	// key : list
	final static String Key_Files_List = "list";

	// key of size
	private final static String Key_Size = "size";

	// number of subfolders
	private final static String Key_FilesNum = "filenum";

	// key of c time
	private final static String Key_CTime = "ctime";

	// key of mtime
	private final static String Key_MTime = "mtime";

	// key of fsid
	private final static String Key_FsId = "fsid";
	// key of isdir
	private final static String Key_IsDir = "isdir";

	// key of has sub folder
	private final static String Key_HasSubFolder = "hassubdir";
}

