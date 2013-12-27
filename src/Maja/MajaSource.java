package Maja;
import java.util.*;
import java.io.*;
import MajaIO.*;

public class MajaSource extends MajaBranch<MajaSource>
{
	private MajaProject _project;

	private int _type;
	//private File _fileHandle;
	private MajaHandlerManager.MajaHandler _handler;

	private Vector<MajaEntry> _entries = new Vector<MajaEntry>();
	private MajaSourceEntry _headSourceEntry;
	//private Vector<MajaSource> _subSources = new Vector<MajaSource>();

	//*****Type defines
	public static final int INVALID = -1;
	public static final int FILE = 0;
	public static final int DIRECTORY = 1;
	//public static final int REMOTE_FILE = 2;
	//public static final int REMOTE_DIRECTORY = 3;
	public static final int PACKAGE = 4;
	
	public MajaSource(File f)
	{
		_headSourceEntry = new MajaSourceEntry(MajaEntry.INVALID);
		_headSourceEntry.clear();
		_headSourceEntry.setSource(this);
		
		//Set _type
		if(f != null && f.isDirectory())
		{
			_type = MajaSource.DIRECTORY;
			_headSourceEntry.setName(f.getName());
			_headSourceEntry.setType(MajaEntry.FOLDER);
			_headSourceEntry.setLastModified(f.lastModified());
			_headSourceEntry.setFileHandle(f);
		}
		else if(f != null && f.isFile())
		{
			_handler = MajaHandlerManager.findPackageHandler(f);
			if(_handler != null)
			{
				_type = MajaSource.PACKAGE;
				_headSourceEntry.setName(f.getName());
				_headSourceEntry.setLastModified(f.lastModified());
				_headSourceEntry.setFileHandle(f);
			}
			else
			{
				_type = MajaSource.FILE;
				_headSourceEntry.setName(f.getName());
				_headSourceEntry.setType(MajaEntry.FILE);
				_headSourceEntry.setLastModified(f.lastModified());
				_headSourceEntry.setPosition(0);
				_headSourceEntry.setUncompressedSize(f.length());
				_headSourceEntry.setFileHandle(f);
			}
		}
		else
		{
			_type = MajaSource.INVALID;
		}
		
		//Set path
		_headSourceEntry.setFileHandle(f);
	}
	
	public boolean addDependantEntry(MajaEntry n_entry)
	{
		if(n_entry == null)
			return false;
		
		if(n_entry.getProject() != _project)
			return false;
		
		if(_entries.contains(n_entry))
			return true;
		
		_entries.add(n_entry);
		return true;
	}
	
	public MajaSourceEntry addSourceEntry(MajaSourceEntry n_entry)
	{
		n_entry.setSource(this);
		return _headSourceEntry.addChild(n_entry);
	}
	
	/*
	public boolean addSubsource(MajaSource n_source)
	{
		if(n_source == null)
			return false;
			
		if(n_source._project != _project)
			return false;
		
		if(_subSources.contains(n_source))
			return true;

		_subSources.add(n_source);
		n_source._parent = this;
		return true;
	}*/
	
