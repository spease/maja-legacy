package Maja;
import java.io.*;
import java.util.*;

public class MajaProject
{
	private String _name;
	private File _outputPath;

	private MajaEntry _headEntry;
	private MajaSource _headSource;
	
	private Vector<MajaEventListener> _eventListeners = new Vector<MajaEventListener>();
	
	//*****Events
	public static final int EVENT_ADDED = 0;
	public static final int EVENT_REMOVED = 1;
	public static final int EVENT_UPDATED = 2;
	
	MajaProject(String n_name)
	{
		this.setName(n_name);
		_headEntry = new MajaEntry(MajaEntry.HEAD);
		_headSource = new MajaSource(null);
		_headEntry.setName("<ENTRIES>");
		_headSource.setName("<ENTRIES>");
		_headEntry.setProject(this);
		_headSource.setProject(this);
	}

	public MajaEntry addEntry(MajaEntry n_entry)
	{
		return _headEntry.addChild(n_entry);
		/*
		if(_entries.contains(n_entry))
			return n_entry;
			
		MajaEntry rtn = MajaEntry.doDuplicateChildCheck(_entries, n_entry);
		if(rtn != null)
			return rtn;
		
		MajaUndoManager.postUndoEvent(this.createUndoEvent(MajaProject.UndoEvent.ADD_ENTRY, n_entry, null));
		_entries.add(n_entry);
		n_entry.setParent(null);
		
		return n_entry;
		*/
	}
	
	public MajaSource addSource(MajaSource n_source)
	{
		return _headSource.addChild(n_source);
		/*
		if(!_sources.contains(n_source))
			_sources.add(n_source);
			
		return n_source;
		*/
	}
	
	public void display()
	{
		MajaApp.displayStatus("==========PROJECT: " + _name);
		MajaApp.displayStatus("=====" + _name + " OUTPUT PATH: " + _outputPath);
		MajaApp.displayStatus("=====" + _name + " ENTRIES=====");
		for(int i = 0; i < _headEntry.getNumChildren(); i++)
		{
			_headEntry.getChild(i).display("");
			//_entries.get(i).display("");
		}
		
		MajaApp.displayStatus("=====" + _name + " SOURCES=====");
		for(int i = 0; i < _headSource.getNumChildren(); i++)
		{
			_headSource.getChild(i).display();
		}
	}
	
	/**
	* Exports the specified entry to the specified location, or exports all project entries to the specified location.
	*/
	public void exportEntry(File f, MajaEntry e)
	{
		MajaApp.setCurrentStatus("Exporting '" + e.getName() + "'...", -1.0f, false);
		if(e == null)
			_headEntry.export(new File(f, _headEntry.getName()));
		else
			e.export(new File(f, e.getName()));
		//MajaApp.displayStatus("=====Exporting directory: '" + f.getPath() + "'");
		/*
		if(e == null)
		{
			MajaApp.setCurrentStatus("Exporting '" + this.getName() + "'...", -1.0f, false);
			for(int i = 0; i < _entries.size(); i++)
			{
				_entries.get(i).export(new File(f, _entries.get(i).getName()));
			}
		}
		else
		{
			MajaApp.setCurrentStatus("Exporting '" + e.getName() + "'...", -1.0f, false);
			e.export(new File(f, e.getName()));
		}*/
		
		MajaApp.resetCurrentStatus();
	}

	public MajaEntry getEntry(int index)
	{
		return _headEntry.getChild(index);
	}
	
	public MajaEntry getEntryHead()
	{
		return _headEntry;
	}
	
	public String getName()
	{
		return new String(_name);
	}
	
	public int getNumEntries(boolean n_IncludeUnsyncedEntries)
	{
		if(n_IncludeUnsyncedEntries)
			return _headEntry.getNumChildren();
		else
		{
			int totalEntries = 0;
			for(int i = 0; i < _headEntry.getNumChildren(); i++)
			{
				if(_headEntry.getChild(i).getStatus() == MajaEntry.SYNCED)
					totalEntries++;
			}
			
			return totalEntries;
		}
	}
	
	public int getNumSources()
	{
		return _headSource.getNumChildren();
	}
	
	public File getOutputPath()
	{
		return _outputPath;
	}
	
	public MajaEntry importDirectory(MajaEntry parentEntry, File f)
	{
		if(!f.isDirectory())
		{
			MajaApp.displayError("Could not import '" + f.getPath() + "' as a directory, because it is not a directory.");
			return null;
		}
		
		MajaApp.setCurrentStatus("Importing '" + f.getPath() + "'...", -1.0f, false);
		
		MajaSource ms = this.addSource(new MajaSource(f));
		ms.sync();
		
		MajaEntry me = new MajaEntry(ms.getSourceEntryHead(), true);
		if(parentEntry != null)
			parentEntry.addChild(me);
		else
			this.addEntry(me);
		
		MajaApp.resetCurrentStatus();
		
		return me;
	}
	
	public MajaEntry importFile(MajaEntry parentEntry, File f)
	{
		if(!f.isFile())
		{
			MajaApp.displayError("Could not import '" + f.getPath() + "' as a file, because it is not a file");
			return null;
		}
		if(f.length() < 1)
		{
			MajaApp.displayWarning("File '" + f.getPath() + "' has a size of 0. You will not be able to write any VPs with this file.");
		}
		
		MajaApp.setCurrentStatus("Importing '" + f.getPath() + "' as file...", -1.0f, false);
		
		MajaSource ms = this.addSource(new MajaSource(f));

		MajaEntry me = new MajaEntry(ms.getSourceEntryHead(), false);
		if(parentEntry != null)
			me = parentEntry.addChild(me);
		else
			me = this.addEntry(me);
			
		MajaApp.resetCurrentStatus();

		return me;
	}
	
