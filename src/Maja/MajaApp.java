package Maja;
public class MajaApp
{
	private static java.util.Vector<MajaProject> _majaProjects = new java.util.Vector<MajaProject>();
	private static java.util.Vector<MajaListener> _majaListeners = new java.util.Vector<MajaListener>();
	private static java.util.Vector<MajaOutputListener> _majaOutputListeners = new java.util.Vector<MajaOutputListener>();
	private static MajaProject _currentProject = null;
	
	public static javax.swing.JFrame _currentGUI = null;

	public MajaApp()
	{
		Thread.currentThread().setName("Main");
		MajaIO.MajaHandlerManager.addPackageHandler(MajaIO.MajaHandlerZIP.initialize());
		MajaIO.MajaHandlerManager.addPackageHandler(MajaIO.MajaHandlerVP.initialize());
		//MajaIO.MajaHandlerCVP.initialize();
		_currentGUI = new MajaGUI.MajaFrame("Maja Express");
		
		//MajaProject p = this.addProject(new MajaProject("root_fs2.vp"));
		//p.importVP("root_fs2.vp");
		/*
		this.addProject(new MajaProject("smarty_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/smarty_fs2.vp");
		this.addProject(new MajaProject("sparky_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/sparky_fs2.vp");
		this.addProject(new MajaProject("sparky_hi_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/sparky_hi_fs2.vp");
		this.addProject(new MajaProject("stu_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/stu_fs2.vp");
		this.addProject(new MajaProject("tango1_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/tango1_fs2.vp");
		this.addProject(new MajaProject("tango2_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/tango2_fs2.vp");
		this.addProject(new MajaProject("tango3_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/tango3_fs2.vp");
		this.addProject(new MajaProject("warble_fs2.vp")).importVP("/media/partitions/System/Program Files/freespace2/retail/warble_fs2.vp");*/
		/*
		MajaProject mp = new MajaProject("Untitled");
		mp.setOutputPath("/home/steven/maja/out_test.vp");
		//mp.importVP(filename);
		if(filename.equals("write"))
		{
			//mp.importFile("testinput.txt");
			mp.importVP("root_fs2.vp");
			//mp.display();
			mp.exportVP();
		}
		else if(filename.equals("read"))
		{
			mp.importVP("out_test.vp");
			//mp.display();
			MajaEntry e = mp.getEntry(new MajaIndex("data/tables/ai.tbl"));
			e.display();
			e.export(e.getName());
		}*/
	}
	
	private static MajaProject addProject(MajaProject p)
	{
		_majaProjects.add(p);
		for(int i = 0; i < _majaListeners.size(); i++)
			_majaListeners.get(i).projectAdded(p);
		if(_currentProject == null)
			MajaApp.setCurrentProject(p);
		return p;
	}
	
	public static MajaProject createProject()
	{
		String newName = "Untitled";
		int newNumber = -1;
		for(int i = 0; i < _majaProjects.size(); i++)
		{
			MajaProject p = _majaProjects.get(i);
			String pName = p.getName();
			int len = newName.length();
			if(pName.length() < newName.length())
				len = pName.length();
			//If both names start with the same thing
			if(pName.substring(0, len).equals(newName))
			{
				int tempNumber = 0;
				
				//Find the last space
				int lastSpace = pName.lastIndexOf(' ');
				
				//If it exists, find our number
				if(lastSpace > -1)
				{
					try
					{
						tempNumber = Integer.valueOf(pName.substring(lastSpace+1));
					}
					catch(java.lang.NumberFormatException e){}
				}
				
				//If number is bigger, change our number (if number is nonexistent, our number will be 1
				if(tempNumber >= newNumber)
					newNumber = tempNumber+1;
			}
		}
		
		if(newNumber > -1)
			newName += " " + newNumber;
		
		return MajaApp.addProject(new MajaProject(newName));
	}
	
	public static int getNumProjects()
	{
		return _majaProjects.size();
	}
	
	public static MajaProject getProject(int index)
	{
		return _majaProjects.get(index);
	}
	
	public static MajaProject getProject(String projName)
	{
		for(int i = 0; i < _majaProjects.size(); i++)
		{
			MajaProject p = _majaProjects.get(i);
			if(p.getName().equals(projName))
				return p;
		}
		
		return null;
	}
	
	public static boolean removeProject(MajaProject p)
	{
		int index = _majaProjects.indexOf(p);
		if(index == -1)
			return false;

		_majaProjects.remove(p);
		for(int i = 0; i < _majaListeners.size(); i++)
			_majaListeners.get(i).projectRemoved(p);

		if(_currentProject == p)
		{
			if(index < _majaProjects.size())
				MajaApp.setCurrentProject(_majaProjects.get(index));
			else if(_majaProjects.size() > 0)
				MajaApp.setCurrentProject(_majaProjects.get(index-1));
			else
				MajaApp.setCurrentProject(null);
		}

		return true;
	}
	
	public static MajaProject getCurrentProject()
	{
		return _currentProject;
	}
	
