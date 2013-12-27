package Maja;
import MajaIO.*;

public class MajaEntry extends MajaBranch<MajaEntry> implements java.awt.datatransfer.Transferable
{
	//This
	String _name;
	int _type;
	int _compression;

	//Source
	MajaSourceEntry _sourceEntry;
	
	//*****Type enumerations
	public static final int INVALID = 0;
	public static final int FILE = 1;
	public static final int FOLDER = 2;
	public static final int HEAD = 3;
	
	//*****Status enumerations
	public static final int DISCONNECTED = 0;
	public static final int CONNECTED = 1;
	public static final int SYNCED = 2;
	
	//*****Compression enumerations
	public static final int DEFAULT = 0;
	public static final int UNCOMPRESSED = 1;
	public static final int BZIP2 = 2;
	public static final int GZIP = 3;
	public static final int UNKNOWN = 4;
	
	//*****Data transfer stuff
	public static final java.awt.datatransfer.DataFlavor _majaEntryDataFlavor =
		new java.awt.datatransfer.DataFlavor(MajaEntry.class, "Maja Entry");

	/**
	* In case you need to buffer data using a MajaEntry
	*/
	/*
	public MajaEntry()
	{
		_type = this.INVALID;
	}
	
	public MajaEntry(MajaProject n_project, String n_name)
	{
		_project = n_project;
		_parent = null;
		
		_name = new String(n_name);
		_compression = MajaEntry.UNCOMPRESSED;
		_sourcEntry = null;
	}
	*/
	
	public MajaEntry(int n_type)
	{
		_type = n_type;
		this.setCompression(MajaEntry.DEFAULT);
	}
	
	public MajaEntry(MajaSourceEntry n_sourceEntry, boolean recursive)
	{	
		if(n_sourceEntry != null)
		{
			this.setName(n_sourceEntry.getName());
			this.setType(n_sourceEntry.getType());
		}
		this.setCompression(MajaEntry.DEFAULT);
		this.setSourceEntry(n_sourceEntry);
		
		if(recursive && _sourceEntry != null)
		{
			for(int i = 0; i < _sourceEntry.getNumChildren(); i++)
			{
				this.addChild(new MajaEntry(_sourceEntry.getChild(i), true));
			}
		}
	}
	
	public MajaEntry clone() throws CloneNotSupportedException
	{
		//MajaEntry newMajaEntry = new MajaEntry(_project, _name, _type, _source, _sourceIndex, _dataCompression, _dataPosition, _dataCompressedSize, _dataLastModified);
		MajaEntry newMajaEntry = new MajaEntry(_sourceEntry, false);
		newMajaEntry.setCompression(_compression);
		newMajaEntry.setName(_name);
		newMajaEntry.setType(_type);
		for(int i = 0; i < this.getNumChildren(); i++)
		{
			newMajaEntry.addChild(this.getChild(i).clone());
		}
		
		return newMajaEntry;
	}
	
	public int getCompression()
	{
		return _compression;
	}
	
	public String getName()
	{
		return new String(_name);
	}
	
	public long getSize()
	{
		if(this.getType() == MajaEntry.FOLDER)
			return 0;
		
		if(_sourceEntry != null)
		{
			if(this.getCompression() == MajaEntry.UNCOMPRESSED)
				return _sourceEntry.getUncompressedSize();
			if(this.getCompression() == _sourceEntry.getCompression())
				return _sourceEntry.getCompressedSize();
		}
		
		//"Default" depends on the filetype
		
		return -1;
	}
	
	public MajaSourceEntry getSourceEntry()
	{
		return _sourceEntry;
	}
	
	public int getType()
	{
		return _type;
	}
	
	public void setCompression(int n_c)
	{
		_compression = n_c;
	}
	
	public void setName(String n_name)
	{
		_name = new String(n_name);

		//This needs to be moved back, or the undo system rethought
		//MajaUndoManager.postUndoEvent(this.createUndoEvent(MajaEntry.UndoEvent.SET_NAME, null));
	}
	