	public MajaSourceEntry exportSourceEntry(MajaOutputStream mos, MajaSourceEntry mse, long maxToWrite) throws java.io.IOException
	{
		if(mse.getSource() == null)
			return null;
		else if(mse.getSource() != this)
			return mse.getSource().exportSourceEntry(mos, mse, maxToWrite);
		
		if(mse.getStatus() != MajaEntry.SYNCED)
			return null;

		MajaInputStream mis = mse.openInputStream();
		if(mis == null)
			return null;
		/*catch(java.io.FileNotFoundException e)
		{
			MajaApp.displayException(e, "Could not open file '" + this.getPath() + "' for export.");
			if(mis!= null) try{mis.close();}catch(java.io.IOException e2){}
			return null;
		}*/

		byte[] buffer = new byte[1024000];
		long bytesRead = 0;
		long bytesWritten = 0;
		long bytesLeft = mse.getSize();
		long startPosition = mse.getPosition();
		long exportedPosition = mos.getCurrentPosition();

		//Do not copy empty files
		if(bytesLeft < 1)
			return null;
		
		//Entry is too big	
		if(maxToWrite < bytesLeft)
			return null;

		do
		{
			//READ
			try
			{
				if(bytesLeft > buffer.length)
					bytesRead = mis.read(buffer);
				else if(bytesLeft > 0)
				{
					bytesRead = mis.read(buffer, 0, (int)bytesLeft);
				}
				else
				{
					break;
				}
			}
			catch(java.io.IOException e)
			{
				if(bytesWritten < 1)
				{
					if(mis!= null) try{mis.close();}catch(java.io.IOException e2){}
					return null;
				}
				else
				{
					//Incomplete write, so pass it up the chain.
					throw e;
				}
			}
			
			//WRITE
			if(bytesRead > -1)
			{
				mos.write(buffer, 0, (int)bytesRead);
				bytesWritten += bytesRead;
				bytesLeft -= bytesRead;
			}
			else
			{
				break;
			}
		}
		while(true);

		mse.closeInputStream(mis);
		
		//Return our new finalEntry
		MajaSourceEntry rse = new MajaSourceEntry(mse.getType());
		rse.setCompression(MajaEntry.UNCOMPRESSED);
		rse.setName(mse.getName());
		rse.setPosition(exportedPosition);
		rse.setUncompressedSize(bytesWritten);
		rse.setLastModified(System.currentTimeMillis());
		//MajaEntry returnEntry = new MajaEntry(_project, me.getName(), me.getType(), null, null, MajaEntry.UNCOMPRESSED, exportedPosition, bytesWritten, System.currentTimeMillis());
		//returnEntry.setUncompressedSize(bytesWritten);
		return rse;
	}
	
	public MajaSource findDependantEntry(MajaEntry e)
	{
		if(_entries.contains(e))
			return this;
		
		for(int i = 0; i < this.getNumChildren(); i++)
		{
			MajaSource retVal = this.getChild(i).findDependantEntry(e);
			if(retVal != null)
				return retVal;
		}
		
		return null;
	}
	
	public java.io.File getFileHandle()
	{
		return _headSourceEntry.getFileHandle();
	}
	
	public MajaHandlerManager.MajaHandler getHandler()
	{
		return _handler;
	}
	
	/*
	public MajaInputStream getInputStream(MajaSourceEntry mse) throws FileNotFoundException, IOException
	{
		if(mse.getPosition() > this.getSize())
		{
			//MajaApp.displayError("Could not get input stream for because start position (" + startPosition + ") is larger than '" + this.getPath() + "' (" + this.getCompressedSize() + ")");
			return null;
		}
		
		MajaInputStream mis = null;

		if(mse.getPosition() > -1)
		{
			mis = new MajaInputStream(_fileHandle);
			if(mis.skip(mse.getPosition()) != mse.getPosition())
			{
				MajaApp.displayError("Seek to start position (" + mse.getPosition() + ") failed for '" + this.getPath() + "'");
				return null;
			}
			
			switch(mse.getCompression())
			{
				case MajaEntry.UNCOMPRESSED:
					break;
				case MajaEntry.BZIP2:
					mis.startBzip2();
					break;
				case MajaEntry.GZIP:
					mis.startGzip();
					break;
				default:
				{
					MajaApp.displayError("Attempted to get stream handle for unsupported input stream");
					try{if(mis != null) mis.close();}catch(java.io.IOException e2){}
					return null;
				}
			}
		}
		else if(_handler != null)
		{
			mis = _handler.getInputStream(this, mse);
		}
		else
		{
			
		}
			
		return mis;
	}
	*/

	/*
	public long getLastModified()
	{
		if(!_fileHandle.exists())
			return 0;
		
		return _fileHandle.lastModified();
	}
	*/
	
	public String getName()
	{
		if(this.getFileHandle() != null)
			return this.getFileHandle().getName();
		else
			return "<NO FILE>";
	}
	
	public int getNumDependantEntries()
	{
		return _entries.size();
	}
	
	/*
	public int getNumSubsources()
	{
		return _subSources.size();
	}*/
	
	public int getNumSourceEntries()
	{
		return _headSourceEntry.getNumChildren();
	}
	
	public String getPath()
	{
		return this.getFileHandle().getPath();
	}
	
	public MajaProject getProject()
	{
		return _project;
	}
	
