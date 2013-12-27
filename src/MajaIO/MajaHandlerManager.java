package MajaIO;
import java.io.*;
import Maja.*;

public class MajaHandlerManager
{
	/**
	* Internal MajaHandler repository
	*/
	private static java.util.Vector<MajaHandler> _handlers = new java.util.Vector<MajaHandler>();
	
	/**
	* Default header length to pass to all package handlers
	*/
	public static final int DEFAULT_HEADER_LENGTH = 16;
	
	//***************-----METHODS-----***************//
	
	/**
	* Prevents instantiation
	*/
	private MajaHandlerManager(){}
	
	/**
	* Adds a package handler to the internal repository
	* 
	* @return				True if the package handler was successfully added.
	*/
	public static boolean addPackageHandler(MajaHandler h)
	{
		if(h == null)
			return false;

		if(_handlers.contains(h))
			return true;
			
		_handlers.add(h);
		return true;
	}
	
	/**
	* Returns the number of currently loaded package handlers.
	*
	* @return				Number of currently loaded package handlers
	*/
	public static int getNumPackageHandlers()
	{
		return _handlers.size();
	}
	
	/**
	* Returns a package handler from the internal repository by its index
	*
	* @return				Package handler, or null if an invalid index is specified
	*/
	public static MajaHandler getPackageHandler(int i)
	{
		if(i < 0 || i >= _handlers.size())
			return null;
			
		return _handlers.get(i);
	}
	
	/**
	* Reads the first 16 bytes of a file, and then returns the proper package handler.
	*/
	public static MajaHandler findPackageHandler(java.io.File f)
	{
		if(f == null)
			return null;
		
		MajaInputStream mis = null;
		byte[] b = new byte[DEFAULT_HEADER_LENGTH];
		try
		{
			mis = new MajaInputStream(f);
			if(mis.read(b, 0, 16) < DEFAULT_HEADER_LENGTH)
				return null;
		}
		catch(FileNotFoundException e)
		{
			return null;
		}
		catch(IOException e)
		{
			return null;
		}
		finally
		{
			if(mis != null) try {mis.close();} catch(IOException e){}
		}
		
		return MajaHandlerManager.findPackageHandler(b);
	}
	/**
	* Based on the first 16 bytes of a file, returns the package handler
	* @param	b			16-byte array or larger of the first 16 bytes of the file
	* @return				A MajaHandler for the file; null if none exists; null if less than 16 bytes is given.
	*/
	public static MajaHandler findPackageHandler(byte[] b)
	{
		if(b == null || b.length < DEFAULT_HEADER_LENGTH)
			return null;
		
		for(int i = 0; i < _handlers.size(); i++)
		{
			if(_handlers.get(i).isPackage(b))
				return _handlers.get(i);
		}
		
		return null;
	}
	
	/**
	* Imports a package using the proper package handler
	* 
	* @param f				The package file
	* @return				Handle to the imported MajaSource
	*/
	public static MajaSource importPackage(File f)
	{
		if(f == null)
			return null;
			
		MajaHandler handler = MajaHandlerManager.findPackageHandler(f);
		if(handler == null)
			return null;

		return handler.importPackage(f);
	}

	public static abstract class MajaHandler
	{
		/**
		* Returns the name of the package type. (eg "ZIP file")
		*
		* @return			The package type's name.
		*/
		public abstract String getName();
		
		/**
		* Returns a brief description of the package type. (eg "Widely used compressed archive format.")
		*
		* @return			The package type's description
		*/
		public abstract String getDescription();
		
		/**
		* Returns the default extension of the package type.
		*
		* @return			The package type's default extension
		*/
		public abstract String getExtension();
		
		/**
		* Retrieves an input stream with all necessary compression turned on.
		* Only used if _dataPosition of a MajaEntry is < -1, but a package source of this type is specified.
		*
		* @param mse		The MajaSourceEntry that the input stream should be for
		* @return			The input stream, or null if it is not supported or an error occurs
		*/
		public abstract MajaInputStream getInputStream(MajaSourceEntry mse);
		
		/**
		* Returns all reasons, if any, that the given MajaEntry may not be written to a package of this type.
		*
		* @param e			The MajaEntry that is being validated
		* @return			A aingle-line String specifying all of the errors, or null if there are none.
		*/
		public abstract String getValidationErrors(MajaEntry e);
		
		/**
		* Exports the given entry as a separate package (or the entire project, if no entry is specified).
		* The masterEntry should be the top-level folder or item of the exported file.
		*
		* @param f				The package file being imported
		* @return				The source for the imported package file
		*/
		public abstract MajaSource importPackage(File f);
		
		/**
		* Checks the first 16 bytes of the file to determine whether it is a valid package or not.
		*
		* @param b			The first 16 bytes of the file in question.
		* @return			True if this package handler can read the specified file, false if not.
		*/
		public abstract boolean isPackage(byte[] b);
		
		/**
		* Gets the status of the specified MajaSourceEntry
		*
		* @param mse		The MajaSourceEntry to check
		* @param n_sync		Whether to attempt to sync the entry or not
		* @return			Status
		*/
		public abstract int manageSourceEntryStatus(MajaSourceEntry mse, boolean n_sync);
		/**
		* Exports the given entry as a separate package (or the entire project, if no entry is specified).
		* The masterEntry should be the top-level folder or item of the exported file.
		*
		* @param p			The project that is being exported from.
		* @return			The number of entries in the final file, or 0 if export failed.
		*/
		public abstract int exportPackage(MajaEntry masterEntry, File f);
		
		/**
		* Runs the package handler's isPackage() function on a file.
		* 
		* @param f				The file to be checked
		* @return				true if the file is a valid package file for the handler, false if it isn't or if an error occurs.
		*/
		public boolean isPackage(File f)
		{
			if(f == null)
				return false;
			
			MajaInputStream mis = null;
			byte[] b = new byte[DEFAULT_HEADER_LENGTH];
			try
			{
				mis = new MajaInputStream(f);
				if(mis.read(b, 0, 16) < DEFAULT_HEADER_LENGTH)
					return false;
			}
			catch(FileNotFoundException e)
			{
				return false;
			}
			catch(IOException e)
			{
				return false;
			}
			finally
			{
				if(mis != null) try {mis.close();} catch(IOException e){}
			}
			
			return this.isPackage(b);
		}
	}
	
	/**
	* Indicates that the current operation was cancelled abruptly and intentionally.
	* (ie due to user input rather than an error)
	*/
	public static class OperationCancelledException extends Exception
	{
		private static final long serialVersionUID = 7581209703335352949L;

		public OperationCancelledException()
		{
			super();
		}
	}
}