	/*
	public void setSource(MajaSource n_source)
	{
		if(n_source.getType() == MajaSource.FILE)
			this.setSource(n_source, null, MajaEntry.UNCOMPRESSED, 0, n_source.getSize(), n_source.getLastModified());
		else
			MajaApp.displayError("Insufficient information given to set source for '" + this.getName() + "'");
	}

	public void setSource(MajaSource n_source, MajaIndex n_sourceIndex, int n_dataCompression, long n_dataPosition, long n_dataCompressedSize, long n_dataLastModified)
	{
		_source = n_source;
		if(_source != null) _source.addDependantEntry(this);

		if(n_sourceIndex == null)
			_sourceIndex = null;
		else
			_sourceIndex = new MajaIndex(n_sourceIndex);

		this.setCompression(n_dataCompression);
		this.setPosition(n_dataPosition);
		this.setCompressedSize(n_dataCompressedSize);
		_dataLastModified = n_dataLastModified;
		
		_type = this.INVALID;
		
		if(_source != null)
		{
			if(_source.getType() == MajaSource.FILE || _source.getType() == MajaSource.PACKAGE)
			{
				_type = this.FILE;
			}
			else if(_source.getType() == MajaSource.DIRECTORY)
			{
				_type = this.FOLDER;
			}
		}
	}*/
	
	public void setSourceEntry(MajaSourceEntry n_mse)
	{
		if(n_mse.getType() == this.getType())
			_sourceEntry = n_mse;
	}
	
	public void setType(int n_type)
	{
		_type = n_type;
	}
	
	public long getTotalSize(boolean n_includeUnsyncedEntries)
	{
		if(!n_includeUnsyncedEntries && this.getStatus() != MajaEntry.SYNCED)
			return 0;

		long returnValue = this.getSize();
		for(int i = 0; i < this.getNumChildren(); i++)
			returnValue += this.getChild(i).getTotalSize(n_includeUnsyncedEntries);
		
		return returnValue;
	}
	
	/*
	public int importPackage(java.io.File f)
	{
		return MajaIO.MajaHandlerManager.importPackage(this.getProject(), this, f);
	}
	*/

	/**
	* Gets the total entries (including the entry this is being called on). So if you called this on a folder,
	* you would get 1 + (number of items in the folder)
	*/
	public int getTotalEntries(boolean n_includeUnsyncedEntries, boolean n_includeBackdirEntries)
	{
		if(!n_includeUnsyncedEntries && this.getStatus() != MajaEntry.SYNCED)
			return 0;

		int returnValue = 1;
		for(int i = 0; i < this.getNumChildren(); i++)
		{
			returnValue += this.getChild(i).getTotalEntries(n_includeUnsyncedEntries, n_includeBackdirEntries);
		}

		if(this.getType() == MajaEntry.FOLDER && n_includeBackdirEntries)
			returnValue++;

		return returnValue;
	}
	
	public java.io.File createOutputPath()
	{
		java.io.File path = this.getOutputDirectory();
		if(path == null)
			return null;

		path.mkdirs();
		if(!path.isDirectory())
		{
			//We couldn't create the path for some reason.
			MajaApp.displayError("Couldn't create output path '" + path.getPath() + "' for entry '" + this.getName() + "'");
			return null;
		}
		
		java.io.File file = new java.io.File(path, this.getName());
		try
		{
			file.createNewFile();
		}
		catch(java.io.IOException ex)
		{
			return null;
		}
		
		if(file.exists())
			return file;
		else
			return null;
	}
	
	public java.io.File getOutputDirectory()
	{
		if(_project.getOutputPath() == null)
			return null;

		//*****Build a list of the parent entries
		java.util.Vector<MajaEntry> parentEntries = new java.util.Vector<MajaEntry>();
		MajaEntry nextParent = _parent;
		while(nextParent != null)
		{
			parentEntries.add(nextParent);
			nextParent = nextParent._parent;
		}
		
		//*****Now use that list to create the path for the file
		java.io.File path = _project.getOutputPath();
		int i = parentEntries.size();
		if(i > 0)
		{
			for(i--; i >= 0; i--)
			{
				path = new java.io.File(path, parentEntries.get(i).getName());
			}
		}
		return path;
	}
	
	/**
	* Sets output stream handle for this entry. You should call closeOutputStreamHandle()
	* when you have finished writing. This method does not modify the entry in any way.
	*/
	public MajaIO.MajaOutputStream openOutputStream()
	{
		if(_project == null)
			return null;
		
		//*****Get the output path
		java.io.File df = this.createOutputPath();
		if(df == null)
			return null;

		MajaIO.MajaOutputStream returnValue = null;
		
		try
		{
			returnValue = new MajaIO.MajaOutputStream(df);
		}
		catch(java.io.FileNotFoundException ex)
		{
			MajaApp.displayException(ex, "Couldn't create output file '" + df.getPath() + "' for entry '" + this.getName() + "'");
			//Close output stream, if possible.
			try{if(returnValue!=null)returnValue.close();}catch(java.io.IOException ex2){}
			return null;
		}
		
		//this.setSource(_project.addSource(new MajaSource(_project, df)), null, MajaEntry.UNCOMPRESSED, 0, 0, System.currentTimeMillis());
		return returnValue;
	}
	
