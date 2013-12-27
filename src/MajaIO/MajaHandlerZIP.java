package MajaIO;
import java.io.*;
import java.util.zip.*;
import Maja.*;

public class MajaHandlerZIP extends MajaHandlerManager.MajaHandler
{
	//********************Singleton support
	public static MajaHandlerZIP _currentInstance = null;
	private MajaHandlerZIP(){}
	public static MajaHandlerZIP initialize()
	{
		if(_currentInstance != null)
			return _currentInstance;
		
		_currentInstance = new MajaHandlerZIP();
		return _currentInstance;
	}
	
	//********************Methods
	public String getName()
	{
		return "ZIP File";
	}
	public String getDescription()
	{
		return "Common zip file";
	}
	public String getExtension()
	{
		return "zip";
	}
	
	public MajaInputStream getInputStream(MajaSourceEntry mse)
	{
		if(mse == null)
			return null;
		
		File f = mse.getSource().getFileHandle();
		
		if(f == null)
			return null;
		
		ZipInputStream zis = null;
			
		try
		{
			zis = new ZipInputStream(new MajaInputStream(f));
			ZipEntry currentEntry = null;
			while((currentEntry = zis.getNextEntry()) != null)
			{
				String f_name = currentEntry.getName();
				
				//Eliminate trailing slash on directories
				if(f_name.lastIndexOf('/') == f_name.length()-1)
					f_name = f_name.substring(0, f_name.length()-1);
				
				if(mse.getIndex().toString().equals(f_name))
				{
					return new MajaInputStream(zis);
				}
			}
		}
		catch (java.io.FileNotFoundException exc)
		{
			MajaApp.displayException(exc, "Could not find ZIP file '" + f.getPath() + "'");
		}
		catch(java.io.IOException exc)
		{
			MajaApp.displayException(exc, "IO Exception while importing ZIP '" + f.getPath() + "'");
		}
		
		//Close file
		if(zis != null) try{zis.close();}catch(java.io.IOException exc){MajaApp.displayError("Emergency close error");}
		
		return null;
	}
	
	protected int getMajaCompressionEnum(ZipEntry ze)
	{
		switch(ze.getMethod())
		{
			case ZipEntry.DEFLATED:
				return MajaEntry.GZIP;
			case ZipEntry.STORED:
				return MajaEntry.UNCOMPRESSED;
			default:
				return MajaEntry.UNKNOWN;
		}
	}
	
	public int manageSourceEntryStatus(MajaSourceEntry mse, boolean n_sync)
	{
		if(mse == null)
			return MajaEntry.DISCONNECTED;
		
		File f = mse.getSource().getFileHandle();
		
		if(f == null)
			return MajaEntry.DISCONNECTED;
		
		ZipInputStream zis = null;
			
		try
		{
			zis = new ZipInputStream(new MajaInputStream(f));
			ZipEntry currentEntry = null;
			while((currentEntry = zis.getNextEntry()) != null)
			{
				String f_name = currentEntry.getName();
				
				//Eliminate trailing slash on directories
				if(f_name.lastIndexOf('/') == f_name.length()-1)
					f_name = f_name.substring(0, f_name.length()-1);
				
				if(mse.getIndex().toString().equals(f_name))
				{
					int compressionType = this.getMajaCompressionEnum(currentEntry);
					if(n_sync)
					{
						long f_dataSizeUncompressed = currentEntry.getSize();
						long f_dataSizeCompressed = currentEntry.getCompressedSize();
						f_name = currentEntry.getName();
						long f_timeStamp = currentEntry.getTime();
						mse.setCompressedSize(f_dataSizeCompressed);
						mse.setCompression(compressionType);
						mse.setLastModified((long)f_timeStamp * (long)1000);
						mse.setUncompressedSize(f_dataSizeUncompressed);
					}
					
					long timeStamp = (long)currentEntry.getTime() * (long)1000;
					if(currentEntry.getSize() == mse.getUncompressedSize()
							&& currentEntry.getCompressedSize() == mse.getCompressedSize()
							&& timeStamp == mse.getLastModified()
							&& compressionType == mse.getCompression())
						return MajaEntry.SYNCED;
					return MajaEntry.CONNECTED;
				}
			}
		}
		catch (java.io.FileNotFoundException exc)
		{
			MajaApp.displayException(exc, "Could not find ZIP file '" + f.getPath() + "'");
		}
		catch(java.io.IOException exc)
		{
			MajaApp.displayException(exc, "IO Exception while importing ZIP '" + f.getPath() + "'");
		}
		
		//Close file
		if(zis != null) try{zis.close();}catch(java.io.IOException exc){MajaApp.displayError("Emergency close error");}
		
		return MajaEntry.DISCONNECTED;
	}
	
