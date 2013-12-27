package MajaGUI;
import javax.swing.*;
public class MajaStatusBar extends JPanel implements java.awt.event.MouseListener
{
	private static final long serialVersionUID = -531563065098285102L;
	private JLabel _messageLabel;
	private JProgressBar _progressBar;
	
	//*****Status-specific variables (reset with status)
	private String _statusMessage;
	private boolean _statusCancellable;
	private boolean _statusCancelled;
	
	//*****Constants
	private static final String DEFAULT_MESSAGE = "Ready.";
	private static final String PROGRESS_BAR_INDETERMINATE_MESSAGE = "Working...";
	private static final String PROGRESS_BAR_CANCELLING_MESSAGE = "Cancelling...";
	private static final int PROGRESS_BAR_MIN = 0;
	private static final int PROGRESS_BAR_MAX = 1000;
	
	public MajaStatusBar(int height)
	{
		//*****Setup the bar itself
		super(new java.awt.BorderLayout());
		super.setPreferredSize(new java.awt.Dimension(100, height));
		
		//*****Set up sub-components
		_messageLabel = new JLabel();
		_progressBar = new JProgressBar(SwingConstants.HORIZONTAL, PROGRESS_BAR_MIN, PROGRESS_BAR_MAX);
		_progressBar.addMouseListener(this);
		
		this.add(_messageLabel, java.awt.BorderLayout.WEST);
		this.add(_progressBar, java.awt.BorderLayout.EAST);
		
		//*****Clear current status
		this.resetCurrentStatus();
	}
	
	protected void hideProgress()
	{
		_progressBar.setIndeterminate(false);
		_progressBar.setValue(PROGRESS_BAR_MIN);
		_progressBar.setVisible(false);
	}
	
	protected void setMessage(String msg)
	{
		_statusMessage = msg;
		if(!_statusCancellable || _statusCancelled)
			_messageLabel.setText(" " + msg);
		else
			_messageLabel.setText(" " + msg + " (Click progress bar to cancel operation)");
	}
	
	protected void setProgress()
	{
		this.setProgress(-1.0f);
	}
	
	protected void setProgress(float progress)
	{
		if(progress < 0.0f || progress > 1.0f)
		{
			_progressBar.setString(PROGRESS_BAR_INDETERMINATE_MESSAGE);
			_progressBar.setIndeterminate(true);
		}
		else
		{
			_progressBar.setString(null);
			_progressBar.setValue((int)(progress * PROGRESS_BAR_MAX));
		}
		_progressBar.setStringPainted(true);
		_progressBar.setVisible(true);
	}
	
	public void cancelCurrentStatus()
	{
		if(_statusCancellable)
		{
			_statusCancelled = true;
		}
	}
	
	public void resetCurrentStatus()
	{
		_statusCancellable = false;
		_statusCancelled = false;
		this.setMessage(DEFAULT_MESSAGE);
		this.hideProgress();
		this.setVisible(false);
	}
	
	public boolean setCurrentStatus(String n_message, float n_percent, boolean cancellable)
	{
		_statusCancellable = cancellable;
		this.setMessage(n_message);
		
		this.setProgress(n_percent);
		
		this.setVisible(true);
		
		return true;
	}
	
	public boolean updateCurrentStatus(float n_percent, boolean cancellable)
	{
		//Update message and _statusCancellable, if necessary
		if(_statusCancellable != cancellable)
		{
			_statusCancellable = cancellable;
			this.setMessage(_statusMessage);
		}

		//Progress bar
		this.setProgress(n_percent);

		//Maybe cancel
		if(_statusCancelled && _statusCancellable)
		{
			_progressBar.setString(PROGRESS_BAR_CANCELLING_MESSAGE);
			this.setMessage(_statusMessage);
			return false;
		}
		
		return true;
	}
	
	//********************INTERFACE: MouseListener********************//
	public void mouseClicked(java.awt.event.MouseEvent e)
	{
		if(e.getSource() == _progressBar)
		{
			this.cancelCurrentStatus();
		}
	}
	public void mouseEntered(java.awt.event.MouseEvent e){}
	public void mouseExited(java.awt.event.MouseEvent e){}
	public void mousePressed(java.awt.event.MouseEvent e){}
	public void mouseReleased(java.awt.event.MouseEvent e){}
}