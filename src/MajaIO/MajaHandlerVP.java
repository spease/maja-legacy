package MajaIO;
import java.io.*;
import Maja.*;

public class MajaHandlerVP extends MajaHandlerManager.MajaHandler
{
	public static MajaHandlerVP _currentInstance = null;
	
	//Note: this may cause problems if you import multiple VPs at once via simultaneous threads
	private static long _currentProjectedSize = 0;

	static final String VP_HEADER_STRING = "VPVP";
	static final int VP_HEADER_SIZE = 16;
	static final int VP_VERSION = 2;
	static final int VP_MAX_ENTRY_NAME_LEN = 32;
	static final int VP_MAX_FILE_SIZE = Integer.MAX_VALUE;
	static final String VP_BACKDIR_NAME = "..";
	
	//***************-----METHODS-----***************//
	private MajaHandlerVP(){}
	public static MajaHandlerVP initialize()
	{
		if(_currentInstance != null)
			return _currentInstance;
		
		_currentInstance = new MajaHandlerVP();
		return _currentInstance;
	}

	public String getName()
	{
		return "VP File";
	}
	public String getDescription()
	{
		return "Uncompressed package file used by FS2_Open";
	}
	public String getExtension()
	{
		return "vp";
	}
	
	public MajaInputStream getInputStream(MajaSourceEntry mse)
	{
		if(mse.getStatus() != MajaEntry.SYNCED)
			return null;
		
		MajaSource ms = mse.getSource();
		if(ms == null)
			return null;
		
		java.io.File f = ms.getFileHandle();
		if(f == null || !f.isFile())
			return null;
		
		MajaInputStream mis = null;
		try
		{
			mis = new MajaInputStream(f);
			mis.skip(mse.getPosition());
		}
		catch(java.io.FileNotFoundException exc)
		{
			MajaApp.displayException(exc, "Could not open file '" + f.getPath() + "' for input stream");
			if(mis!=null)try{mis.close();}catch(java.io.IOException exc2){MajaApp.displayException(exc2, "Could not close MajaInputStream");}
			return null;
		}
		catch(java.io.IOException exc)
		{
			MajaApp.displayException(exc, "IOException while opening input stream in '" + f.getPath() + "' for '" + mse.getName() + "'");
			if(mis!=null)try{mis.close();}catch(java.io.IOException exc2){MajaApp.displayException(exc2, "Could not close MajaInputStream");}
		}
		switch(mse.getCompression())
		{
			case MajaEntry.BZIP2:
				mis.startBzip2();
				MajaApp.displayWarning("VP entry '" + mse.getName() + "' in '" + f.getName() + "' is Bzip2-compressed; which is supposedly impossible for a VP.");
				break;
			case MajaEntry.GZIP:
				mis.startGzip();
				MajaApp.displayWarning("VP entry '" + mse.getName() + "' in '" + f.getName() + "' is Gzip-compressed; which is supposedly impossible for a VP.");
				break;
			case MajaEntry.UNCOMPRESSED:
				break;
			default:
				return null;
		}
		return mis;
	}
	