	/**
	* Given a MajaEntry, returns all errors that would make it unusable on export to a ZIP file.
	* @return String of errors if there are any errors, null otherwise.
	*/
	public String getValidationErrors(MajaEntry e)
	{
		String returnString = "";
		String eName = e.getName();
		int eNameLength = eName.length();
		int eType = e.getType();
		//long eSize = e.getSize();
		
		//Type
		if(eType != MajaEntry.FILE && eType != MajaEntry.FOLDER)
		{
			returnString += "ZIP format only supports files and folders";
		}

		//Name
		if(eNameLength < 1)
		{
			returnString += "Entry must have a name. ";
		}
		if(eName.indexOf('/') > -1)
		{
			returnString += "Entry uses reserved character '/'. ";
		}
		
		if(returnString.length() > 0)
			return returnString;
		
		//No errors
		return null;
	}
	
	public boolean isPackage(byte[] b)
	{
		int i = 0;
		i += (b[0] & 0x000000FF) << 24;
		i += (b[1] & 0x000000FF) << 16;
		i += (b[2] & 0x000000FF) << 8;
		i += (b[3] & 0x000000FF) << 0;
		if(Integer.reverseBytes(i) == 0x04034b50)
			return true;
	
		return false;
	}
	
	/**
	* Exports a MajaEntry and all sub-MajaEntries to a ZipOutputStream recursively.
	*
	* @param zos			Output stream of zip file
	* @param masterEntry	Master entry that is being exported from (or null if all toplevel project entries)
	* @param e				MajaEntry that is being exported
	* @return				Number of exported entries
	*/
	public int exportEntryToZIP(ZipOutputStream zos, MajaEntry masterEntry, MajaEntry me) throws java.io.IOException, MajaIO.MajaHandlerManager.OperationCancelledException
	{
		if(zos == null || me == null)
			return 0;

		if(me.getStatus() != MajaEntry.SYNCED)
			return 0;
			
		MajaSourceEntry mse = me.getSourceEntry();
		
		if(mse == null)
			return 0;
		
		String validationErrors = this.getValidationErrors(me);
		if(validationErrors != null)
		{
			MajaApp.displayWarning("Error writing '" + me.getName() + "': " + validationErrors);
			return 0;
		}
		
		int totalExportedEntries = 0;
		
		//*****Determine the name
		String outName = "";
		MajaEntry ce = me;
		java.util.Vector<MajaEntry> path = new java.util.Vector<MajaEntry>();
		do
		{
			//MajaApp.displayStatus("Pathing '" + ce.getName() + "'");
			//Don't add head.
			if(ce.getType() == MajaEntry.HEAD)
				break;
			path.add(ce);
			if(ce == masterEntry)
				break;
			ce = ce.getParent();
		} while(ce != null);
		MajaIndex finalIndex = new MajaIndex("");
		for(int j = path.size()-1; j > -1; j--)
		{
			//MajaApp.displayStatus("Adding '" + path.get(j).getName());
			finalIndex.addLevel(path.get(j).getName());
		}
		
		outName = finalIndex.toString();
		
		if(me.getType() == MajaEntry.FOLDER)
			outName += "/";
			
		//*****Create the entry and write the data
		ZipEntry ze = new ZipEntry(outName);
		ze.setTime(mse.getLastModified());
		if(me.getType() == MajaEntry.FOLDER)
		{
			ze.setCompressedSize(0);
			ze.setCrc(0);
			ze.setSize(0);
			ze.setMethod(ZipEntry.STORED);
		}
		else if(mse.getUncompressedSize() == 0)
		{
			ze.setCompressedSize(0);
			ze.setCrc(0);
			ze.setSize(0);
			ze.setMethod(ZipEntry.STORED);
		}
		else
		{
			if(mse.getUncompressedSize() > -1)
				ze.setSize(mse.getUncompressedSize());
			
			switch(me.getCompression())
			{
				case MajaEntry.DEFAULT:
				case MajaEntry.GZIP:
					ze.setMethod(ZipEntry.DEFLATED);
					break;
				
				case MajaEntry.UNCOMPRESSED:
				default:
					ze.setMethod(ZipEntry.STORED);
			}
		}
		
		//Write it
		//MajaApp.displayStatus("Writing '" + outName + "'");
		zos.putNextEntry(ze);
		if(me.getType() == MajaEntry.FILE)
		{
			if(me.export(new MajaOutputStream(zos), Long.MAX_VALUE) == null)
			{
				MajaApp.displayError("Failed to export '" + outName + "'.");
			}
			/*
			MajaSource s = me.getSource();
			if(s != null)
				s.exportEntry(new MajaOutputStream(zos), me, Long.MAX_VALUE);
			else
				MajaApp.displayWarning("Could not output file data for '" + outName + "' because it does not have a source.");
			*/
		}
		zos.closeEntry();
		
		totalExportedEntries++;
		
		//Check statusbar
		if(!MajaApp.updateCurrentStatus(-1.0f, true))
			throw new MajaHandlerManager.OperationCancelledException();
		
		for(int i = 0; i < me.getNumChildren(); i++)
		{
			totalExportedEntries += this.exportEntryToZIP(zos, masterEntry, me.getChild(i));
		}
		
		return totalExportedEntries;
	}
	
