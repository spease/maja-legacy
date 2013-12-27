package MajaGUI;
import javax.swing.*;
import java.awt.event.*;
import Maja.*;

public class MajaMainMenuBar extends JMenuBar implements MajaApp.MajaListener
{
	private static final long serialVersionUID = -8539825096095378889L;
	private MajaFrame _majaFrame;
	private MMBMenu _majaMenu;
	private MMBMenu _projectMenu;

	MajaMainMenuBar(MajaFrame n_majaFrame)
	{
		_majaFrame = n_majaFrame;
		JMenuItem menuItem = null;

		//*****Maja menu
		//JMenu majaMenu = new JMenu("Maja");
		//majaMenu.setMnemonic(java.awt.event.KeyEvent.VK_M);
		//majaMenu.getAccessibleContext().setAccessibleDescription("Main menu");
		//majaMenu.setToolTipText("Main menu");
		_majaMenu = new MMBMenu("Maja", "Main menu", KeyEvent.VK_M);
			menuItem = new MMBMenuItem(
				"New Project",
				"Creates a new project",
				KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK))
				{
					private static final long serialVersionUID = 4672562618491997705L;
					public void exec(){MajaApp.createProject();}
				};
			_majaMenu.add(menuItem);

			_majaMenu.addSeparator();
			
			menuItem = new MMBMenuItem(
				"About Maja Express...",
				"Creates a new project",
				KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK))
				{
					private static final long serialVersionUID = 5736187218090803991L;
					public void exec(){_majaFrame.showAboutDialog();}};
			_majaMenu.add(menuItem);
			
			_majaMenu.addSeparator();
			
			menuItem = new MMBMenuItem(
				"Quit...", 
				"Quits the program",
				KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK))
				{
					private static final long serialVersionUID = 7517300707035044642L;
					public void exec(){_majaFrame.quitMaja();}
				};
			_majaMenu.add(menuItem);
		this.add(_majaMenu);
		_projectMenu = new MMBMenu("Project", "Project menu", KeyEvent.VK_P);
			menuItem = new MMBMenuItem(
				"Set output path...",
				"Sets current project intermediate data output path",
				KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK))
				{/**
					 * 
					 */
					private static final long serialVersionUID = 0L;

				public void exec(){_majaFrame.setOutputPath(MajaApp.getCurrentProject());}};
			_projectMenu.add(menuItem);
			
			_projectMenu.addSeparator();
			
			menuItem = new MMBMenuItem(
				"Import file(s)...",
				"Imports file into the current project",
				KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK))
				{
					static final long serialVersionUID = -4953726179778056891L;
					public void exec(){_majaFrame.importFile(MajaApp.getCurrentProject(), null);}
				};
			_projectMenu.add(menuItem);
			menuItem = new MMBMenuItem(
				"Import directory...",
				"Imports directory into the current project",
				KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK))
				{
					private static final long serialVersionUID = 2771797764369216223L;
					public void exec(){_majaFrame.importDirectory(MajaApp.getCurrentProject(), null);}
				};
			_projectMenu.add(menuItem);
			menuItem = new MMBMenuItem(
				"Import Package(s)...",
				"Imports a VP or CVP file into the current project",
				KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK))
				{
					private static final long serialVersionUID = 58850638946114805L;
					public void exec(){_majaFrame.importPackage(MajaApp.getCurrentProject(), null);}
				};
			_projectMenu.add(menuItem);
				
			_projectMenu.addSeparator();
			
			menuItem = new MMBMenuItem(
				"Export Directory...",
				"Exports the current project's entries to a directory",
				KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK))
				{
					private static final long serialVersionUID = 4824106577865474836L;
					public void exec(){_majaFrame.exportEntry(MajaApp.getCurrentProject(), null);}
				};
			_projectMenu.add(menuItem);
			menuItem = new MMBMenuItem(
				"Export Package...",
				"Exports a package file from the current project",
				KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK))
				{
					private static final long serialVersionUID = -936297461965408802L;
					public void exec(){_majaFrame.exportPackage(MajaApp.getCurrentProject(), null);}
				};
			_projectMenu.add(menuItem);
			
			_projectMenu.addSeparator();
				
			menuItem = new MMBMenuItem(
				"Close Project...",
				"Closes current project",
				KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK))
				{
					private static final long serialVersionUID = 7182399312744943150L;
					public void exec(){_majaFrame.closeProject(MajaApp.getCurrentProject());}
				};
			_projectMenu.add(menuItem);
		this.add(_projectMenu);
		
		MajaApp.addMajaListener(this);
	}
	
	public void currentProjectChanged(MajaProject p)
	{
		if(p == null)
		{
			this.remove(_projectMenu);
			this.repaint();
		}
		else if(!this.isAncestorOf(_projectMenu))
			this.add(_projectMenu);
	}
	public void projectAdded(MajaProject p){}
	public void projectRemoved(MajaProject p){}
	
	//********************SUBCLASS: MMBMenu********************//
	public class MMBMenu extends JMenu implements MajaShortcutManager.Interface
	{
		private static final long serialVersionUID = -1402512810680078544L;

		MMBMenu(String name, String description, int key)
		{
			super(name);
			this.setMnemonic(key);
			this.getAccessibleContext().setAccessibleDescription(description);
			this.setToolTipText(description);
		}
	}

	//********************SUBCLASS: MMBMenuItem********************//
	public class MMBMenuItem extends JMenuItem implements java.awt.event.ActionListener, MajaShortcutManager.Interface
	{
		private static final long serialVersionUID = 1955210018505928968L;
		MMBMenuItem(String name, String description, KeyStroke keystroke)
		{
			super(name);
			if(description != null)
			{
				this.getAccessibleContext().setAccessibleDescription(description);
				this.setToolTipText(description);
			}
			if(keystroke != null)
				this.setAccelerator(keystroke);
			this.addActionListener(this);
		}
		
		public void actionPerformed(java.awt.event.ActionEvent e){exec();}
		public void exec(){}
	}
}