	public int manageSourceEntryStatus(MajaSourceEntry mse, boolean n_sync)
	{
		if(mse == null)
			return MajaEntry.DISCONNECTED;
		
		MajaSource ms = mse.getSource();
		if(ms == null)
			return MajaEntry.DISCONNECTED;
		
		java.io.File f = ms.getFileHandle();
		if(f == null || !f.isFile())
			return MajaEntry.DISCONNECTED;
		
		MajaInputStream mis = null;
		try
		{
			mis = new MajaInputStream(f);
			String f_header;
			int f_version, f_tocLocation, f_NumEntries;
			long vp_total_size = f.length();
			
			f_header = mis.readString(4);
			if(!f_header.equals(VP_HEADER_STRING))
				return MajaEntry.DISCONNECTED;
			
			f_version = mis.readLittleInt();
			if(f_version != VP_VERSION)
				return MajaEntry.DISCONNECTED;
			
			f_tocLocation = mis.readLittleInt();
			if(f_tocLocation < 0 || f_tocLocation >= vp_total_size)
				return MajaEntry.DISCONNECTED;
			
			f_NumEntries = mis.readLittleInt();
			if(f_NumEntries < 1)
				return MajaEntry.DISCONNECTED;
			
			long skipGoal = 0;
			skipGoal = f_tocLocation - VP_HEADER_SIZE;
			long skipAmount = mis.skip(skipGoal);
			if(skipGoal != skipAmount)
				return MajaEntry.DISCONNECTED;

			String f_name;
			int f_dataPosition, f_dataSize, f_timeStamp;

			//Set lastFolder up
			MajaIndex currentIndex = new MajaIndex("");
			for(int i = 0; i < f_NumEntries; i++)
			{
				//Read in the data
				f_dataPosition = mis.readLittleInt();
				f_dataSize = mis.readLittleInt();
				f_name = mis.readString(32);
				f_timeStamp = mis.readLittleInt();
				if(f_name.equals("..") && f_dataSize == 0)
				{
					currentIndex.removeInnerLevel();
				}
				else
				{
					currentIndex.addLevel(f_name);
					
					if(currentIndex.equals(mse.getIndex()))
					{
						if(n_sync)
						{
							mse.setCompression(MajaEntry.UNCOMPRESSED);
							mse.setLastModified((long)f_timeStamp * (long)1000);
							mse.setPosition(f_dataPosition);
							mse.setUncompressedSize(f_dataSize);
						}
						if(mse.getLastModified() == ((long)f_timeStamp * (long)1000)
								&& mse.getSize() == f_dataSize
								&& mse.getPosition() == f_dataPosition)
							return MajaEntry.SYNCED;
						
						return MajaEntry.CONNECTED;
					}
					if(f_dataSize != 0)
						currentIndex.removeInnerLevel();
				}
			}
		}
		catch(java.io.IOException exc)
		{
		}
		finally
		{
			if(mis!=null)try{mis.close();}catch(java.io.IOException exc){MajaApp.displayException(exc,"VP getStatus() couldn't close '" + f.getName() + "'");}
		}
		return MajaEntry.DISCONNECTED;
	}
	
	public String getValidationErrors(MajaEntry e)
	{
		String returnString = "";
		String eName = e.getName();
		int eNameLength = eName.length();
		int eType = e.getType();
		long eSize = e.getSize();
		
		//Type
		if(eType != MajaEntry.FILE && eType != MajaEntry.FOLDER)
		{
			returnString += "VP format only supports files and folders";
		}

		//Name
		if(eNameLength < 1)
		{
			returnString += "Entry must have a name. ";
		}
		if(eNameLength > VP_MAX_ENTRY_NAME_LEN)
		{
			returnString += "Entry name is too long (max is " + VP_MAX_ENTRY_NAME_LEN + " characters). ";
		}
		if(eName.equals(VP_BACKDIR_NAME))
		{
			returnString += "Entry uses reserved name '" + VP_BACKDIR_NAME + "'. ";
		}
		
		//Size
		if(eSize < 1 && eType == MajaEntry.FILE)
		{
			returnString += "All files must have a size greater than 0. ";
		}
		if(eSize > VP_MAX_FILE_SIZE)
		{
			returnString += "All files must have a size less than " + VP_MAX_FILE_SIZE + " bytes. ";
		}
		
		if(returnString.length() > 0)
			return returnString;
		
		//No errors
		return null;
	}

	/**
	* Copies entry data into a VP, and fills out the final TOC entry. Does not export unsynced entries.
	* 
	* @throws 		IOException if the file was incompletely written.
	* @return 		null if nothing was written, or the final entry.
	*/
	private static MajaSourceEntry exportEntryDataToVP(MajaOutputStream mos, MajaEntry me) throws java.io.IOException, MajaHandlerManager.OperationCancelledException
	{
		if(!MajaApp.updateCurrentStatus((float)((float)mos.getCurrentPosition()/(float)_currentProjectedSize), true))
			throw new MajaHandlerManager.OperationCancelledException();

		if(me.getType() == MajaEntry.FOLDER)
		{
			//Folders just write any files that are inside of them.
			MajaSourceEntry finalEntry = new MajaSourceEntry(me.getType());
			finalEntry.setName(me.getName());
			finalEntry.setUncompressedSize(0);
			for(int i = 0; i < me.getNumChildren(); i++)
			{
				MajaEntry currentEntry = me.getChild(i);
				MajaSourceEntry mt = MajaHandlerVP.exportEntryDataToVP(mos, currentEntry);
				if(mt != null)
				{
					finalEntry.addChild(mt);
				}
			}
			return finalEntry;
		}
		else if(me.getType() == MajaEntry.FILE)
		{
			//Do not write invalid files
			if(mos.getCurrentPosition() > Integer.MAX_VALUE)
				return null;

			MajaSourceEntry newEntry = me.export(mos, Integer.MAX_VALUE);
			if(newEntry == null)
				return null;

			return newEntry;
		}

		return null;
	}