	public long getSize()
	{
		if(this.getFileHandle().isFile() && (_type == MajaSource.FILE || _type == MajaSource.PACKAGE))
			return this.getFileHandle().length();
		else
			return 0;
	}
	
	public MajaSourceEntry getSourceEntryHead()
	{
		return _headSourceEntry;
	}
	
	public MajaSourceEntry getSourceEntry(int idx)
	{
		return _headSourceEntry.getChild(idx);
	}
	
	public int getType()
	{
		return _type;
	}
	
	public String getTypeString()
	{
		switch(this.getType())
		{
			case MajaSource.FILE:
				return "File";
			case MajaSource.DIRECTORY:
				return "Directory";
			case MajaSource.PACKAGE:
				return "Package (" + this.getHandler().getName() + ")";
			case MajaSource.INVALID:
				return "Invalid";
			default:
				return "Unknown";
		}
	}
	
	/*
	public boolean isSourceEntryConnected(MajaSourceEntry mse)
	{
		//Play nice
		if(mse.getSource() == null)
			return false;
		else if(mse.getSource() != this)
			return mse.getSource().isSourceEntryConnected(mse);

		if(_type == this.FILE || _type == this.PACKAGE)
		{
			if(!_fileHandle.isFile())
				return false;

			if((mse.getPosition() + mse.getSize()) > _fileHandle.length())
				return false;

			return true;
		}
		else if(_type == this.DIRECTORY)
		{
			if(!_fileHandle.isDirectory())
				return false;
		}

		return false;
	}
	*/
	
	//Needs to be replaced with per-source sync checks
	/*
	public boolean isEntrySynced(MajaEntry e)
	{
		//Play nice
		if(e.getSource() == null)
			return false;
		else if(e.getSource() != this)
			return e.getSource().isEntrySynced(e);
			
		if(!e.isConnected())
			return false;

		File f = new File(_path);
		if(_type == this.FILE)
		{
			if(e.getSize() != f.length())
				return false;
			
			if(e.getLastModified() != f.lastModified())
				return false;
			
			return true;
		}
		else if(_type == this.PACKAGE)
		{
			MajaEntry realEntry = MajaVPManager.getVPEntryInfo(_path, e.getSourceIndex());
			if(realEntry == null)
				return false;

			return e.isEqualData(realEntry);
		}
		else if(_type == this.DIRECTORY)
		{
			if(e.getLastModified() != f.lastModified())
				return false;
			
			return true;
		}
		
		return false;
	}*/

	public boolean removeDependantEntry(MajaEntry n_entry)
	{
		if(n_entry == null)
			return false;

		if(!_entries.remove(n_entry))
			return false;
		
		//n_entry.clearSource(n_entry);
		n_entry.setSourceEntry(null);
		return true;
	}
	
	/*
	public boolean removeSubsource(MajaSource n_source, boolean removeEntries)
	{
		if(n_source == null)
			return false;
			
		if(!_subSources.remove(n_source))
			return false;

		n_source._parent = null;
		for(int i = 0; i < n_source._entries.size(); i++)
		{
			MajaEntry e = n_source._entries.get(i);
			e.clearSource();
			if(removeEntries)
				e.remove();
		}
		return true;
	}*/
	
	public void setName(String n_name)
	{
		return;
	}
	
	/*
	public void setPath(String n_path, int n_type)
	{
		_path = new String(n_path);
		_type = n_type;
		if(_type == this.FILE || _type == this.PACKAGE)
		{
			java.io.File f = new java.io.File(_path);
			if(!f.isFile() || !f.canRead())
			{
				_type = this.INVALID;
			}
		}
		else if(_type == this.DIRECTORY)
		{
			java.io.File f = new java.io.File(_path);
			if(!f.isDirectory() || !f.canRead())
			{
				_type = this.INVALID;
			}
		}
		else
		{
			//Catch all unsupported or random "types"
			_type = this.INVALID;
		}
	}*/
	
	public void sync()
	{
		_headSourceEntry.sync();
	}

	public void display()
	{
		System.out.println("<" + this.getFileHandle().getPath() + ">");
		System.out.println("Type: " + _type);
		System.out.println("Entries #: " + _entries.size());
		System.out.println("Subsources #: " + this.getNumChildren());
	}
}