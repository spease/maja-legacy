package MajaIO;
import java.io.*;
import Maja.*;

public class MajaHandlerCVP extends MajaHandlerManager.MajaHandler
{
	//********************Singleton support
	public static MajaHandlerCVP _currentInstance = null;
	
	static final String CVP_HEADER_STRING = "CPVP";
	static final int CVP_HEADER_SIZE = 24;
	static final int CVP_VERSION = 1;
	static final int CVP_MAX_ENTRY_NAME_LEN = 32;
	static final int CVP_MAX_FILE_SIZE = Integer.MAX_VALUE;
	static final String CVP_BACKDIR_NAME = "..";
	
	//***************-----METHODS-----***************//
	private MajaHandlerCVP(){}
	public static MajaHandlerCVP initialize()
	{
		if(_currentInstance != null)
			return _currentInstance;
		
		_currentInstance = new MajaHandlerCVP();
		return _currentInstance;
	}
	
	public String getName()
	{
		return "Compressed VP File";
	}

	public String getDescription()
	{
		return "Compressed package file used by FS2_Open";
	}

	public String getExtension()
	{
		return "cvp";
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
				break;
			case MajaEntry.GZIP:
				mis.startGzip();
				MajaApp.displayWarning("CVP entry '" + mse.getName() + "' in '" + f.getName() + "' is Gzip-compressed; which is supposedly impossible for a CVP.");
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
		MajaApp.displayError("SourceEntryStatus not implemented for CVP.");
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
			returnString += "CVP format only supports files and folders";
		}

		//Name
		if(eNameLength < 1)
		{
			returnString += "Entry must have a name. ";
		}
		if(eNameLength > CVP_MAX_ENTRY_NAME_LEN)
		{
			returnString += "Entry name is too long (max is " + CVP_MAX_ENTRY_NAME_LEN + " characters). ";
		}
		if(eName.equals(CVP_BACKDIR_NAME))
		{
			returnString += "Entry uses reserved name '" + CVP_BACKDIR_NAME + "'. ";
		}
		
		//Size
		if(eSize < 1 && eType == MajaEntry.FILE)
		{
			returnString += "All files must have a size greater than 0. ";
		}
		if(eSize > CVP_MAX_FILE_SIZE)
		{
			returnString += "All files must have a size less than " + CVP_MAX_FILE_SIZE + " bytes. ";
		}
		
		if(returnString.length() > 0)
			return returnString;
		
