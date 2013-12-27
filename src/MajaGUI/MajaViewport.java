package MajaGUI;
import javax.swing.*;
import Maja.*;

public class MajaViewport extends JPanel
{
	private static final long serialVersionUID = 3808528072214281343L;
	MajaProject _project;
	MajaProjectTabs.ProjectPanel _tabPanel;
	MajaFrame _majaFrame;

	JPanel _buttonBar;
	ButtonGroup _buttonGroup;
	java.util.Vector<ComponentTriggerButton> _buttons = new java.util.Vector<ComponentTriggerButton>();
	SwapButton _swapButton;

	java.util.Vector<ComponentPanel> _componentPanels = new java.util.Vector<ComponentPanel>();
	int _currentComponentIndex;
	
	public static final int ENTRIES_VIEW = 0;
	public static final int EDIT_VIEW = 1;
	public static final int OUTPUT_VIEW = 2;
	public static final int SOURCE_VIEW = 3;
	public MajaViewport(MajaFrame n_majaFrame, MajaProject p, MajaProjectTabs.ProjectPanel n_tabPanel, int n_defaultView)
	{
		super(new java.awt.BorderLayout());
		
		_project = p;
		_tabPanel = n_tabPanel;
		_majaFrame = n_majaFrame;
		
		//*****Create the panels
		_componentPanels.add(new MajaComponentEntriesTree(_majaFrame, _tabPanel, _project));
		_componentPanels.add(new MajaComponentEditor(_majaFrame, _tabPanel, _project));
		_componentPanels.add(new MajaComponentOutput(_majaFrame, _tabPanel, _project));
		//_componentPanels.add(new ComponentPanel(_majaFrame, _tabPanel, _project, "Source"));

		//*****Populate _buttonGroup
		_buttonGroup = new ButtonGroup();

		_buttons.add(new ComponentTriggerButton(this, "1", "Entries view", 0));
		_buttonGroup.add(_buttons.lastElement());
		_buttons.add(new ComponentTriggerButton(this, "2", "Edit view", 1));
		_buttonGroup.add(_buttons.lastElement());
		_buttons.add(new ComponentTriggerButton(this, "3", "Output view", 2));
		_buttonGroup.add(_buttons.lastElement());
		//_buttons.add(new ComponentTriggerButton(this, "4", "Source view", 3));
		//_buttonGroup.add(_buttons.lastElement());

		//*****Add buttons
		_buttonBar = new JPanel();
		_buttonBar.setLayout(new BoxLayout(_buttonBar, BoxLayout.PAGE_AXIS));
		
		_buttonBar.add(Box.createRigidArea(new java.awt.Dimension(0,25)));
		for(java.util.Enumeration<AbstractButton> e = _buttonGroup.getElements(); e.hasMoreElements();)
		{
			_buttonBar.add(e.nextElement());
		}
		_buttonBar.add(Box.createRigidArea(new java.awt.Dimension(0,25)));
		_swapButton = new SwapButton(this, "M", "Moves component to main window");
		_buttonBar.add(_swapButton);
		
		//*****Add button bar
		this.add(_buttonBar, java.awt.BorderLayout.WEST);
		
		//*****Set default
		this.setCurrentPanel(n_defaultView);
	}
	
	public java.awt.Dimension getMinimumSize()
	{
		//java.awt.Dimension d = super.getMinimumSize();

		return new java.awt.Dimension(300, 200);
	}
	
	public java.awt.Dimension getPreferredSize()
	{
		java.awt.Dimension d = super.getPreferredSize();
		if(d.width < getMinimumSize().width)
			return this.getMinimumSize();
		
		return d;
	}
	
	public void setCurrentPanel(int index)
	{
		if(index < 0 || index >= _componentPanels.size())
			return;
		
		
		ComponentPanel op = _componentPanels.get(_currentComponentIndex);
		ComponentPanel np = _componentPanels.get(index);

		op.setVisible(false);
		this.remove(op);

		_currentComponentIndex = index;
		this.add(np, java.awt.BorderLayout.CENTER);
		np.setVisible(true);
		
		//*****Make sure the right button is selected
		ComponentTriggerButton tb = _buttons.get(index);
		tb.setSelected(true);
	}
	
	public void setSwapButtonVisible(boolean b)
	{
		if(_swapButton != null)
			_swapButton.setVisible(b);
	}
	
	public void setTarget(MajaEntry e)
	{
		_componentPanels.get(_currentComponentIndex).setTarget(e);
	}

	public class ComponentTriggerButton extends JToggleButton implements java.awt.event.ActionListener
	{
		private static final long serialVersionUID = 6060084427359214365L;
		MajaViewport _majaPanel;
		int _componentPanelIndex;

		public ComponentTriggerButton(MajaViewport n_majaPanel, String n_text, String n_toolTip, int n_componentPanelIndex)
		{
			super(n_text);
			
			_majaPanel = n_majaPanel;
			_componentPanelIndex = n_componentPanelIndex;
			
			this.setToolTipText(n_toolTip);
			
			this.addActionListener(this);
		}
		
		public int getPanelIndex()
		{
			return _componentPanelIndex;
		}
		
		public void actionPerformed(java.awt.event.ActionEvent e)
		{
			_majaPanel.setCurrentPanel(_componentPanelIndex);
		}
	}
	
	public class SwapButton extends JButton implements java.awt.event.ActionListener
	{
		MajaViewport _majaPanel;
		public SwapButton(MajaViewport n_majaPanel, String n_text, String n_toolTip)
		{
			super(n_text);
			
			_majaPanel = n_majaPanel;
			
			this.setToolTipText(n_toolTip);
			this.addActionListener(this);
		}
		
		public void actionPerformed(java.awt.event.ActionEvent e)
		{
			if(_tabPanel != null)
				_majaPanel._tabPanel.swap(_majaPanel);
		}
	}
	
	public static class ComponentPanel extends JPanel
	{
		private static final long serialVersionUID = -1432354758058440027L;
		
		protected String _name;
		protected MajaFrame _majaFrame;
		protected MajaProjectTabs.ProjectPanel _projectPanel;
		protected MajaProject _majaProject;
		
		JLabel _label;
	
		public ComponentPanel(MajaFrame n_majaFrame, MajaProjectTabs.ProjectPanel n_projectPanel, MajaProject n_majaProject, String n_name)
		{
			super(new java.awt.BorderLayout());
			_name = new String(n_name);
			_majaProject = n_majaProject;
			_majaFrame = n_majaFrame;
			_projectPanel = n_projectPanel;
			
			this.setToolTipText(_name);
			this.setBackground(java.awt.Color.BLACK);
			
			_label = new JLabel(" " + _name);
			_label.setForeground(java.awt.Color.WHITE);
			
			this.add(_label, java.awt.BorderLayout.NORTH);
			this.setVisible(true);
		}
		
		public void setCaption()
		{
			_label.setText(" " + _name);
		}
		
		public void setCaption(String n_caption)
		{
			if(n_caption == null)
				this.setCaption();

			_label.setText(" " + _name + " (" + n_caption + ")");
		}
		
		public boolean setTarget(MajaEntry e)
		{
			return false;
		}
	}
}
