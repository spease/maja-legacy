package MajaGUI;
import javax.swing.*;
import java.awt.event.*;
import Maja.*;
import MajaIO.*;

public class MajaFrame extends JFrame implements MajaApp.MajaOutputListener, java.awt.event.WindowListener
{
	private static final long serialVersionUID = 4953229877386973668L;
	MajaMainMenuBar _menuBar;
	MajaProjectTabs _projectTabs;
	MajaStatusBar _statusBar;
	
	//FileChoosers
	private MajaFileChooser _importChooser = null;
	private MajaFileChooser _exportChooser = null;
	private MajaFileChooser _outputChooser = null;

	public MajaFrame(String title)
	{
		super(title);
		this.setSize(800, 600);
		
		//Create a default project to work with
		MajaApp.addMajaOutputListener(this);

		//*****Create components
		//Add menu bar
		_menuBar = new MajaMainMenuBar(this);
		this.setJMenuBar(_menuBar);
		
		//Add main projects panel
		_projectTabs = new MajaProjectTabs(this);
		this.add(_projectTabs);
		
		//Add status bar
		_statusBar = new MajaStatusBar(16);
		this.getContentPane().add(_statusBar, java.awt.BorderLayout.SOUTH);
		
		//*****Setup initial variables
		_importChooser = new MajaFileChooser();
		_exportChooser = new MajaFileChooser();
		_outputChooser = new MajaFileChooser();
		
		//*****Handle closing the window
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(this);
		
		//Finish up
		this.setVisible(true);
	}
	
