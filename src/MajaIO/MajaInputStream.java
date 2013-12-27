package MajaIO;
import java.io.*;
import org.apache.tools.bzip2.*;
import Maja.*;
import java.util.zip.*;

public class MajaInputStream extends InputStream
{
	File _currentFile;
	FileInputStream _uncompressedStream;
	InputStream _compressedStream;
	InputStream _s;

	public MajaInputStream(File f) throws java.io.FileNotFoundException
	{
		_uncompressedStream = new FileInputStream(f);
		_s = _uncompressedStream;
		_currentFile = f;
	}
	
	public MajaInputStream(InputStream compressedStream)
	{
		_compressedStream = compressedStream;
		_s = compressedStream;

		_currentFile = null;
		_uncompressedStream = null;
	}
	
	public void startBzip2()
	{
		_compressedStream = new CBZip2InputStream(_uncompressedStream);
		_s = _compressedStream;
	}
	
	public void startGzip()
	{
		_compressedStream = new ZipInputStream(_uncompressedStream);
		_s = _compressedStream;
	}
	
	public boolean isCompressed()
	{
		if(_compressedStream != null)
			return true;
		
		return false;
	}
	
	public void endCompression()
	{
		_compressedStream = null;
		_s = _uncompressedStream;
	}
	
	public int available() throws java.io.IOException
	{
		return _s.available();
	}

	public void close() throws java.io.IOException
	{
		if(_compressedStream != null)
			_compressedStream.close();
		else if(_uncompressedStream != null)
			_uncompressedStream.close();
		_compressedStream = null;
		_uncompressedStream = null;
		_s = null;
	}
	
	public void mark(int readlimit)
	{
		_s.mark(readlimit);
	}
	
	public boolean markSupported()
	{
		return _s.markSupported();
	}
	
	public int read() throws java.io.IOException
	{
		byte b[] = new byte[1];
		this.read(b);
		return b[0];
	}
	
	public int read(byte[] b) throws java.io.IOException
	{	
		return this.read(b, 0, b.length);
	}
	
	/**
	* Reads data from an input stream. Note that because some input streams do not read
	* everything in one go, this method will loop to ensure that either the end of the
	* stream is reached, or the requested amount is completely read.
	* 
	* @param	b				The array to store data in
	* @param	off				Offset in bytes
	* @param	len				Length in bytes to read
	* @return					Number of bytes read (or -1 if EOS reached)
	*/
	public int read(byte[] b, int off, int len) throws java.io.IOException
	{
		int leftToRead = len;
		int readSoFar = off;
		while(leftToRead > 0)
		{
			int rtn = _s.read(b, readSoFar, leftToRead);
			if((readSoFar-off) == 0 && rtn < 0)
				return rtn;
			
			leftToRead -= rtn;
			readSoFar += rtn;
		}
		return readSoFar - off;
			
		//return _s.read(b, off, len);
	}

	public byte readByte() throws java.io.IOException
	{
		byte b[] = new byte[1];
		_s.read(b, 0, 1);
		return b[0];
	}
	
	public int readBigInt() throws java.io.IOException
	{
		byte[] b = new byte[4];
		int i = 0;
		_s.read(b, 0, 4);
		
		i += (b[0] & 0x000000FF) << 24;
		i += (b[1] & 0x000000FF) << 16;
		i += (b[2] & 0x000000FF) << 8;
		i += (b[3] & 0x000000FF) << 0;
		return i;
	}
	
	public double readDouble() throws java.io.IOException
	{
		byte[] b = new byte[8];
		_s.read(b, 0, 8);
		
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(b);
		return bb.getDouble();
	}
	
	public String readRestAsString() throws java.io.IOException, java.io.UnsupportedEncodingException
	{
		String s = new String("");
		final int BUF_SIZE = 1024;
		byte[] b = new byte[BUF_SIZE];
		int bytes_read = 0;
		while((bytes_read = this.read(b, 0, BUF_SIZE)) > 0)
		{
			s += new String(b, 0, bytes_read, "US-ASCII");
		}
		
		return s;
	}
	
	public float readLittleFloat() throws java.io.IOException
	{
		byte[] b = new byte[4];
		_s.read(b, 0, 4);
		
		//Reverse bytes
		byte[] br = new byte[4];
		for(int i = 0; i < 4; i++)
		{
			br[i] = b[3-i];
		}
		
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(br);
		return bb.getFloat();
	}

	//Because the JVM use BIG_ENDIAN, and FS2 files are all LITTLE_ENDIAN,
	//we do not need to check java.nio.ByteOrder.nativeOrder()
	public int readLittleInt() throws java.io.IOException
	{
		int i = this.readBigInt();
		return Integer.reverseBytes(i);
	}
	
	public String readString(int len) throws java.io.IOException, java.io.UnsupportedEncodingException
	{
		if(len > -1)
		{
			//Read the string into memory
			byte[] b = new byte[len];
			this.read(b, 0, len);
			
			//Determine actual string size
			int num = len;
			for(int i = 0; i < len; i++)
			{
				if(b[i] == '\0')
				{
					num = i;
					break;
				}
			}
			
			//Return string
			return new String(b, 0, num, "US-ASCII");
		}
		else
		{
			return this.readRestAsString();
		}
	}
	
	/*
	public javax.vecmath.Vector3d readVector() throws java.io.IOException
	{
		return new javax.vecmath.Vector3d(this.readFloat(), this.readFloat(), this.readFloat());
	}
	*/
	
	public void reset() throws java.io.IOException
	{
		_s.reset();
	}
	
	public long skip(long n) throws java.io.IOException
	{
		if(n < 0)
		{
			MajaApp.displayError("Unsupported attempt to seek " + n + " bytes in file '" + _currentFile.getName() + "'");
			return 0;
		}

		long bytesSkipped = 0;
		while(bytesSkipped < n)
		{
			bytesSkipped += _s.skip(n - bytesSkipped);
		}
		return bytesSkipped;
		/*
		java.nio.channels.FileChannel fc = _uncompressedStream.getChannel();
		long startPosition = fc.position();
		fc.position(startPosition + n);
		
		return fc.position() - startPosition;*/
	}
	
	/*public long getCurrentPosition() throws java.io.IOException
	{
		return _uncompressedStream.getChannel().position();
	}
	
	public void setCurrentPosition(long n) throws java.io.IOException
	{
		_uncompressedStream.getChannel().position(n);
	}*/
}