package MajaIO;
import Maja.*;
import java.io.*;
import org.apache.tools.bzip2.*;

public class MajaOutputStream extends OutputStream
{
	FileOutputStream _uncompressedStream;
	CBZip2OutputStream _bzip2Stream;
	OutputStream _compressedStream;

	OutputStream _s;		//Current stream to use

	public MajaOutputStream(File f) throws FileNotFoundException
	{
		_uncompressedStream = new FileOutputStream(f);
		_s = _uncompressedStream;
	}

	MajaOutputStream(OutputStream os)
	{
		_uncompressedStream = null;
		_bzip2Stream = null;
		_s = _compressedStream = os;
	}
	
	public void startBzip2(int level) throws java.io.IOException
	{
		_bzip2Stream = new CBZip2OutputStream(_uncompressedStream, level);
		_s = _bzip2Stream;
	}
	
	public void endCompression() throws java.io.IOException
	{
		//Not sure about these.
		//_cos.flush();
		if(_s == _bzip2Stream)
		{
			_bzip2Stream.flush();
			_bzip2Stream = null;
			_s = _uncompressedStream;
		}
		else if(_s == _compressedStream)
		{
			this.close();
		}
	}
	
	public void close() throws java.io.IOException
	{
		if(_s != null)
			_s.close();
		_compressedStream = null;
		_uncompressedStream = null;
		_bzip2Stream = null;
		_s = null;
	}

	public void write(byte[] b) throws java.io.IOException
	{
		_s.write(b);
	}

	public void write(byte[] b, int off, int len) throws java.io.IOException
	{
		_s.write(b, off, len);
	}
	
	public void write(int i) throws java.io.IOException
	{
		_s.write((byte)i);
	}
	
	public void writeLittleDouble(double d) throws java.io.IOException
	{
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(8);
		bb.putDouble(d);
		byte[] b = bb.array();
		
		//Reverse for little endian
		byte[] br = new byte[8];
		for(int i = 0; i < 8; i++)
			br[i] = b[7-i];
		
		_s.write(br, 0, br.length);
	}
	
	public void writeLittleFloat(float f) throws java.io.IOException
	{
		java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(4);
		bb.putFloat(f);
		byte[] b = bb.array();
		
		//Reverse for little endian
		byte[] br = new byte[4];
		for(int i = 0; i < 4; i++)
			br[i] = b[3-i];
		
		_s.write(br, 0, br.length);
	}

	//Because the JVM use BIG_ENDIAN, and FS2 files are all LITTLE_ENDIAN,
	//we do not need to check java.nio.ByteOrder.nativeOrder()
	public void writeLittleInt(int v) throws java.io.IOException
	{
		int i = Integer.reverseBytes(v);
		byte[] b = new byte[4];
		b[0] = (byte)((i & 0xff000000) >> 24);
		b[1] = (byte)((i & 0xff0000) >> 16);
		b[2] = (byte)((i & 0xff00)   >> 8);
		b[3] = (byte)(i & 0xff);
		_s.write(b, 0, b.length);
	}
	
	public void writeLittleLong(long v)
	{
		MajaApp.displayError("Couldn't write long - no functionality.");
	}

	public void writeString(String v, int len) throws java.io.IOException, java.io.UnsupportedEncodingException
	{
		byte[] b = v.getBytes("US-ASCII");
		if(b.length <= len)
		{
			_s.write(b);
			for(int i = b.length; i < len; i++)
				_s.write('\0');
		}
		else
		{
			_s.write(b, 0, len);
		}
	}
	
	public long getCurrentPosition() throws java.io.IOException
	{
		if(_uncompressedStream != null)
			return _uncompressedStream.getChannel().position();
		else
			return -1;
	}
	
	public long setCurrentPosition(long newPosition) throws java.io.IOException
	{
		java.nio.channels.FileChannel fc = _uncompressedStream.getChannel();
		fc.position(newPosition);
		return fc.position();
	}
}