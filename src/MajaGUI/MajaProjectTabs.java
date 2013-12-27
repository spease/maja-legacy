package MajaGUI;
import javax.swing.*;
import Maja.*;

public class MajaProjectTabs extends JTabbedPane implements javax.swing.event.ChangeListener, java.awt.event.MouseListener
{
	private static final long serialVersionUID = -6614264527504706745L;

	private MajaFrame _majaFrame;

	private java.util.Vector<ProjectPanel> _tabPanels = new java.util.Vector<ProjectPanel>();
	private ProjectsPanelUpdater _updater = new ProjectsPanelUpdater(this);

	MajaProjectTabs(MajaFrame n_majaFrame)
	{
		super();
		_majaFrame = n_majaFrame;
		for(int i = 0; i < MajaApp.getNumProjects(); i++)
		{
			this.addProject(MajaApp.getProject(i));
		}

		MajaApp.addMajaListener(_updater);
		this.addChangeListener(this);
		this.addMouseListener(this);
	}
	
	public void addProject(MajaProject p)
	{
		if(p == null)
			return;

		ProjectPanel tp = new MajaProjectTabs.ProjectPanel(p);
		_tabPanels.add(tp);
		this.addTab(p.getName(), tp);
		p.addEventListener(_updater);
	}
	
	public java.awt.Component getComponent(MajaProject p)
	{
		for(int i = 0; i < _tabPanels.size(); i++)
		{
			if(_tabPanels.get(i).getProject() == p)
			{
				return _tabPanels.get(i);
			}
		}
		
		return null;
	}
	
	public MajaProject getProject(java.awt.Component c)
	{
		for(int i = 0; i < _tabPanels.size(); i++)
		{
			if(_tabPanels.get(i) == c)
			{
				return _tabPanels.get(i).getProject();
			}
		}
		
		return null;
	}
	
	public void removeProject(MajaProject p)
	{
		if(p == null)
			return;

		java.awt.Component c = this.getComponent(p);
		this.remove(c);
		_tabPanels.remove(c);
		p.removeEventListener(_updater);
	}
	
	public void setProject(MajaProject p)
	{
		if(p == null)
			return;

		java.awt.Component c = this.getComponent(p);
		this.setSelectedComponent(c);
	}
	
	//********************ChangeListener********************//
	public void stateChanged(javax.swing.event.ChangeEvent e)
	{
		MajaApp.setCurrentProject(this.getProject(this.getSelectedComponent()));
	}
	
	//********************MouseListener********************//
	public void maybePopup(java.awt.event.MouseEvent e)
	{
		if(!e.isPopupTrigger())
			return;
		
		int tabIndex = this.indexAtLocation(e.getX(), e.getY());
		if(tabIndex == -1)
			return;
		
		java.awt.Component tab = this.getComponentAt(tabIndex);
		ProjectTabsPanelPopupMenu popup = new ProjectTabsPanelPopupMenu(this.getProject(tab), tab);
		popup.show(this, e.getX(), e.getY());
	}
	public void mouseClicked(java.awt.event.MouseEvent e){}
	public void mouseEntered(java.awt.event.MouseEvent e){}
	public void mouseExited(java.awt.event.MouseEvent e){}
	public void mousePressed(java.awt.event.MouseEvent e)
	{
		this.maybePopup(e);
	}
	public void mouseReleased(java.awt.event.MouseEvent e)
	{
		this.maybePopup(e);
	}
	
	//********************Subclasses********************//
	public class ProjectTabsPanelPopupMenu extends JPopupMenu implements java.awt.event.ActionListener
	{
		private static final long serialVersionUID = -3224884285864307661L;
		java.awt.Component _tab;
		MajaProject _majaProject;
		
		JMenuItem _menuRename;
		JMenuItem _menuClose;
		public ProjectTabsPanelPopupMenu(MajaProject n_project, java.awt.Component n_tab)
		{
			super();
			
			_majaProject = n_project;
			_tab = n_tab;
			
			_menuRename = new JMenuItem("Rename Project...");
			_menuRename.addActionListener(this);
			this.add(_menuRename);
			
			this.addSeparator();

			_menuClose = new JMenuItem("Close Project...");
			_menuClose.addActionListener(this);
			this.add(_menuClose);
		}
		