	public static void main(String[] args) throws java.io.IOException
	{
		MajaApp mj = new MajaApp();
		if(args.length < 1)
		{
			//Create an empty project
			MajaApp.createProject();
		}
		else
		{
			for(int i = 0; i < args.length; i++)
			{
				//Remove any apostrophes from the beginning and end of the string
				String filePath = args[i];
				if(filePath.substring(0,0).equals("\""))
					filePath = filePath.substring(1);
				if(filePath.substring(filePath.length()-1,filePath.length()-1).equals("\""))
					filePath = filePath.substring(0, filePath.length()-2);

				//Get the file
				java.io.File f = new java.io.File(filePath);
				MajaProject mp = MajaApp.createProject();
				if(f.isFile())
				{
					if(mp.importPackage(null, f) != null)
					{
						mp.setName(f.getName());
					}
					else
					{
						mp.importFile(null, f);
					}
				}
				else if(f.isDirectory())
				{
					if(mp.importDirectory(null, f) != null)
					{
						mp.setName(f.getName());
					}
				}
			}
		}
	}
	
	public static void setCurrentProject(MajaProject p)
	{
		_currentProject = p;
		for(int i = 0; i < _majaListeners.size(); i++)
			_majaListeners.get(i).currentProjectChanged(p);
	}
	
	//***************-----INTERFACE: MajaListener-----***************//
	public interface MajaListener
	{
		public void currentProjectChanged(MajaProject p);
		public void projectAdded(MajaProject p);
		public void projectRemoved(MajaProject p);
	}
	
	public static void addMajaListener(MajaListener l)
	{
		_majaListeners.add(l);
	}
	
	public static void removeMajaListener(MajaListener l)
	{
		_majaListeners.remove(l);
	}
	
	//***************-----INTERFACE: MajaOutputListener-----***************//
	public interface MajaOutputListener
	{
		public void displayException(Exception e, String n_message);
		public void displayError(String n_message);
		public void displayWarning(String n_message);
		public void displayStatus(String n_message);

		/**
		* Updates current status indicator and message. Returns false if operation should be stopped
		*/
		public boolean setCurrentStatus(String n_message, float n_percent, boolean n_cancellable);

		/**
		* Updates current status indicator. Returns false if operation should be stopped
		*/
		public boolean updateCurrentStatus(float n_percent, boolean n_cancellable);
		public void resetCurrentStatus();
	}
	
	public static void addMajaOutputListener(MajaOutputListener l)
	{
		_majaOutputListeners.add(l);
	}
	
	public static void removeMajaOutputListener(MajaOutputListener l)
	{
		_majaListeners.remove(l);
	}
	
	public static void displayException(Exception e, String n_message)
	{
		for(int i = 0; i < _majaOutputListeners.size(); i++)
			_majaOutputListeners.get(i).displayException(e, n_message);
		
		if(_majaOutputListeners.size() < 1)
		{
			System.out.println("((EXCEPTION)): " + n_message);
		}
	}
	public static void displayError(String n_error)
	{
		for(int i = 0; i < _majaOutputListeners.size(); i++)
			_majaOutputListeners.get(i).displayError(n_error);
		
		//javax.swing.JOptionPane.showMessageDialog(null, n_error, "Maja Error", javax.swing.JOptionPane.ERROR_MESSAGE);
		if(_majaOutputListeners.size() < 1)
		{
			System.out.println("((ERROR)): " + n_error);
		}
	}
	public static void displayWarning(String n_warning)
	{
		for(int i = 0; i < _majaOutputListeners.size(); i++)
			_majaOutputListeners.get(i).displayWarning(n_warning);
		
		//javax.swing.JOptionPane.showMessageDialog(null, n_warning, "Maja Warning", javax.swing.JOptionPane.WARNING_MESSAGE);
		if(_majaOutputListeners.size() < 1)
		{
			System.out.println("((WARNING)): " + n_warning);
		}
	}
	public static void displayStatus(String n_status)
	{
		for(int i = 0; i < _majaOutputListeners.size(); i++)
			_majaOutputListeners.get(i).displayStatus(n_status);
		
		if(_majaOutputListeners.size() < 1)
		{
			System.out.println(n_status);
		}
	}
	
	public static boolean setCurrentStatus(String n_message, float n_percent, boolean n_cancellable)
	{
		boolean keepGoing = true;
		for(int i = 0; i < _majaOutputListeners.size(); i++)
		{
			if(!_majaOutputListeners.get(i).setCurrentStatus(n_message, n_percent, n_cancellable))
				keepGoing = false;
		}
		
		return keepGoing;
	}
	
	public static boolean updateCurrentStatus(float n_percent, boolean n_cancellable)
	{
		boolean keepGoing = true;
		for(int i = 0; i < _majaOutputListeners.size(); i++)
		{
			if(!_majaOutputListeners.get(i).updateCurrentStatus(n_percent, n_cancellable))
				keepGoing = false;
		}
		
		return keepGoing;
	}
	
	public static void resetCurrentStatus()
	{
		for(int i = 0; i < _majaOutputListeners.size(); i++)
			_majaOutputListeners.get(i).resetCurrentStatus();
	}
}