	/**
	* Call this when you've finished with openOutputStreamHandle(). If an error occured,
	* call this with null to clean up.
	*/
	public void closeOutputStream(MajaIO.MajaOutputStream mos)
	{
		java.io.File f = new java.io.File(this.getOutputDirectory(), this.getName());
		
		if(mos == null && f != null)
		{
			f.delete();
			return;
		}

		if(mos != null)
			try{mos.close();}catch(java.io.IOException ex){}

		if(f == null)
			return;
		
		MajaSourceEntry mse = this.getSourceEntry();
		if(mse != null)
		{
			MajaSource ms = mse.getSource();
			if(ms != null)
			{
				//Check if we need to remove the entry from the source.
				if(!ms.getPath().equals(f))
				{
					ms.removeDependantEntry(this);
					this.setSourceEntry(null);
				}
			}
		}
		
		if(this.getSourceEntry() == null)
		{
			//Don't forget _project.addSource()
			this.setSourceEntry(_project.addSource(new MajaSource(f)).getSourceEntryHead());
		}
		this.sync();
	}
	
	public MajaIO.MajaInputStream openInputStream()
	{
		MajaSourceEntry mse = this.getSourceEntry();
		if(mse == null)
			return null;
		
		return mse.openInputStream();
	}
	
	public void closeInputStream(MajaIO.MajaInputStream mis)
	{
		try{mis.close();}catch(java.io.IOException ex){}
	}
	
	/*public boolean isEqualData(MajaEntry e)
	{
		return	(_dataPosition == e._dataPosition
			&& _dataCompressedSize == e._dataCompressedSize
			&& _dataUncompressedSize == e._dataUncompressedSize
			&& _dataLastModified == e._dataLastModified);
	}*/
	/**
	* Checks to see whether an entry is still connected. This means that the source still exists,
	* and is still large enough that the dataPosition and dataSize variables can be used. Obviously,
	* if the actual content has been moved or changed inside the file, this will not be detected.
	*/
	public int getStatus()
	{
		switch(this.getType())
		{
			case MajaEntry.INVALID:
				return MajaEntry.DISCONNECTED;
			case MajaEntry.HEAD:
				return MajaEntry.SYNCED;
			case MajaEntry.FOLDER:
				if(_sourceEntry == null)
					return MajaEntry.SYNCED;
				//No break intentionally
			default:
				return _sourceEntry.getStatus();
		}
		
		/*
		for(int i = 0; i < this.getNumChildren(); i++)
		{
			if(!this.getChild(i).isSynced())
				return false;
		}
		
		MajaEntry parent = this.getParent();
		while(type != MajaEntry.HEAD)
		{
			if(!parent.isSynced())
				return false;
			parent = this.getParent();
			type = parent.getType();
		}
		*/
	}
	
	public String getStatusString()
	{
		switch(this.getStatus())
		{
			case MajaEntry.DISCONNECTED:
				return "Disconnected";
			case MajaEntry.CONNECTED:
				return "Unsynced";
			case MajaEntry.SYNCED:
				return "Synced";
			default:
				return "Unknown";
		}
	}
	
	/**
	* Transmorgrifies MajaEntry into targeted object
	*/
	/*
	public boolean importData(java.io.File f)
	{
		MajaEntry parent = this.getParent();
		MajaSource ms = null;

		if(f.isDirectory())
		{
			ms = new MajaSource(f);
			
			MajaSourceEntry mse = new MajaSourceEntry(f.getName(), MajaEntry.FOLDER);
			mse.setLastModified(f.lastModified());
			ms.addSourceEntry(mse);
			
			this.setName(f.getName());
			this.setSourceEntry(null);
			this.setType(MajaEntry.FOLDER);
			this.setCompression(MajaEntry.UNCOMPRESSED);
			this.setSourceEntry(mse);
			//this.setSource(ms, null, MajaEntry.UNCOMPRESSED, 0, 0, 0);
		}
		else if(f.isFile())
		{
			ms = new MajaSource(f);
			
			MajaSourceEntry mse = new MajaSourceEntry(f.getName(), MajaEntry.FILE);
			mse.setUncompressedSize(f.length());
			mse.setLastModified(f.lastModified());
			ms.addSourceEntry(mse);
			
			this.setName(f.getName());
			this.setType(MajaEntry.FILE);
			this.setCompression(MajaEntry.UNCOMPRESSED);
			this.setSourceEntry(mse);
			//this.setSource(ms, null, MajaEntry.UNCOMPRESSED, 0, f.length(), f.lastModified());
		}
		else
		{
			return false;
		}
		
		if(this.getProject() != null)
			this.getProject().addSource(ms);
		
		if(parent != null)
		{
			_parent = parent;
			MajaSource parentSource = parent.getSource();
			if(parentSource != null)
			{
				parentSource.addSubsource(ms);
			}
		}
		
		return true;
	}
	*/
	