	/**
	* Exports entry to a VP TOC.
	* 
	* @throws		IOException if the TOC was incompletely written.
	* @return 		number of entries written
	*/
	public static int exportEntryToVP(MajaOutputStream mos, MajaSourceEntry mse) throws java.io.IOException
	{
		//Number of entries written from within this function (And any recursive calls)
		int retval = 0;
		
		//TOC:
		//ALL are little-endian
		//=====
		//int (4 bytes) - position
		//int (4 bytes) - size
		//char (32 bytes) - name
		//timestamp (4 bytes) - time since 1970 in seconds
		//=====
		long f_position = mse.getPosition();
		long f_size = mse.getSize();
		String f_name = mse.getName();
		int f_timeStamp = (int)(mse.getLastModified() / (long)1000);

		//*****All values should be OK once this function is called. But check anyway.
		if(f_name.length() < 1 || f_name.length() > 32)
		{
			MajaApp.displayError("Invalid name length (" + f_name.length() + ") passed to exportEntryToVP (entry '" + mse.getName() + "' skipped)");
			return retval;
		}

		if(mse.getType() == MajaEntry.FILE)
		{
			if(f_position < 0 || f_position > Integer.MAX_VALUE)
			{
				MajaApp.displayError("Invalid position (" + f_position + ") passed to exportEntryToVP (entry '" + mse.getName() + "' skipped)");
				return retval;
			}
			if(f_size < 1 || f_size > Integer.MAX_VALUE)
			{
				MajaApp.displayError("Invalid size (" + f_size + ") passed to exportEntryToVP (entry '" + mse.getName() + "' skipped)");
				return retval;
			}
			if(mse.getNumChildren() > 0)
			{
				MajaApp.displayWarning("Ignoring invalid file subentries passed to exportEntryToVP (entry '" + mse.getName() + "')");
			}
		}
		else if(mse.getType() == MajaEntry.FOLDER)
		{
			if(f_size != 0)
			{
				MajaApp.displayWarning("Ignoring invalid folder size passed to exportEntryToVP (entry '" + mse.getName() + "')");
			}
			/*if(f_position != 0)
			{
				MajaApp.displayWarning("Ignoring invalid folder data passed to exportEntryToVP (entry '" + mse.getName() + "')");
			}*/
		}
		else
		{
			MajaApp.displayError("Invalid entry type passed to exportEntryToVP (entry '" + mse.getName() + "' skipped)");
			return retval;
		}

		//*****Write the TOC ENTRY!!
		mos.writeLittleInt((int)f_position);
		if(mse.getType() == MajaEntry.FILE)
		{
			mos.writeLittleInt((int)f_size);
		}
		else if(mse.getType() == MajaEntry.FOLDER)
		{
			mos.writeLittleInt(0);
		}
		mos.writeString(f_name, 32);
		mos.writeLittleInt(f_timeStamp);
		
		retval++;

		if(mse.getType() == MajaEntry.FILE)
			return retval;
		
		//*****Write subentries
		for(int i = 0; i < mse.getNumChildren(); i++)
		{
			retval += MajaHandlerVP.exportEntryToVP(mos, mse.getChild(i));
		}
		
		//*****Backdir
		mos.writeLittleInt(0);
		mos.writeLittleInt(0);
		mos.writeString("..", 32);
		mos.writeLittleInt(0);
		retval++;
		
		return retval;
	}
	
	public boolean isPackage(byte[] b)
	{
		try
		{
			String f_header = new String(b, 0, 4, "US-ASCII");
			if(f_header.equals(VP_HEADER_STRING))
				return true;
		}
		catch(java.io.UnsupportedEncodingException e)
		{
			MajaApp.displayException(e, "VP Support is disabled. VP support requires US-ASCII character encoding support");
		}
		
		return false;
	}
	
