package Maja;
import java.io.File;

import MajaIO.*;

public class MajaSourceEntry extends MajaBranch<MajaSourceEntry>
{
	String _name;
	int _type;

	MajaSource _source;
	MajaIndex _sourceIndex;
	
	java.io.File _fileHandle;
	
	int _dataCompression;
	long _dataPosition;
	long _dataCompressedSize;
	long _dataUncompressedSize;
	long _dataLastModified;
	
	public MajaSourceEntry(int n_type)
	{
		this.clear();
		_type = n_type;
	}
	
	public void clear()
	{
		_type = MajaEntry.INVALID;
		_source = null;
		_sourceIndex = null;
		_dataCompression = MajaEntry.UNCOMPRESSED;
		_dataPosition = -1;
		_dataCompressedSize = -1;
		_dataUncompressedSize = -1;
		_dataLastModified = 0;
	}

	/*
	public void clearSource(MajaEntrySource n_caller)
	{
		//Prevents unnecessary recursion
		if(n_caller != this && _source != null)
			_source.removeDependantEntry(this);

		_source = null;
		_dataPosition = 0;
		_dataCompressedSize = 0;
		_dataUncompressedSize = 0;
		_dataLastModified = 0;
	}*/
	
	public MajaSourceEntry export(MajaOutputStream mos, long maxToWrite) throws java.io.IOException
	{
		if(_source == null)
			return null;
		
		return _source.exportSourceEntry(mos, this, maxToWrite);
	}
	
	public MajaSourceEntry addChild(MajaSourceEntry n_child)
	{
		MajaSourceEntry mse = super.addChild(n_child);
		if(mse != null)
		{
			n_child.setSource(this.getSource());
		}
		
		return mse;
	}
	
	public MajaSourceEntry createPath(MajaIndex n_path)
	{
		if(n_path == null)
			return null;
		
		MajaIndex mi = new MajaIndex(n_path);
		String currentLevel = mi.getOuterLevel();
		MajaSourceEntry currentParent = this;
		MajaSourceEntry currentChild = null;
		int i =0;
		while(currentLevel.length() > 0)
		{
			int numChildren = currentParent.getNumChildren();
			for(i = 0; i < numChildren; i++)
			{
				currentChild = currentParent.getChild(i);
				if(currentChild.getName().equals(currentLevel))
				{
					currentParent = currentChild;
					break;
				}
			}
			
			if(i == numChildren)
			{
				MajaSourceEntry mse = new MajaSourceEntry(MajaEntry.FOLDER);
				mse.setName(currentLevel);
				currentParent.addChild(mse);
				currentParent = mse;
			}
			
			mi.removeOuterLevel();
			currentLevel = mi.getOuterLevel();
		}
		
		return currentParent;
	}
	
	public String getCompressionString()
	{
		switch(this.getCompression())
		{
			case MajaEntry.UNCOMPRESSED:
				return "Uncompressed";
			case MajaEntry.BZIP2:
				return "Bzip2";
			case MajaEntry.GZIP:
				return "Gzip";
			default:
				return "Unknown";
		}
	}
	
	public long getLastModified()
	{
		return _dataLastModified;
	}
	
	public long getCompressedSize()
	{
		return _dataCompressedSize;
	}
	
	public int getCompression()
	{
		return _dataCompression;
	}
	
	public java.io.File getFileHandle()
	{
		if(_fileHandle == null && this.getSource() != null && this.getSource().getType() == MajaSource.PACKAGE)
			return this.getSource().getFileHandle();
		else
			return _fileHandle;
	}
	
	public MajaIndex getIndex()
	{
		return new MajaIndex(_sourceIndex);
	}
	