		//No errors
		return null;
	}
	
	public boolean isPackage(byte[] b)
	{
		try
		{
			String f_header = new String(b, 0, 4, "US-ASCII");
			if(f_header.equals(CVP_HEADER_STRING))
				return true;
		}
		catch(java.io.UnsupportedEncodingException e)
		{
			MajaApp.displayException(e, "CVP Support is disabled. CVP support requires US-ASCII character encoding support");
		}
		
		return false;
	}
	public int exportPackage(MajaEntry masterEntry, File f)
	{
		MajaApp.displayError("CVP Export is not implemented yet.");
		return 0;
	}
	public MajaSource importPackage(File f)
	{
		if(f == null)
			return null;
		
		MajaInputStream dis = null;
		MajaSource finalSource = null;
		int totalEntries = 0;
		try
		{
			MajaApp.displayStatus("=====Importing CVP: " + f.getPath());
			MajaApp.setCurrentStatus("Importing '" + f.getPath() + "'...", 0.0f, true);
			//FileInputStream fis = new FileInputStream(f);
			dis = new MajaInputStream(f);
			long cvp_total_size = f.length();

			//*****Read header
			String f_header;
			int f_version, f_tocLocation, f_uncompressedSize, f_NumEntries;
			long f_crcData;

			//Check header
			f_header = dis.readString(4);
			MajaApp.displayStatus("HEADER: " + f_header);
			if(!f_header.equals(CVP_HEADER_STRING))
			{
				MajaApp.displayError("Error while importing CVP - file has an invalid header (" + f_header + ")");
				dis.close();
				return null;
			}
			
			//Version
			f_version = dis.readLittleInt();
			MajaApp.displayStatus("VERSION: " + f_version);
			if(f_version != CVP_VERSION)
			{
				MajaApp.displayError("Error while importing CVP - unsupported version (" + f_version + ")");
				dis.close();
				return null;
			}
			
			//TOC Data
			f_tocLocation = dis.readLittleInt();
			MajaApp.displayStatus("TOC LOC: " + f_tocLocation);
			if(f_tocLocation < 0 || f_tocLocation >= cvp_total_size)
			{
				MajaApp.displayError("Error while importing CVP - invalid TOC location (" + f_tocLocation + ")");
				dis.close();
				return null;
			}
			
			//TOC entries
			f_NumEntries = dis.readLittleInt();
			MajaApp.displayStatus("FILENUM: " + f_NumEntries);
			if(f_NumEntries < 0)
			{
				MajaApp.displayError("Error while importing CVP - invalid number of TOC entries (" + f_NumEntries + ")");
				dis.close();
				return null;
			}
			
			if(!MajaApp.updateCurrentStatus(0, true))
				throw new MajaHandlerManager.OperationCancelledException();

			//Display what we know so far
		
			//Skip to TOC
			int skipAmount = f_tocLocation - CVP_HEADER_SIZE;
			if(dis.skip(skipAmount) != skipAmount)
			{
				MajaApp.displayError("Error while importing VP - could not seek to TOC. (No files imported)");
				dis.close();
				return null;
			}
			
			MajaApp.updateCurrentStatus(0, false);

			//*****Read TOC
			//Because the TOC is always compressed, read everything that we can.
			if(!dis.readString(2).equals("BZ"))
			{
				MajaApp.displayError("Bad compression in CVP");
				dis.close();
				return null;
			}
			dis.startBzip2();

			//Create source
			finalSource = new MajaSource(f);
			String f_name;
			int f_dataPosition, f_dataSizeUncompressed, f_dataSizeCompressed, f_timeStamp;

			//Set lastFolder up
			MajaSourceEntry lastFolder = null;
			MajaIndex currentIndex = new MajaIndex("");
			for(int i = 0; i < f_NumEntries; i++)
			{
				//Read in the data
				f_dataPosition = dis.readLittleInt();
				f_dataSizeUncompressed = dis.readLittleInt();
				f_dataSizeCompressed = dis.readLittleInt();
				f_name = dis.readString(32);
				f_timeStamp = dis.readLittleInt();

				if(f_name.length() < 1)
				{
					MajaApp.displayError("Error while importing CVP - Empty entry detected in CVP file. CVP may be corrupted. (Entry was skipped)");
				}
				else if(f_name.equals("..") && f_dataSizeUncompressed == 0)
				{
					if(lastFolder != null)
					{
						lastFolder = lastFolder.getParent();
						currentIndex.removeInnerLevel();
					}
					else
					{
						MajaApp.displayWarning("Too many backdir entries in CVP file. CVP is probably corrupted.");
					}
				}
				else
				{
					//Create and add the actual entry
					MajaSourceEntry mse = null;

					//Add level, regardless of file or folder, so we can store the proper index
					currentIndex.addLevel(f_name);

					//Handle files
					if(f_dataSizeUncompressed != 0)
					{
						boolean addSource = true;
						if(f_dataSizeUncompressed < 0)
						{
							MajaApp.displayWarning("Error: Entry '" + f_name + "' has an invalid uncompressed size (" + f_dataSizeUncompressed + ") and will be disconnected");
							addSource = false;
						}
						else if(f_dataPosition < CVP_HEADER_SIZE || (f_dataPosition >= f_tocLocation))
						{
							MajaApp.displayWarning("Error: Entry '" + f_name + "' has an invalid position (" + f_dataPosition + ") and be disconnected.");
							addSource = false;
						}
						else if((f_dataPosition + f_dataSizeCompressed) > f_tocLocation)
						{
							MajaApp.displayWarning("Error: Entry '" + f_name + "' claims to have a position and compressed size that would overwrite the TOC, and will be disconnected");
							addSource = false;
						}
						
						if(addSource)
						{
							int compressionType = (f_dataSizeCompressed == f_dataSizeUncompressed) ? MajaEntry.UNCOMPRESSED : MajaEntry.BZIP2;
							//me = new MajaEntry(p, f_name, MajaEntry.FILE, ms, currentIndex, compressionType, f_dataPosition, f_dataSizeUncompressed, (long)f_timeStamp * (long)1000);
							mse = new MajaSourceEntry(MajaEntry.FILE);
							mse.setIndex(currentIndex);
							mse.setCompression(compressionType);
							mse.setCompressedSize(f_dataSizeCompressed);
							mse.setLastModified((long)f_timeStamp * (long)1000);
							mse.setPosition(f_dataPosition);
							mse.setUncompressedSize(f_dataSizeUncompressed);
						}
						else
						{
							//me = new MajaEntry(p, f_name, MajaEntry.FILE);
						}
						
						//We don't have subfiles :P
						currentIndex.removeInnerLevel();
					}
					else
					{
						//Handle folders
						//me = new MajaEntry(p, f_name, MajaEntry.FOLDER);
						mse = new MajaSourceEntry(MajaEntry.FOLDER);
						mse.setIndex(currentIndex);
					}
					
					//If we are in a folder, add it there.
					if(mse != null)
					{
						if(lastFolder != null)
						{
							mse = lastFolder.addChild(mse);
							if(mse == null)
								MajaApp.displayError("Error adding entry to '" + lastFolder.getName() + "'");
						}
						else
						{
							mse = finalSource.addSourceEntry(mse);
						}
							
						//Set lastFolder
						if(mse.getType() == MajaEntry.FOLDER)
						{
							lastFolder = mse;
						}
		
						//Increment totalEntries
						totalEntries++;
					}
					MajaApp.updateCurrentStatus((float)totalEntries / (float)f_NumEntries, false);
				}
			}
			
			//Just a nice little anal-check
			if(lastFolder != null)
			{
				MajaApp.displayWarning("Note: Imported CVP lacked ending backdirs. This does not affect importing in any way.");
			}
			
			dis.close();
		}
		catch (MajaHandlerManager.OperationCancelledException e)
		{
			MajaApp.displayWarning("Cancelled import of '" + f.getPath() + "' on user command.");
		}
		catch (java.io.FileNotFoundException e)
		{
			MajaApp.displayException(e, "Could not find CVP file '" + f.getPath() + "'");
		}
		catch(java.io.IOException e)
		{
			MajaApp.displayException(e, "IO Exception while importing CVP '" + f.getPath() + "'");
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