		public void actionPerformed(java.awt.event.ActionEvent e)
		{
			if(e.getSource() == _menuRename)
			{
				_majaFrame.renameProject(_majaProject);
			}
			else if(e.getSource() == _menuClose)
			{
				_majaFrame.closeProject(_majaProject);
			}
		}
	}
	public class ProjectsPanelUpdater implements MajaApp.MajaListener, MajaProject.MajaEventListener
	{
		private MajaProjectTabs _panel;
		public ProjectsPanelUpdater(MajaProjectTabs n_p)
		{
			_panel = n_p;
		}
		public void currentProjectChanged(MajaProject p)
		{
			_panel.setProject(p);
		}
		public void projectAdded(MajaProject p)
		{
			_panel.addProject(p);
		}
		public void projectRemoved(MajaProject p)
		{
			_panel.removeProject(p);
		}
		
		//********************MajaEventListener********************//
		public void processEvent(int e, MajaEntry me){}
		public void processEvent(int e, MajaProject mp)
		{
			switch(e)
			{
				case MajaProject.EVENT_UPDATED:
					java.awt.Component t = MajaProjectTabs.this.getComponent(mp);
					if(t != null)
					{
						MajaProjectTabs.this.setTitleAt(MajaProjectTabs.this.indexOfComponent(t), mp.getName());
					}
					break;
			}
		}
		public void processEvent(int e, MajaSource ms){}
		public void processEvent(int e, MajaSourceEntry mse){}
		
		//********************UndoListener********************//
		/*
		public void undoEventPosted(MajaUndoEvent e){}
		public void majaEntryUndoEventPosted(MajaEntry.UndoEvent e){}
		public void majaProjectUndoEventPosted(MajaProject.UndoEvent e)
		{
			switch(e.getAction())
			{
				case MajaProject.UndoEvent.SET_NAME:
				{
					MajaProject p = e.getProject();
					java.awt.Component t = MajaProjectTabs.this.getComponent(p);
					if(t != null)
					{
						MajaProjectTabs.this.setTitleAt(MajaProjectTabs.this.indexOfComponent(t), p.getName());
					}
					break;
				}
			}
		}*/
	}
	
	public class ProjectPanel extends JSplitPane
	{
		private static final long serialVersionUID = 7841128095655699702L;

		private MajaProject _project;
		
		private JSplitPane _leftPanel;
		
		java.util.Vector<MajaViewport> _majaPanels = new java.util.Vector<MajaViewport>();
		public ProjectPanel(MajaProject p)
		{
			super(JSplitPane.HORIZONTAL_SPLIT);
			_project = p;
			
			_majaPanels.add(new MajaViewport(_majaFrame, _project, this, MajaViewport.ENTRIES_VIEW));
			_majaPanels.add(new MajaViewport(_majaFrame, _project, this, MajaViewport.OUTPUT_VIEW));
			_majaPanels.add(new MajaViewport(_majaFrame, _project, this, MajaViewport.EDIT_VIEW));

			//*****Left panel
			_leftPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _majaPanels.get(0), _majaPanels.get(1));
			this.setLeftComponent(_leftPanel);

			//*****Right panel
			this.setRightComponent(_majaPanels.get(2));
			_majaPanels.get(2).setSwapButtonVisible(false);
		}
		
		public MajaProject getProject()
		{
			return _project;
		}
		
		public void setTarget(MajaEntry e)
		{
			for(int i = 0; i < _majaPanels.size(); i++)
			{
				_majaPanels.get(i).setTarget(e);
			}
		}
		
		//Rotates the three panels clockwise
		public void swap(MajaViewport c)
		{
			java.awt.Component cMainComponent = this.getRightComponent();
			MajaViewport cMain = null;
			if(_majaPanels.contains(cMainComponent))
			{
				cMain = (MajaViewport) this.getRightComponent();
			}

			if(c == cMain)
				return;
			
			if(c == _leftPanel.getTopComponent())
			{
				_leftPanel.setTopComponent(cMainComponent);
				this.setRightComponent(c);
				c.setSwapButtonVisible(false);
				if(cMain != null)
				{
					cMain.setSwapButtonVisible(true);
				}
			}
			else if(c == _leftPanel.getBottomComponent())
			{
				_leftPanel.setBottomComponent(cMainComponent);
				this.setRightComponent(c);
				c.setSwapButtonVisible(false);
				if(cMain != null)
				{
					cMain.setSwapButtonVisible(true);
				}
			}
			
			//Eh? How did we get here?
		}
	}
}