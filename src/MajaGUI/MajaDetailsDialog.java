package MajaGUI;
import javax.swing.*;
import Maja.*;

public class MajaDetailsDialog
{
	/**
	* Internal pointers to the object that is the focus of the dialog.
	* Only one should be non-null.
	*/
	private MajaEntry _majaEntry = null;
	private MajaProject _majaProject = null;
	private MajaSource _majaSource = null;

	/**
	* Internal string buffer, displayed with show()
	*/
	private String _message = "(Null)";
	
	/**
	* Buffers and formats a human-readable string about a MajaEntry
	* 
	* @param	n_majaEntry		The entry that the dialog is to be displayed about.
	*/
	MajaDetailsDialog(MajaEntry n_majaEntry)
	{
		_majaEntry = n_majaEntry;
		if(_majaEntry == null)
			return;
		
		
		_message = _majaEntry.getName();
		_message += "\n\n";
		switch(_majaEntry.getType())
		{
			case MajaEntry.INVALID:
				_message += "\nType:\n     Invalid";
				break;
			case MajaEntry.FILE:
				_message += "\nType:\n     File";
				break;
			case MajaEntry.FOLDER:
				_message += "\nType:\n     Folder";
				break;
			case MajaEntry.HEAD:
				_message += "\nType:\n     Head";
				break;
			default:
				_message += "\nType:\n     Unknown";
				break;
		}
		if(_majaEntry.getProject() != null)
			_message += "\nProject:\n     " + _majaEntry.getProject().getName();
	
		if(_majaEntry.getParent() != null)
			_message += "\nParent:\n     " + _majaEntry.getParent().getName();
			
		_message += "\n\n";
		_message += "\nStatus:\n     " + _majaEntry.getStatusString();
		//_message += "\nConnected:\n     " + (_majaEntry.isConnected() ? "YES" : "NO");
		//_message += "\nSynced:\n     " + (_majaEntry.isSynced() ? "YES" : "NO");
	
		_message += "\n\n";
		if(_majaEntry.getSourceEntry() != null)
		{
			MajaSourceEntry mse = _majaEntry.getSourceEntry();
			MajaSource ms = mse.getSource();
			if(ms != null)
			{
				_message += "\nSource:\n     " + ms.getPath();
				_message += "\nSource type:\n     " + ms.getTypeString();
			}
			_message += "\nData Type:\n     " + mse.getTypeString();
			_message += "\nData Compression:\n     " + mse.getCompressionString();
			_message += "\nData Index:\n     " + mse.getIndex().toString();
			_message += "\nData Last Modified:\n     " + new java.util.Date(mse.getLastModified()).toString();
			_message += "\nData Position:\n     " + mse.getPosition();
			_message += "\nData Compressed Size:\n     " + this.parseSize(mse.getCompressedSize());
			_message += "\nData Uncompressed size:\n     " + this.parseSize(mse.getUncompressedSize());
			
			//_message += "\nData status:\n     " + mse.getStatusString();
			//_message += "\nData Connected:\n     " + (mse.isConnected() ? "YES" : "NO");
			//_message += "\nData Synced:\n     " + (mse.isSynced() ? "YES" : "NO");
			_message += "\n\n";
		}

		_message += "\nSubentries:\n     " + _majaEntry.getNumChildren();
	}
	
	/**
	* Buffers and formats a human-readable string about a MajaProject
	* 
	* @param	n_majaProject	The project that the dialog is to be displayed about.
	*/
	MajaDetailsDialog(MajaProject n_majaProject)
	{
		_majaProject = n_majaProject;
		if(_majaProject == null)
			return;
		
		_message = _majaProject.getName();
		_message += "\n\n";

		if(_majaProject.getOutputPath() != null)
			_message += "\nOutput Path:\n     " + _majaProject.getOutputPath().getPath();
		else
			_message += "\nOutput Path:\n     (Unspecified)";

		long projectedSyncedSize = 0;
		long projectedTotalSize = 0;
		int num = _majaProject.getNumEntries(true);
		for(int i = 0; i < num; i++)
		{
			projectedSyncedSize += _majaProject.getEntry(i).getTotalSize(false);
		}
		for(int i = 0; i < num; i++)
		{
			projectedTotalSize += _majaProject.getEntry(i).getTotalSize(true);
		}
		_message += "\n\n";
		_message += "\nSynced Entries:\n     " + _majaProject.getNumEntries(false) + " (" + this.parseSize(projectedSyncedSize) + ")";
		_message += "\nTotal Entries:\n     " + _majaProject.getNumEntries(true) + " (" + this.parseSize(projectedTotalSize) + ")";
		_message += "\nSources:\n     " + _majaProject.getNumSources();
	}
	
	/**
	* Buffers and formats a human-readable string about a MajaSource
	* 
	* @param	n_majaSource	The source that the dialog is to be displayed about.
	*/
	MajaDetailsDialog(MajaSource n_majaSource)
	{
		_majaSource = n_majaSource;
		if(_majaSource == null)
			return;
			
		_message = _majaSource.getPath();
		
		_message += "\n\n";
		_message += "\nType:\n     " + _majaSource.getTypeString();
		_message += "\nProject:\n     " + _majaSource.getProject().getName();

		_message += "\n\n";
		_message += "\nSize:\n     " + _majaSource.getSize();
		//_message += "\nLast Modified:\n     " + new java.util.Date(_majaSource.getLastModified()).toString();
		
		_message += "\n\n";		
		_message += "\nDependant entries:\n     " + _majaSource.getNumDependantEntries();
		_message += "\nSubsources:\n     " + _majaSource.getNumChildren();
	}
	
	/**
	* Parses any long integer into a string of type "Integer B/KB/MB/GB"
	* 
	* @param	sizeInBytes		The size to be parsed
	* @return					Formatted size string
	*/
	public String parseSize(long sizeInBytes)
	{
		if(Math.abs(sizeInBytes) > 1024*1024*1024)
		{
			long sizeInGigabytes = (sizeInBytes/(1024*1024*1024));
			return new String(sizeInGigabytes + " GB");
		}
		else if(Math.abs(sizeInBytes) > 1024*1024)
		{
			long sizeInMegabytes = (sizeInBytes/(1024*1024));
			return new String(sizeInMegabytes + " MB");
		}
		else if(Math.abs(sizeInBytes) > 1024)
		{
			long sizeInKilobytes = (sizeInBytes/1024);
			return new String(sizeInKilobytes + " KB");
		}
		else
		{
			return new String(sizeInBytes + " B");
		}
	}
	
	/**
	* Shows the details dialog box using the stored string and type.
	* 
	* @param	majaFrame		Parent frame of the dialog box
	*/
	public void show(MajaFrame majaFrame)
	{
		String name = "(null)";
		if(_majaEntry != null)
			name = "Entry '" + _majaEntry.getName() + "'";
		else if(_majaProject != null)
			name = "Project '" + _majaProject.getName() + "'";
		else if(_majaSource != null)
			name = "Source '" + _majaSource.getPath() + "'";
			
		JOptionPane.showMessageDialog(majaFrame, _message, name + " details", JOptionPane.INFORMATION_MESSAGE);
	}
}