	/**
	* Determines whether the passed file has the same name as a Freespace official VP file
	*
	* @return			True if the file has a retail name, false if it does not.
	*/
	public boolean isDefaultFSPackage(java.io.File f)
	{
		String filename = f.getName();
		final String defaultVPs[] = {
			//FS1
			"freespace.vp",
			"cbanim.vp",
			"voc.vp",
			"root.vp",
			"fs1.vp",
			//FS2
			"root_fs2.vp",
			"smarty_fs2.vp",
			"sparky_fs2.vp",
			"sparky_hi_fs2.vp",
			"stu_fs2.vp",
			"tango1_fs2.vp",
			"tango2_fs2.vp",
			"tango3_fs2.vp",
			"warble_fs2.vp"
		};
		for(int i = 0; i < defaultVPs.length; i++)
		{
			if(filename.equalsIgnoreCase(defaultVPs[i]))
				return true;
		}
		
		return false;
	}

	/**
	* Exports a VP file using the output path (Current VP export version: 2).
	* Does not export out-of-sync or disconnected entries.
	*/
	public int exportPackage(MajaEntry masterEntry, File f)
	{
		//*****Do we have an outputpath?
		if(f == null)
		{
			MajaApp.displayError("No export location specified for VP export");
			return 0;
		}
		
		if(this.isDefaultFSPackage(f))
		{
			MajaApp.displayError("The export filename you have chosen is identical to a retail Freespace VP file. To prevent accidents, "
								+"Maja will not save any VP files with the same name as retail VPs.");
			return 0;
		}
		
		//*****Assemble the entries we are going to export at the top level
		java.util.Vector<MajaEntry> entriesToWrite = new java.util.Vector<MajaEntry>();
		if(masterEntry.getType() == MajaEntry.HEAD)
		{
			int numChildren = masterEntry.getNumChildren();
			for(int i = 0; i < numChildren; i++)
			{
				MajaEntry currentEntry = masterEntry.getChild(i);
				if(currentEntry.getStatus() == MajaEntry.SYNCED)
					entriesToWrite.add(currentEntry);
			}
		}
		else
		{
			entriesToWrite.add(masterEntry);
		}

		//*****Calculate if this is possible
		//Because VPs use ints to store all size and position data, we cannot have a VP
		//of INT_MAX bytes or more. We couldn't find the index, then.
		long totalProjectedSize = VP_HEADER_SIZE;
		int totalProjectedEntries = 0;
		for(int i = 0; i < entriesToWrite.size(); i++)
		{
			MajaEntry e = entriesToWrite.get(i);
			totalProjectedEntries += e.getTotalEntries(false, true);

			if(totalProjectedEntries >= Integer.MAX_VALUE /*&& i+1 >= p.getNumEntries(true)*/)
			{
				MajaApp.displayError("VP Version " + VP_VERSION + " cannot support a VP with this many entries.");
				return 0;
			}

			//Size
			totalProjectedSize += e.getTotalSize(false);
		}

		if(totalProjectedSize > Integer.MAX_VALUE)
		{
			MajaApp.displayError("VP Version " + VP_VERSION + " cannot support a VP of this size.");
			return 0;
		}
		
		_currentProjectedSize = totalProjectedSize;
		
		//*****Open the file
		MajaOutputStream mos = null;
		boolean _fileOpened = false;			//Used to determine if we can delete the file on exception
		boolean _fileShouldBeDeleted = false;	//Used to determine whether we should delete the file after exception
		try
		{
			MajaApp.displayStatus("=====Exporting VP: " + f.getPath());
			MajaApp.setCurrentStatus("Exporting VP '" + f.getPath() + "'...", 0.0f, true);
			mos = new MajaOutputStream(f);
			_fileOpened = true;

			//*****Write the header
			mos.writeString(VP_HEADER_STRING, 4);
			mos.writeLittleInt(VP_VERSION);
			mos.writeLittleInt((int)totalProjectedSize);
			mos.writeLittleInt(totalProjectedEntries);

			MajaApp.displayStatus("HEADER: " + VP_HEADER_STRING);
			MajaApp.displayStatus("VERSION: " + VP_VERSION);
			MajaApp.displayStatus("TOC LOC: " + (int)totalProjectedSize);
			MajaApp.displayStatus("FILENUM: " + totalProjectedEntries);
			
			if(!MajaApp.updateCurrentStatus((float)(VP_HEADER_SIZE/totalProjectedSize), true))
				throw new MajaHandlerManager.OperationCancelledException();

			//*****Create a temp entries array for writing the end result
			//Note that these will most likely have subentries as well
			MajaSourceEntry tocEntries[] = new MajaSourceEntry[entriesToWrite.size()];
			int numTOCEntries = 0;
			
			//*****Copy data
			for(int i = 0; i < entriesToWrite.size(); i++)
			{
				MajaEntry e = entriesToWrite.get(i);

				MajaSourceEntry finalEntry = MajaHandlerVP.exportEntryDataToVP(mos, e);
				if(finalEntry != null)
					tocEntries[numTOCEntries++] = finalEntry;
			}
			
			long totalActualSize = mos.getCurrentPosition();
			int totalActualEntries = 0;
			
			//*****Write TOC
			for(int i = 0; i < numTOCEntries; i++)
			{
				MajaSourceEntry mse = tocEntries[i];
				totalActualEntries += MajaHandlerVP.exportEntryToVP(mos, mse);
			}
			
			//Re-write the header
			if(totalActualSize != totalProjectedSize || totalActualEntries != totalProjectedEntries)
			{
				mos.setCurrentPosition(0);
				mos.writeString(VP_HEADER_STRING, 4);
				mos.writeLittleInt(VP_VERSION);
				mos.writeLittleInt((int)totalActualSize);
				mos.writeLittleInt(totalActualEntries);
			}
			
			//*****WE'RE DONE!
			return totalActualEntries;
		}
		catch(java.io.FileNotFoundException e)
		{
			MajaApp.displayException(e, "Could not create or open file for exporting VP '" + f.getPath() + "'");
		}
		catch(MajaHandlerManager.OperationCancelledException ex)
		{
			MajaApp.displayWarning("Cancelled export of '" + f.getPath() + "' on user command.");
			_fileShouldBeDeleted = true;
		}
		catch(SecurityException e)
		{
			MajaApp.displayException(e, "Access denied to write to file '" + f.getPath() + "' when exporting VP.");
			_fileShouldBeDeleted = true;
		}
		catch(java.io.IOException e)
		{
			MajaApp.displayException(e, "IO exception for file '" + f.getPath() + "' when exporting VP.");
			_fileShouldBeDeleted = true;
		}
		finally
		{
			try
			{
				if(mos != null) mos.close();
				if(_fileShouldBeDeleted && _fileOpened && !f.delete())
					MajaApp.displayWarning("Could not delete partially-exported VP file '" + f.getPath() + "'. This file is almost certainly corrupt.");
			}
			catch(java.io.IOException e)
			{
				MajaApp.displayError("Unable to close export handle for '" + f.getPath() + "'");
			}
			MajaApp.resetCurrentStatus();
		}
		
		return 0;
	}

