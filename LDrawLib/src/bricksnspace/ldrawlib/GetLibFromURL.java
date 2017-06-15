/*
	Copyright 2013-2017 Mario Pascucci <mpascucci@gmail.com>
	This file is part of LDrawLib

	LDrawLib is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	LDrawLib is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with LDrawLib.  If not, see <http://www.gnu.org/licenses/>.

*/


package bricksnspace.ldrawlib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.SwingWorker;



public class GetLibFromURL extends SwingWorker<Integer, Void> {

	private URL updateUrl;
	private String dest;
	
	
	
	GetLibFromURL(URL url, String dest) {
		
		this.updateUrl = url;
		this.dest = dest;
	}
	
	
	@Override
	protected Integer doInBackground() throws IOException {

		// getting file
		File tempFile = new File(dest+".tmp");
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection connect = (HttpURLConnection) updateUrl.openConnection();
		int res = connect.getResponseCode();
		int tries = 0;
		while (res>=300 && res<400) {
			// it is a redirect
			updateUrl = new URL(connect.getHeaderField("Location"));
			//System.out.println(updateUrl);
			// get new connection
			connect = (HttpURLConnection) updateUrl.openConnection();
			res = connect.getResponseCode();
			tries++;
			if (tries > 4) {
				throw new IOException("Too many redirect, aborted");
			}
		}
		int fileLen = connect.getContentLength();
		//System.out.println(fileLen);
		byte[] buffer = new byte[4096];
		if (fileLen != 0) {
			FileOutputStream temp = new FileOutputStream(tempFile);
			InputStream remoteFile = connect.getInputStream();
			int r;
			int total = 0;
			while ((r = remoteFile.read(buffer)) > 0) {
				temp.write(buffer, 0, r);
				total += r;
				if ((total % 50000) < 4095) {
					setProgress(total/(fileLen/100));
				}
			}
			temp.close();
			remoteFile.close();
		}
		LDLibrary l = new LDLibrary(tempFile.getPath(), true);
		if (!l.isLDrawStd()) {
			throw new IOException("File isn't a standard LDraw library, aborted.");
		}
		File f = new File(dest);
		f.delete();
		tempFile.renameTo(f);
		return fileLen;
	}

	
}