	/**
	* Gets input stream handle for the entry. The InputStream will
	* automatically be set to decompress, if necessary. The file must be synced.
	*/
	public MajaIO.MajaInputStream openInputStream()
	{
		if(this.getStatus() != MajaEntry.SYNCED)
			return null;
			
		MajaIO.MajaInputStream mis = null;

		if(this.getType() == MajaEntry.FILE && this.getPosition() > -1)
		{
			//Open the file
			try
			{
				mis = new MajaInputStream(this.getFileHandle());
				
				//Find the right position
				if(mis.skip(this.getPosition()) != this.getPosition())
				{
					MajaApp.displayWarning("Seek to start position (" + this.getPosition() + ") failed for '" + this.getName() + "'");
					return null;
				}
			}
			catch(java.io.FileNotFoundException exc)
			{
				try{if(mis != null) mis.close();}catch(java.io.IOException e2){}
				MajaApp.displayWarning("Could not find file for source entry '" + this.getName() + "'");
				return null;
			}
			catch(java.io.IOException exc)
			{
				try{if(mis != null) mis.close();}catch(java.io.IOException e2){}
				MajaApp.displayWarning("Could not open/seek '" + this.getName() + "'");
				return null;
			}
			
			//Set the correct compression
			switch(this.getCompression())
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
		else if(this.getSource() != null && this.getSource().getHandler() != null)
		{
			mis = this.getSource().getHandler().getInputStream(this);
		}
		
		//Return what we've got
		
		return mis;
	}
	
	public void closeInputStream(MajaIO.MajaInputStream mis)
	{
		try{mis.close();}catch(java.io.IOException ex){}
	}
	
	public String getName()
	{
		if(_name != null)
			return new String(_name);
		else if(_sourceIndex != null)
			return _sourceIndex.getInnerLevel();
		else if(_fileHandle != null)
			return _fileHandle.getName();
		else
			return _source.getName() + " - " + this.getPosition();
	}
	
	public long getPosition()
	{
		return _dataPosition;
	}
	
	public long getSize()
	{
		if(this.getCompression() == MajaEntry.UNCOMPRESSED)
			return this.getUncompressedSize();
		else
			return this.getCompressedSize();
	}
	
	public MajaSource getSource()
	{
		return _source;
	}
	
	public MajaIndex getSourceIndex()
	{
		return new MajaIndex(_sourceIndex);
	}
	
	public int getStatus()
	{
		MajaSource ms = this.getSource();
		if(ms == null)
			return MajaEntry.DISCONNECTED;
		
		int sourceType = ms.getType();
		if(sourceType == MajaSource.FILE || sourceType == MajaSource.DIRECTORY)
		{
			if(this.getType() == MajaEntry.FOLDER)
			{
				java.io.File f = this.getFileHandle();
				if(f == null)
					return MajaEntry.DISCONNECTED;
				
				if(f.isDirectory())
				{
					File listing[] = f.listFiles();
					boolean synced = true;
					int numChildren = this.getNumChildren();
					int i,j;
					for(i = 0; i < listing.length; i++)
					{
						File currentFile = listing[i];
						for(j = 0; j < numChildren; j++)
						{
							MajaSourceEntry currentChild = this.getChild(i);
							if(currentFile.getName().equals(currentChild.getName()))
							{
								//It's the right file, but is it synced?
								if(currentChild.getStatus() == MajaEntry.SYNCED)
									break;
								else
									return MajaEntry.CONNECTED;
							}
						}
						
						//We couldn't find it.
						if(j == numChildren)
							return MajaEntry.CONNECTED;
					}
					
					//All tests have been passed
					return MajaEntry.SYNCED;
				}
			}
			else if(this.getType() == MajaEntry.FILE)
			{
				java.io.File f = this.getFileHandle();
				if(f == null)
					return MajaEntry.DISCONNECTED;
				if(f.isFile())
				{
					if(this.getSize() == f.length() && this.getPosition() <= f.length() && this.getLastModified() == f.lastModified())
						return MajaEntry.SYNCED;
					
					return MajaEntry.CONNECTED;
				}
			}
		}
		else if(sourceType == MajaSource.PACKAGE)
		{
				MajaHandlerManager.MajaHandler mh = ms.getHandler();
				if(mh == null)
					return MajaEntry.DISCONNECTED;
				
				return mh.manageSourceEntryStatus(this, false);
		}
		else
		{
			MajaApp.displayWarning("Unknown source type '" + ms.getType() + "'");
			return MajaEntry.DISCONNECTED;
		}
		
		return MajaEntry.DISCONNECTED;
	}
	
	public int getType()
	{
		return _type;
	}
	
	public String getTypeString()
	{
		switch(this.getType())
		{
			case MajaEntry.FILE:
				return "File";
			case MajaEntry.FOLDER:
				return "Folder";
			case MajaEntry.INVALID:
				return "Invalid";
			default:
				return "Unknown";
		}
	}
	
	public long getUncompressedSize()
	{
		return _dataUncompressedSize;
	}
	/*
	public boolean isConnected()
	{
		return true;

		if(this.getType() == MajaEntry.FILE)
		{
			java.io.File f = this.getFileHandle();
			if(f == null || !f.isFile())
			{
				return false;
			}

			if(this.getPosition() > -1)
			{
				if((this.getPosition() + this.getSize()) > f.length())
					return false;
			}

			return true;
		}
		else if(_type == MajaEntry.FOLDER)
		{
			if(!_fileHandle.isDirectory())
				return false;
			
			return true;
		}

		return false;

	}
	*/
	
	/*
	public boolean isSynced()
	{
		if(_source == null)
 			return false;
		
		if(!this.isConnected())
			return false;

		//return _source.isEntrySynced(this);
		return true;
	}*/
	
	public void setCompressedSize(long n_dataCompressedSize)
	{
		_dataCompressedSize = n_dataCompressedSize;
	}
	
	public void setCompression(int n_compression)
	{
		_dataCompression = n_compression;
	}
	
	public void setFileHandle(java.io.File in_fileHandle)
	{
		_fileHandle = in_fileHandle;
	}
	
	public void setIndex(MajaIndex n_index)
	{
		_sourceIndex = new MajaIndex(n_index);
	}
	
	public void setLastModified(long n_lastModified)
	{
		_dataLastModified = n_lastModified;
	}
	
	public void setName(String n_name)
	{
		_name = new String(n_name);
	}
	
	public void setPosition(long n_dataPosition)
	{
		_dataPosition = n_dataPosition;
	}
	
	protected void setSource(MajaSource n_source)
	{
		_source = n_source;
		for(int i = 0; i < this.getNumChildren(); i++)
		{
			this.getChild(i).setSource(n_source);
		}
	}
	
	public void setType(int n_type)
	{
		_type = n_type;
	}
	
	public void setUncompressedSize(long n_uncompressedSize)
	{
		_dataUncompressedSize = n_uncompressedSize;
	}
	
	public void sync()
	{
		if(_source == null)
			return;
		
		int sourceType = this.getSource().getType();
		if(sourceType == MajaSource.PACKAGE)
		{
			MajaHandlerManager.MajaHandler mh = this.getSource().getHandler();
			if(mh == null)
				return;
			
			mh.manageSourceEntryStatus(this, true);
			return;
		}
		else if(sourceType == MajaSource.DIRECTORY || sourceType == MajaSource.FILE)
		{
			if(this.getType() == MajaEntry.FILE)
			{
				this.setUncompressedSize(_fileHandle.length());
				this.setLastModified(_fileHandle.lastModified());
				for(int i = 0; i < this.getNumChildren(); i++)
				{
					//_entries.get(i).setSource(this, null, MajaEntry.UNCOMPRESSED, 0, this.getSize(), this.getLastModified());
					this.getChild(i).sync();
				}
			}
			else if(this.getType() == MajaEntry.FOLDER)
			{
				java.io.File[] listing = this.getFileHandle().listFiles();
				boolean[] sourcesExist = new boolean[this.getNumChildren()];
				MajaSourceEntry[] sourceReferences = new MajaSourceEntry[this.getNumChildren()];
				for(int i = 0; i < sourceReferences.length; i++) sourceReferences[i] = this.getChild(i);
	
				for(int i = 0; i < listing.length; i++)
				{
					java.io.File cf = listing[i];
					boolean createCF = true;
					
					//Figure out which sourcesExist we need to create
					for(int j = 0; j < this.getNumChildren(); j++)
					{
						if(this.getChild(j).getFileHandle().equals(cf))
						{
							createCF = false;
							sourcesExist[j] = true;
							break;
						}
					}
					
					//...and create them
					if(createCF)
					{
						//MajaApp.displayStatus("Adding " + cf.getPath());
						MajaSourceEntry mse = null;
						if(cf.isDirectory())
							mse = new MajaSourceEntry(MajaEntry.FOLDER);
						else if(cf.isFile())
							mse = new MajaSourceEntry(MajaEntry.FILE);
						else
							continue;
						
						mse.setFileHandle(cf);
						mse.setUncompressedSize(cf.length());
						mse.setLastModified(cf.lastModified());
						mse.setPosition(0);
						if(this.addChild(mse) != null)
						{
							//MajaApp.displayStatus("Added " + cf.getPath() + "; ENTRIES #: " + _entries.size());
							/*
							for(int j = 0; j < _entries.size(); j++)
							{
								//MajaApp.displayStatus("Adding entry: " + cf.getPath());
								MajaEntry me = null;
								if(_type == this.DIRECTORY)
									me = new MajaEntry(_project, cf.getName(), MajaEntry.FOLDER, cs, null, MajaEntry.UNCOMPRESSED, 0, 0, cf.lastModified());
								else if(_type == this.FILE)
									me = new MajaEntry(_project, cf.getName(), MajaEntry.FILE, cs, null, MajaEntry.UNCOMPRESSED, 0, cf.length(), cf.lastModified());
								_entries.get(j).addChild(me);
							}
							*/
						}
						else
						{
							MajaApp.displayError("Could not add '" + cf.getPath() + "' during sync procedure for '" + _fileHandle.getPath() + "'");
						}
					}
				}
				
				//*****Pass - remove missing sources
				for(int i = 0; i < sourcesExist.length; i++)
				{
					if(!sourcesExist[i])
					{
						sourceReferences[i].remove();
					}
				}
				
				//*****Pass - sync existing sources
				for(int i = 0; i < this.getNumChildren(); i++)
				{
					this.getChild(i).sync();
				}
			}
		}
	}
}