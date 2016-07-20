package com.vmware.view.client.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import android.os.Environment;


class ViewUtils {

	private static char hexChar[] = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * md5String - get md5 string 
	 */
	public static String md5String(String content) {
		String md5 = "";
		MessageDigest messagedigest = null;
		try {
			messagedigest = MessageDigest.getInstance("MD5");
			messagedigest.update(content.getBytes());
			md5 = byty2HexString(messagedigest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return md5;
	}

	/**
	 * byty2HexString - byte to hex string
	 */
	public static String byty2HexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			sb.append(hexChar[(bytes[i] & 0xf0) >>> 4]);
			sb.append(hexChar[bytes[i] & 0x0f]);
		}
		return sb.toString();
	}
	
	public static String getFileName(String filePath) {
		return filePath.substring(filePath.lastIndexOf('/') + 1);
	}

	public static String getFileDirectory(String filePath) {
		int lastIndex = filePath.lastIndexOf('/');
		return lastIndex != -1 ? filePath.substring(0, lastIndex + 1) : "";
	}

	public static boolean loadFileSuccess(String filePath) {
		if (filePath == null || filePath.equals("")) {
			
			return false;
		}
		String sd_path = Environment.getExternalStorageDirectory()
				+ File.separator;
		if (filePath.contains(sd_path) && !isSDCardReady()) {
			
			return false;
		} else {
			return true;
		}
	}

	public static boolean isSDCardReady() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * unZipToCurrentPath
	 */
	public static void unZipToCurrentPath(String unZipfileName,
			ViewActionInfo.SimplefiedResponse ret) {
		File fileForUnzip = new File(unZipfileName);
		if (fileForUnzip.exists() && fileForUnzip.isFile()) {
			unZip(unZipfileName,
					getFileDirectory(fileForUnzip.getAbsolutePath()), ret);
		} else {
			ret.errorCode = ViewHttpClientErrorCode.Error_File_Not_Exist;
			ret.message = "get File info error with " + unZipfileName;
		}
	}

	/**
	 * unzip file zipfile 
	 */
	public static void unZip(String zipFileName, String outputDirectory,
			ViewActionInfo.SimplefiedResponse ret) {
		try {
			ZipFile zipFile = new ZipFile(zipFileName, "gbk");
			java.util.Enumeration e = zipFile.getEntries();

			ZipEntry zipEntry = null;

			while (e.hasMoreElements()) {
				zipEntry = (ZipEntry) e.nextElement();
				if (zipEntry.isDirectory()) {
					String name = zipEntry.getName();
					name = name.substring(0, name.length() - 1);
					mkDirs(outputDirectory + File.separator + name);
				} else {
					String name = zipEntry.getName();
					String dir = name.substring(0, name.lastIndexOf("/"));
					mkDirs(outputDirectory + File.separator + dir);
					File f = new File(outputDirectory + File.separator
							+ zipEntry.getName());
					f.createNewFile();
					InputStream in = zipFile.getInputStream(zipEntry);
					FileOutputStream out = new FileOutputStream(f);
					int c;
					byte[] by = new byte[1024];
					while ((c = in.read(by)) != -1) {
						out.write(by, 0, c);
					}
					out.close();
					in.close();
				}
			}
		} catch (Exception ex) {
			ret.message = ex.getMessage();
			ex.printStackTrace();
		}
	}

	/**
	 * create dirs 
	 */
	public static boolean mkDirs(String dir) throws Exception {
		if (dir == null || dir.equals("")) {
			return false;
		}
		File f1 = new File(dir);
		if (f1.exists()) {
			return true;
		}
		return f1.mkdirs();
	}
}
