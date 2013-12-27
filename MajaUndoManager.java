package Maja;
public class MajaUndoManager
{
	private java.util.Vector<MajaUndoEvent> _events = new java.util.Vector<MajaUndoEvent>();
	private static MajaUndoEvent _currentEvent = null;
	
	private java.util.Vector<UndoListener> _listeners = new java.util.Vector<UndoListener>();
	
	public void beginUndoEvent(String description)
	{
		//If no undo event is in progress, add a new master event.
		//Master undo events are ones that are accessible to the user.
		if(_currentEvent != null)
		{
			MajaApp.displayWarning("Attempted to end undo event when none were in progress");
			return;
		}

		MajaUndoEvent newEvent = new MajaUndoEvent(_currentEvent, MajaUndoEvent.NONE, description);
		_currentEvent.addSubevent(newEvent);
		_currentEvent = newEvent;
	}

	public static void postUndoEvent(MajaProject.UndoEvent e)
	{
		//I figure this is probably a safer way to determine which project this event is for.
		MajaProject currentProject = e.getProject();
		if(currentProject != null)
		{
			//MajaApp.displayStatus("postUndoEvent activated with event " + e.getDescription());
			for(int i = 0; i < currentProject.getNumListeners(); i++)
			{
				currentProject.getListener(i).majaProjectUndoEventPosted(e);
			}
		}

		MajaUndoManager.postUndoEventInternal(e);
	}

	public static void postUndoEvent(MajaEntry.UndoEvent e)
	{
		MajaProject currentProject = MajaApp.getCurrentProject();
		if(currentProject != null)
		{
			for(int i = 0; i < currentProject.getNumListeners(); i++)
				currentProject.getListener(i).majaEntryUndoEventPosted(e);
		}

		MajaUndoManager.postUndoEventInternal(e);
	}

	public static void postUndoEvent(MajaUndoEvent e)
	{
		MajaProject currentProject = MajaApp.getCurrentProject();
		if(currentProject != null)
		{
			for(int i = 0; i < currentProject.getNumListeners(); i++)
				currentProject.getListener(i).undoEventPosted(e);
		}
		MajaUndoManager.postUndoEventInternal(e);
	}
	
	private static void postUndoEventInternal(MajaUndoEvent e)
	{
		if(_currentEvent == null)
			return;

		_currentEvent.addSubevent(e);
	}

	public void endUndoEvent()
	{
		if(_currentEvent == null)
		{
			MajaApp.displayWarning("Attempted to end undo event when none were in progress");
			return;
		}

		_currentEvent = _currentEvent.getParent();
	}
	
	public void addUndoListener(UndoListener l)
	{
		_listeners.add(l);
	}
	
	public UndoListener getListener(int index)
	{
		return _listeners.get(index);
	}

	public int getNumListeners()
	{
		return _listeners.size();
	}

	public void removeUndoListener(UndoListener l)
	{
		_listeners.remove(l);
	}

	public void undo()
	{
		if(_events.size() < 1)
			return;

		_events.lastElement().undo();
		_events.remove(_events.lastElement());
	}

	public interface UndoListener
	{
		public void undoEventPosted(MajaUndoEvent e);
		public void majaEntryUndoEventPosted(MajaEntry.UndoEvent e);
		public void majaProjectUndoEventPosted(MajaProject.UndoEvent e);
		//public void majaSourceUndoEventPosted(MajaUndoEvent.MajaSource e);
	}
}