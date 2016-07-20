package com.vmware.view.client.android;

import java.util.List;

public final class ViewActionInfo {

	/**
	 * 
	 */
	public static class SimplefiedResponse {

		/** 0 means success */
		public int errorCode = ViewHttpClientErrorCode.Error_DefaultError;

		/** fail message detail */
		public String message = null;
	}

	/**
	 * general class for a normal file
	 */
	public static class ViewFileInfo {

		/** file path */
		public String path = null;

		/** modified time in millisecond */
		public long mTime = 0L;

		/** created time in millisecond */
		public long cTime = 0L;

		/** file Md5 value */
		public String blockList = null;

		/** file size */
		public long size = -1;

		/** is directory or not */
		public boolean isDir = false;

		/** has sub directory or not */
		public boolean hasSubFolder = false;

		/** file unique id */
		public long fsId = 0L;
	}


	/**
	 * sample action response
	 */
	public static class ViewListInfoResponse {

		/** response status */
		public SimplefiedResponse status = new SimplefiedResponse();

		/** the list of sample file */
		public List<ViewFileInfo> list = null;
	}
	
	//TODO
	//--- user can add more response structure here ---
}