	/**
	* A fast function which does not output any info besides the return type.
	*/
	/*
	public static MajaEntry getVPEntryInfo(String n_filePath, MajaIndex mi)
	{
		MajaInputStream mis = null;
		try
		{
			FileInputStream fis = new FileInputStream(n_filePath);
			mis = new MajaInputStream(new BufferedInputStream(fis));
			long vp_total_size = fis.getChannel().size();

			String f_header = mis.readString(4);
			if(!f_header.equals(VP_HEADER_STRING))
				return null;

			int f_version = mis.readInt();
			if(f_version != VP_VERSION)
				return null;
			
			int f_tocLocation = mis.readInt();
			if(f_tocLocation < 0 || f_tocLocation > vp_total_size)
				return null;
			
			int f_numEntries = mis.readInt();
			if(f_numEntries < 0)
				return null;
			
			int skipAmount = f_tocLocation - VP_HEADER_SIZE;
			if(mis.skipBytes(skipAmount) != skipAmount)
				return null;
			
			MajaIndex currentIndex = new MajaIndex("");
			for(int i = 0; i < f_numEntries; i++)
			{
				int f_dataPosition = mis.readInt();
				int f_dataSize = mis.readInt();
				String f_name = mis.readString(32);
				int f_timeStamp = mis.readInt();
				
				if(f_name.equals("..") && f_dataSize == 0)
				{
					currentIndex.removeInnerLevel();
				}
				else
				{
					currentIndex.addLevel(f_name);
					if(f_dataSize == 0)
					{
						if(mi.equals(currentIndex)) return new MajaEntry(f_name, MajaEntry.FOLDER, null, null, 0, 0, 0);
					}
					else
					{
						if(mi.equals(currentIndex)) return new MajaEntry(f_name, MajaEntry.FILE, null, currentIndex, f_dataPosition, f_dataSize, (long)f_timeStamp * (long)1000);
						currentIndex.removeInnerLevel();
					}
				}
			}
		}
		catch(java.io.IOException e) {return null;}
		finally
		{
			try{if(mis != null) mis.close();}catch(java.io.IOException e){}
		}
		
		return null;
	}
	*/