	/**
	* Exports file contents
	*/
	public MajaSourceEntry export(MajaOutputStream mos, long maxToWrite) throws java.io.IOException
	{
		if(this.getStatus() != MajaEntry.SYNCED)
			return null;
			
		return _sourceEntry.export(mos, maxToWrite);
	}
	
	/**
	* Exports file or folder. Does not export if entry is not synced.
	*/
	public void export(java.io.File f)
	{
		if(this.getStatus() != MajaEntry.SYNCED)
			return;
			
		MajaApp.setCurrentStatus("Exporting '" + this.getName() + "'...", -1.0f, false);

		if(this.getType() == MajaEntry.FOLDER)
		{
			boolean directoryExists = false;
			if(f.isDirectory())
				directoryExists = true;
			else if(f.mkdir())
				directoryExists = true;
			
			if(directoryExists)
			{
				for(int i = 0; i < this.getNumChildren(); i++)
				{
					MajaEntry sub = this.getChild(i);
					sub.export(new java.io.File(f,sub.getName()));
				}
			}
			else
			{
				MajaApp.displayWarning("Could not create/open directory '" + f.getPath() + "' during export of '" + this.getName() + "'");
			}
		}
		else if(this.getType() == MajaEntry.FILE)
		{
			MajaIO.MajaOutputStream mos = null;
			try
			{
				mos = new MajaIO.MajaOutputStream(f);
				_sourceEntry.export(mos, Long.MAX_VALUE);
			}
			/*catch(MajaPackageManager.IncompleteExportException e)
			{
				MajaApp.displayException(e, "Incomplete export of '" + this.getName() + "'");
				f.delete();
			}*/
			catch(java.io.FileNotFoundException e)
			{
				MajaApp.displayException(e, "Could not create file '" + f.getPath() + "'");
			}
			catch(java.io.IOException e)
			{
				MajaApp.displayException(e, "Error exporting entry '" + this.getName() + "'");
				f.delete();
			}
			finally
			{
				if(mos != null)try{mos.close();}catch(java.io.IOException e){MajaApp.displayException(e, "export close error");}
			}
		}
		
		MajaApp.resetCurrentStatus();
	}
	
	public void display()
	{
		display("");
	}
	public void display(String tabstring)
	{
		System.out.println(tabstring + "<" + _name + ">");
		tabstring += "\t";
		if(this.getType() == MajaEntry.FILE)
		{
			/*
			if(_source != null)
				System.out.println(tabstring + "Src: " + _source.getPath());
			else
				System.out.println(tabstring + "Src: (none)");
			if(_sourceIndex != null)
				System.out.println(tabstring + "Idx: " + _sourceIndex.toString());
			else
				System.out.println(tabstring + "Idx: (none)");
			System.out.println(tabstring + "Pos: " + this.getPosition() + " (" + _dataPosition + ")");
			System.out.println(tabstring + "Size: " + this.getCompressedSize() + " (" + _dataCompressedSize + ")");
			System.out.println(tabstring + "Time: " + new Date(this.getLastModified()).toString() + " (" +_dataLastModified + ")");
			
			System.out.print(tabstring + "STATUS: ");
			if(this.isConnected()) System.out.print("CONNECTED ");
			if(this.isSynced()) System.out.print("SYNCED ");
			System.out.println();
			*/
		}
		for(int i = 0; i < this.getNumChildren(); i++)
		{
			this.getChild(i).display(tabstring);
		}
		tabstring = tabstring.substring(0, tabstring.length()-1);
		System.out.println(tabstring + "</" + _name + ">");
	}
	
	public void sync()
	{
		if(_sourceEntry != null)
		{
			_sourceEntry.sync();
		}
	}
	
