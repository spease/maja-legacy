package MajaGUI;
import javax.swing.*;
import Maja.*;

public class MajaComponentOutput extends MajaViewport.ComponentPanel implements MajaApp.MajaOutputListener
{
	private static final long serialVersionUID = 2713923932720466341L;
	private JPanel _panel;
	private JScrollPane _scrollPane;
	MajaComponentOutput(MajaFrame n_majaFrame, MajaProjectTabs.ProjectPanel tp, MajaProject p)
	{
		super(n_majaFrame, tp, p, "[3] Output");
		
		//*****Setup
		_panel = new JPanel();
		_panel.setBackground(java.awt.Color.WHITE);
		_panel.setOpaque(true);
		_panel.setLayout(new BoxLayout(_panel, BoxLayout.PAGE_AXIS));
		_scrollPane = new JScrollPane(_panel);
		this.add(_scrollPane, java.awt.BorderLayout.CENTER);
		
		//Add initial entry
		this.addMessage("Output component initialized.", java.awt.Color.GRAY);
		
		//*****Add thyself
		MajaApp.addMajaOutputListener(this);
	}
	
	protected void addMessage(String n_message, java.awt.Color n_color)
	{
		JLabel newField = new JLabel(n_message);
		_panel.add(newField);
		//newField.setEditable(false);
		newField.setBorder(new javax.swing.border.EmptyBorder(0,0,0,0));
		newField.setForeground(n_color);
		newField.setBackground(java.awt.Color.WHITE);
		_panel.repaint();
		
		//Scroll down
		//Doesn't work.
		//this.getVerticalScrollBar().setValue(this.getVerticalScrollBar().getMaximum());
	}
	
	public void displayException(Exception e, String n_message)
	{
		this.addMessage(n_message + ": " + e.getLocalizedMessage(), java.awt.Color.BLACK);
	}
	public void displayError(String n_message)
	{
		this.addMessage(n_message, java.awt.Color.RED);
	}
	public void displayWarning(String n_message)
	{
		this.addMessage(n_message, java.awt.Color.MAGENTA);
	}
	public void displayStatus(String n_message)
	{
		this.addMessage(n_message, java.awt.Color.GRAY);
	}
	public void resetCurrentStatus(){}
	public boolean setCurrentStatus(String n_message, float n_percent, boolean cancellable){return true;}
	public boolean updateCurrentStatus(float n_percent, boolean cancellable){return true;}
}