	public int exportPackage(MajaEntry masterEntry, File f)
	{
		//*****Do we have an outputpath?
		if(f == null)
		{
			MajaApp.displayError("No export location specified for ZIP export");
			return 0;
		}
		
		//*****Open the file
		ZipOutputStream zos = null;
		boolean fileOpened = false;			//Used to determine if we can delete the file on exception
		boolean fileShouldBeDeleted = false;	//Used to determine whether we should delete the file after exception
		try
		{
			MajaApp.displayStatus("=====Exporting ZIP: " + f.getPath());
			MajaApp.setCurrentStatus("Exporting ZIP '" + f.getPath() + "'...", 0.0f, true);
			zos = new ZipOutputStream(new MajaOutputStream(f));
			fileOpened = true;

			//*****Create a temp entries array for the items we'll be writing
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
			
			//*****Write the header
			final int outputMethod = ZipOutputStream.DEFLATED;
			final int outputLevel = 9;
			zos.setMethod(outputMethod);
			if(outputMethod == ZipOutputStream.DEFLATED)
				MajaApp.displayStatus("METHOD: DEFLATED");
			else if(outputMethod == ZipOutputStream.STORED)
				MajaApp.displayStatus("METHOD: STORED");
			zos.setLevel(outputLevel);
			MajaApp.displayStatus("LEVEL: " + outputLevel);
			MajaApp.displayStatus("ROOTNUM: " + entriesToWrite.size());
			
			//*****Copy entries
			int totalExportedEntries = 0;
			for(int i = 0; i < entriesToWrite.size(); i++)
			{
				MajaEntry e = entriesToWrite.get(i);
				
				totalExportedEntries += exportEntryToZIP(zos, masterEntry, e);
			}
			
			zos.close();
			
			//*****WE'RE DONE!
			return totalExportedEntries;
		}
		catch(java.io.FileNotFoundException e)
		{
			MajaApp.displayException(e, "Could not create or open file for exporting package '" + f.getPath() + "'");
		}
		catch(MajaHandlerManager.OperationCancelledException ex)
		{
			MajaApp.displayWarning("Cancelled export of '" + f.getPath() + "' on user command.");
			fileShouldBeDeleted = true;
		}
		catch(SecurityException e)
		{
			MajaApp.displayException(e, "Access denied to write to file '" + f.getPath() + "' when exporting ZIP");
			fileShouldBeDeleted = true;
		}
		catch(java.io.IOException e)
		{
			MajaApp.displayException(e, "IO exception for file '" + f.getPath() + "' when exporting ZIP");
			fileShouldBeDeleted = true;
		}
		finally
		{
			try
			{
				if(zos != null) zos.close();
				if(fileShouldBeDeleted && fileOpened && !f.delete())
					MajaApp.displayWarning("Could not delete partially-exported ZIP file '" + f.getPath() + "'. This file is almost certainly corrupt.");
			}
			catch(java.io.IOException e)
			{
				MajaApp.displayError("Unable to close export handle for '" + f.getPath() + "'");
			}
			MajaApp.resetCurrentStatus();
		}
		
		return 0;
	}