	public void closeProject(MajaProject p)
	{
		if(JOptionPane.showConfirmDialog(this, "Are you sure you want to close '" + p.getName() + "'?", "Close project", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			MajaApp.removeProject(p);
	}
	
	public void createFile(MajaProject p, MajaEntry e)
	{
		String fileName = JOptionPane.showInputDialog(this, "Enter file name:", "Create Empty File", JOptionPane.PLAIN_MESSAGE);
		if(fileName != null && fileName.length() > 0)
		{
			MajaEntry me = new MajaEntry(MajaEntry.FILE);
			me.setName(fileName);
			if(e != null)
				e.addChild(me);
			else
				p.addEntry(me);
			
			java.io.File f = me.createOutputPath();
			if(f != null)
			{
				me.setSourceEntry(p.addSource(new MajaSource(f)).getSourceEntryHead());
			}
		}
	}
	
	public void createFolder(MajaProject p, MajaEntry e)
	{
		String folderName = JOptionPane.showInputDialog(this, "Enter folder name:", "Create New Folder", JOptionPane.PLAIN_MESSAGE);
		if(folderName != null && folderName.length() > 0)
		{
			MajaEntry me = new MajaEntry(MajaEntry.FOLDER);
			me.setName(folderName);
			if(e != null)
				e.addChild(me);
			else
				p.addEntry(me);
		}
	}
	
	public void exportEntry(final MajaProject p, final MajaEntry e)
	{
		_exportChooser.setDefaultType(MajaFileChooser.EXPORT_ENTRY);
		int returnValue = _exportChooser.showDialog(this);
		
		if(returnValue == MajaFileChooser.APPROVE_OPTION)
		{
			final java.io.File f = _exportChooser.getSelectedFile();
			Thread t = new Thread("Entry '" + e.getName() + "' export")
			{
				public void run()
				{
					p.exportEntry(f, e);
				}
			};
			t.start();
		}
	}
	
	public void exportPackage(MajaProject p, MajaEntry e)
	{
		if(p == null)
			return;
		
		_exportChooser.setDefaultType(MajaFileChooser.EXPORT_PACKAGE);
		int returnValue = _exportChooser.showDialog(this);
		
		if(returnValue == MajaFileChooser.APPROVE_OPTION)
		{
			MajaFileChooser.PackageFileFilter pc = (MajaFileChooser.PackageFileFilter)_exportChooser.getFileFilter();
			final MajaHandlerManager.MajaHandler mph = pc.getHandler();
			
			java.io.File currentFile = _exportChooser.getSelectedFile();
			if(mph.getExtension() != null && !currentFile.getName().endsWith("." + mph.getExtension()))
			{
				final String options[] = {
					"Add default extension",
					"Save with current filename",
					"Cancel",
				};
				
				int res = JOptionPane.showOptionDialog(
					this,
					"Export filename does not end with the default extension for this package type (\"" + mph.getExtension() + "\")."
						+ "\n\nThis may prevent other programs, including Maja, from recognizing the package.",
					"Exporting '" + p.getName() + "'",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[0]);
				if(res == JOptionPane.YES_OPTION)
					currentFile = new java.io.File(currentFile.getPath() + "." + mph.getExtension());
				else if(res == JOptionPane.NO_OPTION)
					{}
				else if(res == JOptionPane.CANCEL_OPTION)
					return;
			}
			
			final java.io.File f = currentFile;
			if(e == null)
				e = p.getEntryHead();
			final MajaEntry masterEntry = e;
			Thread t = new Thread("Project '" + p.getName() + "' export")
			{
				public void run()
				{
					mph.exportPackage(masterEntry, f);
				}
			};
			t.start();
		}
	}
	
	public void importDirectory(MajaProject p, MajaEntry e)
	{
		_importChooser.setDefaultType(MajaFileChooser.IMPORT_DIRECTORY);
		int returnValue = _importChooser.showDialog(this);

		if(returnValue == MajaFileChooser.APPROVE_OPTION)
		{
			p.importDirectory(e, _importChooser.getSelectedFile());
		}
	}
	
	public void importFile(final MajaProject p, final MajaEntry e)
	{
		_importChooser.setDefaultType(MajaFileChooser.IMPORT_FILE);
		int returnValue = _importChooser.showDialog(this);

		if(returnValue == MajaFileChooser.APPROVE_OPTION)
		{
			final java.io.File[] files = _importChooser.getSelectedFiles();
			if(files != null && files.length > 0)
			{
				Thread t = new Thread("File(s) '" + files[0].getName() + "' import")
				{
					public void run()
					{
						for(int i = 0; i < files.length; i++)
						{
							p.importFile(e, files[i]);
						}
					}
				};
				t.start();
			}
		}
	}

	public void importPackage(final MajaProject p, final MajaEntry parentEntry)
	{
		_importChooser.setDefaultType(MajaFileChooser.IMPORT_PACKAGE);
		int returnValue = _importChooser.showDialog(this);

		if(returnValue == MajaFileChooser.APPROVE_OPTION)
		{
			final java.io.File[] files = _importChooser.getSelectedFiles();
			Thread t = new Thread("Package(s) '" + files[0].getName() + "' import")
			{
				public void run()
				{
					for(int i = 0; i < files.length; i++)
					{
						if(p.importPackage(parentEntry, files[i]) == null)
						{
							MajaApp.displayError("Unsupported package type for file '" + files[i].getPath() + "'");
						}
					}
				}
			};
			t.start();
		}
	}
	
	public void quitMaja()
	{
		if(JOptionPane.showConfirmDialog(this, "Are you sure you want to quit?", "Quit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			System.exit(0);
	}
	
	public void removeEntry(MajaEntry e)
	{
		int rv = JOptionPane.showConfirmDialog(this,
								"Are you sure you want to remove '" + e.getName() + "'?",
								"Entry removal",
								JOptionPane.YES_NO_OPTION);
		if(rv == JOptionPane.YES_OPTION)
		{
			e.getProject().removeEntry(e);
		}
	}
	
	public void renameEntry(MajaEntry e)
	{
		Object newName = JOptionPane.showInputDialog(this, "Enter new name:", "Rename entry", JOptionPane.PLAIN_MESSAGE, null, null, e.getName());
		if(newName != null)
		{
			if(newName instanceof String)
			{
				e.setName((String)newName);
			}
		}
	}
	
	public void renameProject(MajaProject p)
	{
		Object newName = JOptionPane.showInputDialog(this, "Enter new name:", "Rename project", JOptionPane.PLAIN_MESSAGE, null, null, p.getName());
		if(newName != null)
		{
			if(newName instanceof String)
			{
				p.setName((String)newName);
			}
		}
	}
	
	public void setOutputPath(MajaProject p)
	{
		_outputChooser.setDefaultType(MajaFileChooser.OUTPUT_PATH);
		java.io.File currentOutputPath = p.getOutputPath();
		if(currentOutputPath != null)
			_outputChooser.setCurrentDirectory(currentOutputPath);
		int returnValue = _outputChooser.showDialog(this);

		if(returnValue == MajaFileChooser.APPROVE_OPTION)
		{
			p.setOutputPath(_outputChooser.getSelectedFile());
		}
	}
	
	public void showAboutDialog()
	{
		JOptionPane.showMessageDialog(this, "Maja Express Beta v0.86"
			+ "\n\nDesigned and coded by WMCoolmon",
			"About Maja",
			JOptionPane.INFORMATION_MESSAGE);
	}

	//***************-----INTERFACE: MajaListener-----***************//
	public void displayException(Exception e, String n_message)
	{
		javax.swing.JOptionPane.showMessageDialog(MajaFrame.this, n_message + ": " + e.getLocalizedMessage(), "Maja Error", javax.swing.JOptionPane.ERROR_MESSAGE);
	}
	public void displayError(String n_error)
	{
		javax.swing.JOptionPane.showMessageDialog(MajaFrame.this, n_error, "Maja Error", javax.swing.JOptionPane.ERROR_MESSAGE);
	}
	public void displayWarning(String n_warning)
	{
		//javax.swing.JOptionPane.showMessageDialog(null, n_warning, "Maja Warning", javax.swing.JOptionPane.WARNING_MESSAGE);
	}
	public void displayStatus(String n_status){}
	public boolean setCurrentStatus(String n_message, float n_percent, boolean cancellable)
	{
		return _statusBar.setCurrentStatus(n_message, n_percent, cancellable);
	}
	public boolean updateCurrentStatus(float n_percent, boolean cancellable)
	{
		return _statusBar.updateCurrentStatus(n_percent, cancellable);
	}
	public void resetCurrentStatus()
	{
		_statusBar.resetCurrentStatus();
	}
	
	//***************-----INTERFACE: WindowListener-----***************//
	public void windowActivated(java.awt.event.WindowEvent e){}
	public void windowClosed(java.awt.event.WindowEvent e){}
	public void windowClosing(java.awt.event.WindowEvent e)
	{
		this.quitMaja();
	}
	public void windowDeactivated(java.awt.event.WindowEvent e){}
	public void windowDeiconified(java.awt.event.WindowEvent e){}
	public void windowIconified(java.awt.event.WindowEvent e){}
	public void windowOpened(java.awt.event.WindowEvent e){}
}