	/**
	* Imports a VP, adds it as a source, and imports all entries. To add the package
	* to the Project's root, set parentEntry to null.
	*/
	public MajaSource importPackage(File f)
	{
		if(f == null)
			return null;
		
		MajaInputStream dis = null;
		MajaSource finalSource = null;
		int totalEntries = 0;
		try
		{
			MajaApp.displayStatus("=====Importing VP: " + f.getPath());
			MajaApp.setCurrentStatus("Importing '" + f.getPath() + "'...", 0.0f, true);
			//FileInputStream fis = new FileInputStream(f);
			dis = new MajaInputStream(f);
			long vp_total_size = f.length();

			//*****Read header
			String f_header;
			int f_version, f_tocLocation, f_NumEntries;

			//Check header
			f_header = dis.readString(4);
			MajaApp.displayStatus("HEADER: " + f_header);
			if(!f_header.equals(VP_HEADER_STRING))
			{
				MajaApp.displayError("Error while importing VP - file has an invalid header (" + f_header + ")");
				dis.close();
				return null;
			}
			
			//Version
			f_version = dis.readLittleInt();
			MajaApp.displayStatus("VERSION: " + f_version);
			if(f_version != VP_VERSION)
			{
				MajaApp.displayError("Error while importing VP - unsupported version (" + f_version + ")");
				dis.close();
				return null;
			}
			
			//TOC Data
			f_tocLocation = dis.readLittleInt();
			MajaApp.displayStatus("TOC LOC: " + f_tocLocation);
			if(f_tocLocation < 0 || f_tocLocation >= vp_total_size)
			{
				MajaApp.displayError("Error while importing VP - TOC location is outside of file (" + f_tocLocation + ")");
				dis.close();
				return null;
			}
			
			//TOC entries
			f_NumEntries = dis.readLittleInt();
			MajaApp.displayStatus("FILENUM: " + f_NumEntries);
			if(f_NumEntries < 0)
			{
				MajaApp.displayError("Error while importing VP - invalid number of TOC entries (" + f_NumEntries + ")");
				dis.close();
				return null;
			}
			
			if(!MajaApp.updateCurrentStatus(0, true))
				throw new MajaHandlerManager.OperationCancelledException();
		
			//Skip to TOC
			long skipGoal = 0;
			skipGoal = f_tocLocation - VP_HEADER_SIZE;
			long skipAmount = dis.skip(skipGoal);
			if(skipGoal != skipAmount)
			{
				MajaApp.displayError("Error while importing VP - could not seek to TOC. Tried: " + skipGoal + ", got: " + skipAmount + ". (No files imported)");
				dis.close();
				return null;
			}
			
			MajaApp.updateCurrentStatus(0, false);

			//*****Read TOC
			//Create source
			finalSource = new MajaSource(f);
			String f_name;
			int f_dataPosition, f_dataSize, f_timeStamp;

			//Set lastFolder up
			MajaSourceEntry currentParent = finalSource.getSourceEntryHead();
			MajaIndex currentIndex = new MajaIndex("");
			for(int i = 0; i < f_NumEntries; i++)
			{
				//Read in the data
				f_dataPosition = dis.readLittleInt();
				f_dataSize = dis.readLittleInt();
				f_name = dis.readString(32);
				f_timeStamp = dis.readLittleInt();
				//Debug stuff
				//MajaApp.displayStatus(f_dataPosition + " | " + f_dataSize + " | " + f_name + " | " + f_timeStamp);

				if(f_name.equals("..") && f_dataSize == 0)
				{
					if(currentParent != finalSource.getSourceEntryHead())
					{
						currentParent = currentParent.getParent();
						currentIndex.removeInnerLevel();
					}
					else
					{
						MajaApp.displayWarning("Too many backdir entries in VP file. VP is probably corrupted.");
					}
				}
				else
				{
					//Validate name, correct if necessary
					if(f_name.length() < 1)
					{
						String newName = "#" + totalEntries + "#";
						MajaApp.displayError("Error while importing VP - Nameless entry detected in '" + currentIndex.toString() + "'. VP may be corrupted. Entry will be added as '" + newName + "'");
						f_name = newName;
					}
					
					//Create and add the actual entry
					MajaSourceEntry mse = null;

					//Add level, regardless of file or folder, so we can store the proper index
					currentIndex.addLevel(f_name);

					//Handle files
					boolean addSource = true;
					int addType = MajaEntry.FOLDER;
					if(f_dataSize != 0)
					{
						addType = MajaEntry.FILE;
						if(f_dataSize < 0)
						{
							MajaApp.displayError("Entry '" + f_name + "' has an invalid size (" + f_dataSize + ") and will not be writable.");
							addSource = false;
						}
						else if(f_dataPosition < VP_HEADER_SIZE || (f_dataPosition >= f_tocLocation))
						{
							MajaApp.displayError("Entry '" + f_name + "' has an invalid position (" + f_dataPosition + ") and will not be writable.");
							addSource = false;
						}
						else if((f_dataPosition + f_dataSize) > f_tocLocation)
						{
							MajaApp.displayError("Entry '" + f_name + "' claims to have a position and size that would overwrite the TOC, and will not be writable.");
							addSource = false;
						}
					}
					if(addSource)
					{
						mse = new MajaSourceEntry(addType);
						mse.setCompression(MajaEntry.UNCOMPRESSED);
						mse.setIndex(currentIndex);
						mse.setLastModified((long)f_timeStamp * (long)1000);
						mse.setPosition(f_dataPosition);
						mse.setUncompressedSize(f_dataSize);
					}
					
					if(addType != MajaEntry.FOLDER)
					{
						//We don't have subfiles :P
						currentIndex.removeInnerLevel();
					}
					
					//If we are in a folder, add it there.
					if(mse != null)
					{
						mse = currentParent.addChild(mse);
						if(mse == null)
							MajaApp.displayError("Error adding entry to '" + currentParent.getName() + "'");
							
						//Set lastFolder
						if(mse.getType() == MajaEntry.FOLDER)
						{
							currentParent = mse;
						}
		
						//Increment totalEntries
						totalEntries++;
					}
					MajaApp.updateCurrentStatus((float)totalEntries / (float)f_NumEntries, false);
				}
			}
			
			//Just a nice little anal-check
			if(currentParent != finalSource.getSourceEntryHead())
			{
				MajaApp.displayWarning("Note: Imported VP lacked ending backdirs. This does not affect importing in any way.");
			}
		}
		catch (MajaHandlerManager.OperationCancelledException e)
		{
			MajaApp.displayWarning("Cancelled import of '" + f.getPath() + "' on user command.");
		}
		catch (java.io.FileNotFoundException e)
		{
			MajaApp.displayException(e, "Could not find VP file '" + f.getPath() + "'");
		}
		catch(java.io.IOException e)
		{
			MajaApp.displayException(e, "IO Exception while importing VP '" + f.getPath() + "'");
		}
		finally
		{
			//Close file
			if(dis != null)
			{
				try
				{
					dis.close();
				}
				catch(java.io.IOException e)
				{
					MajaApp.displayError("Unable to close import handle for '" + f.getPath() + "'");
				}
			}
			//Remove source (if it hasn't been added to) and return
			if(finalSource != null && finalSource.getNumSourceEntries() < 1)
			{
				finalSource = null;
			}
		}
		
		//For testing disconnected behavior
		//ms.setPath("CANDYLAND", MajaSource.FILE);
		
		//Reset status, we're done
		MajaApp.resetCurrentStatus();

		return finalSource;
	}
}