	public MajaSource importPackage(File f)
	{
		if(f == null)
			return null;
		
		MajaInputStream mis = null;
		MajaSource finalSource = null;
		int totalEntries = 0;
		try
		{
			MajaApp.displayStatus("=====Importing ZIP: " + f.getPath());
			mis = new MajaInputStream(f);
			//long zip_total_size = f.length();
			ZipInputStream zis = new ZipInputStream(mis);

			//Create source
			finalSource = new MajaSource(f);
			String f_name, realName, realPath;
			long f_dataSizeUncompressed, f_dataSizeCompressed, f_timeStamp;
			int compressionType;

			//Set lastFolder up
			//MajaSourceEntry currentParent = finalSource.getSourceEntryHead();
			//MajaIndex currentIndex = new MajaIndex("");
			ZipEntry currentEntry = null;
			while((currentEntry = zis.getNextEntry()) != null)
			{
				//Read in the data
				compressionType = this.getMajaCompressionEnum(currentEntry);
				f_dataSizeUncompressed = currentEntry.getSize();
				f_dataSizeCompressed = currentEntry.getCompressedSize();
				f_name = currentEntry.getName();
				f_timeStamp = currentEntry.getTime();
				
				//Eliminate trailing slash on directories
				if(f_name.lastIndexOf('/') == f_name.length()-1)
					f_name = f_name.substring(0, f_name.length()-1);
				
				/*
				int lastSlash = f_name.lastIndexOf('/');
				if(lastSlash > -1)
					f_name = f_name.substring(lastSlash+1);
				*/

				if(f_name.length() < 1)
				{
					MajaApp.displayError("Error while importing ZIP - Empty entry detected in ZIP file. ZIP may be corrupted. (Entry was skipped)");
				}
				else
				{
					//Create and add the actual entry
					MajaSourceEntry mse = null;
					
					/*String cis = currentIndex.toString();
					while(cis.length() > f_name.length() || cis.length() > 0 && !cis.equals(f_name.substring(0, cis.length())))
					{
						if(currentParent != finalSource.getSourceEntryHead())
						{
							//MajaApp.displayStatus(cis);
							currentIndex.removeInnerLevel();
							cis = currentIndex.toString();
							currentParent = currentParent.getParent();
						}
						else
						{
							break;
						}
					}*/
					
					//Figure out real name
					int lastSlash = f_name.lastIndexOf('/');
					if(lastSlash > -1)
					{
						realName = f_name.substring(lastSlash+1);
						realPath = f_name.substring(0, lastSlash);
					}
					else
					{
						realName = new String(f_name);
						realPath = null;
					}
					
					MajaSourceEntry currentPath = null;
					if(realPath == null && !currentEntry.isDirectory())
						currentPath = finalSource.getSourceEntryHead();
					else
					{
						MajaIndex mi = new MajaIndex(realPath);
						if(currentEntry.isDirectory())
							mi.addLevel(realName);
						currentPath = finalSource.getSourceEntryHead().createPath(mi);
					}

					//Add level, regardless of file or folder, so we can store the proper index
					//currentIndex.addLevel(realName);

					//Handle type
					int type = MajaEntry.FILE;
					if(currentEntry.isDirectory())
					{
						type = MajaEntry.FOLDER;
						mse = currentPath;
					}
					else
					{
						mse = new MajaSourceEntry(MajaEntry.FILE);
						currentPath.addChild(mse);
					}
					
					//Handle index
					MajaIndex mi = new MajaIndex(realPath);
					mi.addLevel(realName);
					
					//Input it
					mse.setName(realName);
					mse.setCompressedSize(f_dataSizeCompressed);
					mse.setCompression(compressionType);
					mse.setIndex(mi);
					mse.setLastModified((long)f_timeStamp * (long)1000);
					mse.setUncompressedSize(f_dataSizeUncompressed);
	
					//Increment totalEntries
					totalEntries++;
				}
			}

			mis.close();
		}
		catch (java.io.FileNotFoundException e)
		{
			MajaApp.displayException(e, "Could not find ZIP file '" + f.getPath() + "'");
		}
		catch(java.io.IOException e)
		{
			MajaApp.displayException(e, "IO Exception while importing ZIP '" + f.getPath() + "'");
		}
		finally
		{
			//Close file
			if(mis != null) try{mis.close();}catch(java.io.IOException e){MajaApp.displayError("Emergency close error");}
			//Remove source (if it hasn't been added to) and return
			if(finalSource != null && finalSource.getNumSourceEntries() < 1)
			{
				finalSource = null;
			}
		}
		
		//For testing disconnected behavior
		//ms.setPath("CANDYLAND", MajaSource.FILE);

		return finalSource;
	}
}