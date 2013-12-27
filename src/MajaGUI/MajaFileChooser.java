package MajaGUI;
import javax.swing.*;
import Maja.*;
import MajaIO.*;

//***************-----CLASS: MajaFileChooser-----***************//
public class MajaFileChooser extends JFileChooser
{
	//public static final int EXPORT_DIRECTORY = 0;
	public static final int NONE = -1;
	public static final int EXPORT_ENTRY = 0;
	public static final int EXPORT_PACKAGE = 1;
	public static final int IMPORT_DIRECTORY = 2;
	public static final int IMPORT_FILE = 3;
	public static final int IMPORT_PACKAGE = 4;
	public static final int OUTPUT_PATH = 5;
	public static final int OPEN_FILE = 6;
	public static final int SAVE_AS_FILE = 7;
	
	public static final String _typeStrings[] = {
		"MajaFileChooser: Export Entry",
		"MajaFileChooser: Export Package",
		"MajaFileChooser: Import Directory",
		"MajaFileChooser: Import File",
		"MajaFileChooser: Import Package",
		"MajaFileChooser: Output Path",
		"MajaFileChooser: Open File",
		"MajaFileChooser: Save As File",
	};
	
	private int _currentType;
	
	public MajaFileChooser()
	{
		_currentType = NONE;
		this.resetChoosableFileFilters();
		this.setTitleDescription("Open File", "Open", "Open file");
	}
	
	public void addExtension(String n_ext, String n_desc)
	{
		javax.swing.filechooser.FileFilter ff = new ExtensionFileFilter(n_ext, n_desc);
		this.addChoosableFileFilter(ff);
	}
	
	public void setDefaultType(int n_type)
	{
		_currentType = n_type;
		this.resetChoosableFileFilters();
		switch(n_type)
		{
			case EXPORT_ENTRY:
				super.setDialogType(JFileChooser.SAVE_DIALOG);
				this.setTitleDescription("Export to directory", "Export Entry", "Exports selected entries into the specified directory");
				super.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				super.setMultiSelectionEnabled(false);
				break;
			
			case EXPORT_PACKAGE:
				super.setDialogType(JFileChooser.SAVE_DIALOG);
				this.setTitleDescription("Export Package", "Export Package", "Exports project to the specified package type");
				super.setFileSelectionMode(JFileChooser.FILES_ONLY);
				super.setMultiSelectionEnabled(true);
				super.setAcceptAllFileFilterUsed(false);
				for(int i = 0; i < MajaHandlerManager.getNumPackageHandlers(); i++)
				{
					super.addChoosableFileFilter(new PackageFileFilter(MajaHandlerManager.getPackageHandler(i)));
				}
				break;

			case IMPORT_DIRECTORY:
				super.setDialogType(JFileChooser.OPEN_DIALOG);
				this.setTitleDescription("Import Directory", "Import Directory", "Imports selected directory into the current project");
				super.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				super.setMultiSelectionEnabled(false);
				break;

			case IMPORT_FILE:
				super.setDialogType(JFileChooser.OPEN_DIALOG);
				this.setTitleDescription("Import file(s)", "Import", "Imports selected file into the current project");
				super.setFileSelectionMode(JFileChooser.FILES_ONLY);
				super.setMultiSelectionEnabled(true);
				break;
				
			case IMPORT_PACKAGE:
				super.setDialogType(JFileChooser.OPEN_DIALOG);
				this.setTitleDescription("Import Package(s)", "Import Package", "Imports specified package file into the project");
				super.setFileSelectionMode(JFileChooser.FILES_ONLY);
				super.setMultiSelectionEnabled(true);
				for(int i = 0; i < MajaHandlerManager.getNumPackageHandlers(); i++)
				{
					super.addChoosableFileFilter(new PackageFileFilter(MajaHandlerManager.getPackageHandler(i)));
				}
				super.addChoosableFileFilter(new PackageFileFilter());
				break;
				
			case OUTPUT_PATH:
				super.setDialogType(JFileChooser.SAVE_DIALOG);
				this.setTitleDescription("Set Output Path", "Set Output Path", "Sets current project's data storage output path");
				super.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				super.setMultiSelectionEnabled(false);
				break;
			
			case OPEN_FILE:
				super.setDialogType(JFileChooser.OPEN_DIALOG);
				this.setTitleDescription("Open file", "Open", "Opens selected file");
				super.setFileSelectionMode(JFileChooser.FILES_ONLY);
				super.setMultiSelectionEnabled(false);
				break;
			
			case SAVE_AS_FILE:
				super.setDialogType(JFileChooser.SAVE_DIALOG);
				this.setTitleDescription("Save As File", "Save", "Saves current file");
				super.setFileSelectionMode(JFileChooser.FILES_ONLY);
				super.setMultiSelectionEnabled(false);
				break;
		}
		
		if(n_type < _typeStrings.length)
		{
			String newPath = java.util.prefs.Preferences.userNodeForPackage(MajaFileChooser.class).get(_typeStrings[n_type], null);
			if(newPath != null)
				super.setCurrentDirectory(new java.io.File(newPath));
		}
	}
	
	public void setTitleDescription(String title, String buttonText, String buttonToolTipText)
	{
		this.setDialogTitle(title);
		this.setApproveButtonText(buttonText);
		this.setApproveButtonToolTipText(buttonToolTipText);
	}
	
	public int showDialog(java.awt.Component c)
	{
		if(_currentType == NONE)
		{
			MajaApp.displayError("Attempt to show an uninitialized MajaFileChooser");
			return JFileChooser.ERROR_OPTION;
		}

		int retval = super.showDialog(c, super.getApproveButtonText());
		if(_currentType < _typeStrings.length && _currentType != NONE)
		{
			java.util.prefs.Preferences.userNodeForPackage(MajaFileChooser.class).put(_typeStrings[_currentType], super.getCurrentDirectory().getPath());
		}
		return retval;
	}
	
	//***************-----SUBCLASS: ExtensionFileFilter-----***************//
	public static class ExtensionFileFilter extends javax.swing.filechooser.FileFilter
	{
		String _ext;
		String _desc;
		public ExtensionFileFilter(String n_ext, String n_desc)
		{
			_ext = n_ext.toLowerCase();
			_desc = new String(n_desc);
			
			if(_ext == null)
			{
				_ext = "*";
				_desc = "All Files";
			}
		}
		public boolean accept(java.io.File f)
		{
			if(f.isDirectory())
				return true;
			
			String extension = null;
			String filename = f.getName();
			int lastIndex = filename.lastIndexOf('.')+1;
			if(lastIndex < filename.length() && lastIndex > 0)
				extension = filename.substring(lastIndex).toLowerCase();
	
			return extension.equals(_ext);
		}
	
		public String getDescription()
		{
			return _desc;
		}
	}
	
	//***************-----SUBCLASS: PackageFileFilter-----***************//
	public static class PackageFileFilter extends javax.swing.filechooser.FileFilter
	{
		MajaHandlerManager.MajaHandler _handler;
		public PackageFileFilter()
		{
			_handler = null;
		}
		public PackageFileFilter(MajaHandlerManager.MajaHandler n_handler)
		{
			_handler = n_handler;
		}
		public boolean accept(java.io.File f)
		{
			if(f.isDirectory())
				return true;
	
			if(_handler != null)
				return _handler.isPackage(f);
			else
				return (MajaHandlerManager.findPackageHandler(f) != null);
		}
	
		public String getDescription()
		{
			if(_handler != null)
				return _handler.getName();
			else
				return "Supported Package files";
		}
		
		public MajaHandlerManager.MajaHandler getHandler()
		{
			return _handler;
		}
	}
}