	//***************-----INTERFACE: Transferable-----***************
	public static java.awt.datatransfer.DataFlavor getDataFlavor()
	{
		return _majaEntryDataFlavor;
	}

	public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) throws java.awt.datatransfer.UnsupportedFlavorException
	{
		if(flavor.equals(MajaEntry.getDataFlavor()))
		{
			return this;
		}
		else if(flavor.equals(java.awt.datatransfer.DataFlavor.stringFlavor))
		{
			return this.getName();
		}
		else if(flavor.equals(java.awt.datatransfer.DataFlavor.javaFileListFlavor))
		{
			MajaSourceEntry mse = this.getSourceEntry();
			if(mse != null)
			{
				MajaSource ms = mse.getSource();
				if(ms != null && (ms.getType() == MajaSource.FILE || ms.getType() == MajaSource.DIRECTORY))
				{
					java.util.Vector<java.io.File> files = new java.util.Vector<java.io.File>();
					files.add(mse.getFileHandle());
					return files;
				}
			}
		}
		
		throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
	}
	
	public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors()
	{
		java.util.Vector<java.awt.datatransfer.DataFlavor> tdf = new java.util.Vector<java.awt.datatransfer.DataFlavor>();
		
		//1: Data flavor
		tdf.add(_majaEntryDataFlavor);
		
		//2: String flavor
		tdf.add(java.awt.datatransfer.DataFlavor.stringFlavor);
		
		//3: FileListFlavor
		//Commented this out because I can't see a good reason that people would want to
		//move files from projects already on the hard drive
		//Otherwise, it works.
		/*
		MajaSourceEntry mse = this.getSourceEntry();
		if(mse != null)
		{
			MajaSource ms = mse.getSource();
			if(ms != null && (ms.getType() == MajaSource.FILE || ms.getType() == MajaSource.DIRECTORY))
			{
				tdf.add(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
			}
		}
		*/
		
		//Convert to array
		java.awt.datatransfer.DataFlavor rval[] = new java.awt.datatransfer.DataFlavor[tdf.size()];
		for(int i = 0; i < tdf.size(); i++)
		{
			rval[i] = tdf.get(i);
		}
		
		return rval;
	}
	
	public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor)
	{
		java.awt.datatransfer.DataFlavor[] tdf = this.getTransferDataFlavors();
		for(int i = 0; i < tdf.length; i++)
		{
			if(tdf[i].equals(flavor))
			{
				return true;
			}
		}

		return false;
	}
	//********************UndoEvent*************************
	/*
	public MajaEntry.UndoEvent createUndoEvent(int action, MajaEntry relatedEntry)
	{
		MajaEntry b = new MajaEntry();	//buffer for values
		switch(action)
		{
			case MajaEntry.UndoEvent.SET_NAME:
			{
				b._name = new String(_name);
				return new MajaEntry.UndoEvent(this, "set event name", action, b);
			}
			case MajaEntry.UndoEvent.ADD_SUBENTRY:
			{
				return new MajaEntry.UndoEvent(this, "add subentry", action, relatedEntry);
			}
			case MajaEntry.UndoEvent.REMOVE_SUBENTRY:
			{
				return new MajaEntry.UndoEvent(this, "remove subentry", action, relatedEntry);
			}
			default:
				break;
		}
		
		return null;
	}*/
	/*
	public static class UndoEvent extends MajaUndoEvent
	{
		MajaEntry _target;
		MajaEntry _buffer;
		
		public static final int NONE = 0;
		public static final int SET_NAME = 1;
		public static final int ADD_SUBENTRY = 2;
		public static final int REMOVE_SUBENTRY = 3;
		
		UndoEvent(MajaEntry n_target, String n_description, int n_action, MajaEntry n_buffer)
		{
			super(null, n_action, n_description);
			_target = n_target;
			_buffer = n_buffer;
		}
		
		public MajaEntry getBuffer()
		{
			return _buffer;
		}
		
		public MajaEntry getTarget()
		{
			return _target;
		}
		
		public boolean undo()
		{
			super.undo();
			switch(this.getAction())
			{
				case MajaEntry.UndoEvent.SET_NAME:
				{
					_target._name = new String(_buffer._name);
					return true;
				}
				case MajaEntry.UndoEvent.ADD_SUBENTRY:
				{
					_target.removeChild(_buffer);
					return true;
				}
				case MajaEntry.UndoEvent.REMOVE_SUBENTRY:
				{
					_target.addChild(_buffer);
					return true;
				}
				default:
					break;
			}

			return false;
		}
	}
	*/
}