	public MajaSource importPackage(MajaEntry parentEntry, File f)
	{
		MajaSource ms = MajaIO.MajaHandlerManager.importPackage(f);
		if(ms != null)
		{
			this.addSource(ms);
			for(int i = 0; i < ms.getNumSourceEntries(); i++)
			{
				MajaEntry me = new MajaEntry(ms.getSourceEntry(i), true);
				
				if(parentEntry != null)
					parentEntry.addChild(me);
				else
					this.addEntry(me);
			}
		}
		return ms;
	}
	
	public void addEventListener(MajaEventListener mel)
	{
		if(mel == null)
			return;
		
		if(!_eventListeners.contains(mel))
			_eventListeners.add(mel);
	}
	
	public void removeEventListener(MajaEventListener mel)
	{
		if(mel == null)
			return;
		
		_eventListeners.remove(mel);
	}

	public void postEvent(int n_event, MajaBranch mb)
	{
		for(int i = 0; i < _eventListeners.size(); i++)
		{
			if(mb instanceof MajaEntry)
				_eventListeners.get(i).processEvent(n_event, (MajaEntry) mb);
			else if(mb instanceof MajaSource)
				_eventListeners.get(i).processEvent(n_event, (MajaSource) mb);
			else if(mb instanceof MajaSourceEntry)
				_eventListeners.get(i).processEvent(n_event, (MajaSourceEntry) mb);
			else
				MajaApp.displayWarning("Unknown message from " + mb.getName());
		}
	}
	
	public void postEvent(int n_event, MajaProject mp)
	{
		for(int i = 0; i < _eventListeners.size(); i++)
		{
			_eventListeners.get(i).processEvent(n_event, mp);
		}
	}
	
	public void removeEntry(MajaEntry n_entry)
	{
		if(n_entry == _headEntry)
			return;
		n_entry.remove();
		/*
		if(!_entries.contains(n_entry))
		{
			//This entry isn't part of the toplevel entries.
			//Have its parent remove it. This will invoke the MajaEntry undo system.
			MajaEntry parent = n_entry.getParent();
			if(parent != null)
				parent.removeChild(n_entry);
			return;
		}
		
		MajaUndoManager.postUndoEvent(this.createUndoEvent(MajaProject.UndoEvent.REMOVE_ENTRY, n_entry, null));
		_entries.remove(n_entry);
		*/
	}
	
	public void removeSource(MajaSource n_source)
	{
		if(n_source == _headSource)
			return;
		n_source.remove();
	}

	public void setName(String n_name)
	{
		if(n_name == null)
			return;

		if(n_name.equals(_name))
			return;

		_name = new String(n_name);
		
		//This needs to be moved back, or the undo system rethought
		//MajaUndoManager.postUndoEvent(this.createUndoEvent(MajaProject.UndoEvent.SET_NAME, null, null));
		this.postEvent(MajaProject.EVENT_UPDATED, this);
	}
	
	public void setOutputPath(File f)
	{
		_outputPath = f;
	}
	
	public interface MajaEventListener
	{
		public void processEvent(int n_event, MajaEntry me);
		public void processEvent(int n_event, MajaProject mp);
		public void processEvent(int n_event, MajaSource ms);
		public void processEvent(int n_event, MajaSourceEntry mse);
	}
	
	/*
	public MajaProject.UndoEvent createUndoEvent(int action, MajaEntry relatedEntry, MajaSource relatedSource)
	{
		switch(action)
		{
			case MajaProject.UndoEvent.ADD_ENTRY:
			{
				return new MajaProject.UndoEvent(this, "add entry", action, relatedEntry, null, null);
			}
			case MajaProject.UndoEvent.REMOVE_ENTRY:
			{
				return new MajaProject.UndoEvent(this, "remove entry", action, relatedEntry, null, null);
			}
			case MajaProject.UndoEvent.SET_NAME:
			{
				return new MajaProject.UndoEvent(this, "set name", action, null, null, new MajaProject(this.getName()));
			}
		}
		return null;
	}
	
	public static class UndoEvent extends MajaUndoEvent
	{
		MajaEntry _entryBuffer;
		MajaSource _sourceBuffer;
		MajaProject _projectBuffer;
		
		MajaProject _project;
		
		public static final int NONE = 0;
		public static final int ADD_ENTRY = 1;
		public static final int REMOVE_ENTRY = 2;
		public static final int ADD_SOURCE = 3;
		public static final int REMOVE_SOURCE = 4;
		public static final int SET_NAME = 5;
		
		public UndoEvent(MajaProject n_project, String n_description, int n_action, MajaEntry n_entryBuffer, MajaSource n_sourceBuffer, MajaProject n_projectBuffer)
		{
			super(null, n_action, n_description);
			_project = n_project;
			_entryBuffer = n_entryBuffer;
			_sourceBuffer = n_sourceBuffer;
			_projectBuffer = n_projectBuffer;
		}
		
		public MajaEntry getMajaEntryBuffer()
		{
			return _entryBuffer;
		}
		
		public MajaProject getProject()
		{
			return _project;
		}

		public boolean undo()
		{
			super.undo();
			
			switch(this.getAction())
			{
				case MajaProject.UndoEvent.ADD_ENTRY:
				{
					_project.removeEntry(_entryBuffer);
					return true;
				}
				case MajaProject.UndoEvent.REMOVE_ENTRY:
				{
					_project.addEntry(_entryBuffer);
					return true;
				}
				case MajaProject.UndoEvent.SET_NAME:
				{
					_project._name = new String(_projectBuffer._name);
					return true;
				}
			}
			return false;
		}
	}
	*/
}