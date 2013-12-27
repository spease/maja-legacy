package MajaGUI;
import javax.swing.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import Maja.*;
import MajaIO.*;

public class MajaComponentEditor extends MajaViewport.ComponentPanel implements java.awt.event.ActionListener
{
	private static final long serialVersionUID = -5420381220771745205L;
	private BasicEditor _currentEditor;
	private JScrollPane _scrollPane;
	
	private JPanel _buttonPanel;
	private JButton _openButton;
	private JButton _saveButton;
	private JButton _saveAsButton;
	
	private MajaFileChooser _openChooser;
	private MajaFileChooser _saveAsChooser;
	
	//*****Target
	private MajaEntry _targetEntry;
	private java.io.File _targetFile;
	String _targetExtension;
	boolean _targetModified;
	
	//STATIC
	private static boolean _imagePluginsLoaded = false;
	
	//STATIC FINAL
	private static final int MAX_SIZE = Integer.MAX_VALUE;

	public MajaComponentEditor(MajaFrame n_majaFrame, MajaProjectTabs.ProjectPanel n_ProjectPanel, MajaProject n_majaProject)
	{
		super(n_majaFrame, n_ProjectPanel, n_majaProject, "[2] Editor");
		
		_currentEditor = null;
		_scrollPane = new JScrollPane();
		this.add(_scrollPane, java.awt.BorderLayout.CENTER);
		
		_buttonPanel = new JPanel(new java.awt.FlowLayout());
		
		_openButton = new JButton("Open");
		_openButton.setToolTipText("Opens a new document");
		_openButton.addActionListener(this);
		_buttonPanel.add(_openButton);
		
		_saveButton = new JButton("Save");
		_saveButton.setToolTipText("Saves the current document");
		_saveButton.addActionListener(this);
		_buttonPanel.add(_saveButton);
		
		_saveAsButton = new JButton("Save As...");
		_saveAsButton.setToolTipText("Saves the current document to a new file");
		_saveAsButton.addActionListener(this);
		_buttonPanel.add(_saveAsButton);
		
		this.add(_buttonPanel, java.awt.BorderLayout.SOUTH);
		
		_openChooser = new MajaFileChooser();
		_saveAsChooser = new MajaFileChooser();
		
		//*****Initial target entry values
		_targetExtension = "";
		_targetModified = false;
		
		//*****For the image editor
		if(!_imagePluginsLoaded)
		{
			javax.imageio.spi.IIORegistry iior = javax.imageio.spi.IIORegistry.getDefaultInstance();
			iior.registerServiceProvider(new PCXReaderSpi(), javax.imageio.spi.ImageReaderSpi.class);
			iior.registerServiceProvider(new TGAReaderSpi(), javax.imageio.spi.ImageReaderSpi.class);
			
			_imagePluginsLoaded = true;
		}
		
		//Setup buttons 'n stuff
		this.updateLook();
	}
	
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if(e.getSource() == _openButton)
		{
			_openChooser.setDefaultType(MajaFileChooser.OPEN_FILE);
			int returnValue = _openChooser.showDialog(this);
			
			if(returnValue == MajaFileChooser.APPROVE_OPTION)
			{
				final java.io.File f = _openChooser.getSelectedFile();
				this.setTarget(f);
			}
		}
		else if(_currentEditor != null)
		{
			if(e.getSource() == _saveButton)
			{
				this.saveTarget();
			}
			if(e.getSource() == _saveAsButton)
			{
				_saveAsChooser.setDefaultType(MajaFileChooser.SAVE_AS_FILE);
				
				//*****Set default extension and filename
				if(_targetExtension != null)
				{
					_saveAsChooser.addExtension(_targetExtension, _targetExtension.toUpperCase() + " File");
					java.io.File cwd = _saveAsChooser.getCurrentDirectory();
					
					String filename = null;
					
					if(_targetFile != null)
						filename = _targetFile.getName();
					else if(_targetEntry != null)
						filename = _targetEntry.getName();
					
					if(filename != null)
						_saveAsChooser.setSelectedFile(new java.io.File(cwd, filename));
				}
				
				//****Show it
				int returnValue = _saveAsChooser.showDialog(this);
		
				if(returnValue == MajaFileChooser.APPROVE_OPTION)
				{
					final java.io.File f = _saveAsChooser.getSelectedFile();
					MajaOutputStream mos = null;
					try
					{
						mos = new MajaOutputStream(f);
						_currentEditor.save(mos);
						mos.close();
						this.setTargetModified(false);
						this.setTarget(f);
					}
					catch(java.io.IOException exc)
					{
						MajaApp.displayException(exc, "Error opening output stream for '" + f.getName() + "'");
						if(mos != null)
						{
							try
							{
								mos.close();
							}
							catch(java.io.IOException exc2){}
						}
					}
				}
			}
		}
	}
	
	public boolean getTargetModified()
	{
		return _targetModified;
	}
	
	private void targetChanged()
	{
		_targetModified = false;
		this.updateLook();
	}
	
	public void saveTarget()
	{
		if(_currentEditor == null)
			return;

		if(!_currentEditor.canSave())
		{
			MajaApp.displayError("Current editor does not support saving files");
		}
		else if(_targetFile != null)
		{
			MajaOutputStream mos = null;
			try
			{
				mos = new MajaOutputStream(_targetFile);
				_currentEditor.save(mos);
				mos.close();
				this.setTargetModified(false);
			}
			catch(java.io.IOException exc)
			{
				MajaApp.displayException(exc, "Error opening output stream for '" + _targetFile.getName() + "'");
				if(mos != null)
				{
					try
					{
						mos.close();
					}
					catch(java.io.IOException exc2) {}
				}
			}
		}
		else if(_majaProject.getOutputPath() != null)
		{
			MajaOutputStream mos = _targetEntry.openOutputStream();
			if(mos != null)
			{
				_currentEditor.save(mos);
				_targetEntry.closeOutputStream(mos);
				this.setTargetModified(false);
			}
			else
			{
				MajaApp.displayError("Error opening output stream for '" + _targetEntry.getName() + "'");
			}
		}
		else
		{
			MajaApp.displayError("You must specify an output path before you can save anything.");
		}
	}
	
	private boolean showCloseOptions(String filename)
	{
		if(this.getTargetModified())
		{
			final String options[] = {
				"Save and switch",
				"Switch without saving",
				"Cancel",
			};
			int result = JOptionPane.showOptionDialog(
				_majaFrame,
				"Do you want to save '" + filename + "' before switching files?",
				"Closing '" + filename + "'",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[2]);
			if(result == JOptionPane.YES_OPTION)
				this.saveTarget();
			else if(result == JOptionPane.NO_OPTION)
				{}
			else if(result == JOptionPane.CANCEL_OPTION)
				return false;
		}
		
		return true;
	}
	
	private BasicEditor chooseEditor(String extension)
	{
		BasicEditor newEditor = null;
		if(	extension == null
			|| extension.equals("c")
			|| extension.equals("cfg")
			|| extension.equals("cpp")
			|| extension.equals("fs2")
			|| extension.equals("h")
			|| extension.equals("htm")
			|| extension.equals("html")
			|| extension.equals("java")
			|| extension.equals("log")
			|| extension.equals("lua")
			|| extension.equals("patch")
			|| extension.equals("py")
			|| extension.equals("tbd")
			|| extension.equals("tbl")
			|| extension.equals("tbm")
			|| extension.equals("txt")
			|| extension.equals("xml")
			)
		{
			newEditor = new TextEditor();
		}
		else if(extension.equals("pof"))
		{
			newEditor = new POFEditor();
		}
		else if(ImageIO.getImageReadersBySuffix(extension).hasNext())
		{
			newEditor = new ImageEditor();
		}
		return newEditor;
	}
	
	public void updateLook()
	{
		if(_currentEditor != null)
		{
			String newCaption = "<UNKNOWN>";
			if(_targetFile != null)
			{
				try
				{
					newCaption = _targetFile.getCanonicalPath();
				}
				catch(java.io.IOException exc)
				{
					newCaption = _targetFile.getPath();
				}
			}
			if(_targetEntry != null)
			{
				newCaption = _targetEntry.getName();
			}
			
			if(_targetModified)
			{
				this.setCaption(newCaption + "*");
				_saveButton.setEnabled(_currentEditor.canSave());
			}
			else
			{
				this.setCaption(newCaption);
				_saveButton.setEnabled(false);
			}
			_saveButton.setVisible(_currentEditor.canSave());
			_saveAsButton.setEnabled(_currentEditor.canSave());
			_saveAsButton.setVisible(_currentEditor.canSave());
		}
		else
		{
			this.setCaption();
			
			_saveButton.setEnabled(false);
			_saveButton.setVisible(false);
			_saveAsButton.setEnabled(false);
			_saveAsButton.setVisible(false);
		}
	}
	
	public boolean setTarget(java.io.File f)
	{
		if(f == null || !f.isFile())
			return false;
		
		if(f.length() > MAX_SIZE)
		{
			//MajaInputStream.getString only supports up to MAX_INT
			MajaApp.displayError("Editor can only work with files <= " + MAX_SIZE/1024/1024 + " MB in size. '" + f.getName() + "' is " + f.length()/1024/1024 + " MB.");
			return false;
		}
		
		MajaInputStream mis = null;
		try
		{
			mis = new MajaInputStream(f);
		}
		catch(java.io.FileNotFoundException exc)
		{
			MajaApp.displayError("Couldn't open file '" + f.getName() + "' - permissions may be invalid");
			return false;
		}
		
		if(!this.showCloseOptions(f.getName()))
			return false;
		
		String extension = null;
		String filename = f.getName();
		int lastIndex = filename.lastIndexOf('.')+1;
		if(lastIndex < filename.length() && lastIndex > 0)
			extension = filename.substring(lastIndex).toLowerCase();
			
		BasicEditor newEditor = this.chooseEditor(extension);
		if(newEditor == null)
		{
			MajaApp.displayError("'" + filename + "' does not have an extension that is supported by the editor");
			return false;
		}
		
		if(newEditor.load(mis, f.length()))
		{
			_scrollPane.setViewportView(newEditor);
			_currentEditor = newEditor;
			
			_targetFile = f;
			_targetEntry = null;
			_targetExtension = extension;
		}
		else
		{
			MajaApp.displayError("Error loading '" + f.getName() + "' into the editor");
		}
	try
		{
			mis.close();
		}
		catch(java.io.IOException exc)
		{
			MajaApp.displayWarning("Couldn't perform filesystem close for '" + f.getName() + "'");
		}
		
		this.targetChanged();
		return true;
	}
	
	public boolean setTarget(MajaEntry e)
	{
		if(e == null || e.getType() != MajaEntry.FILE)
			return false;
		
		//javax.swing.filechooser.FileView fv = new javax.swing.filechooser.FileView();
		
		if(e.getStatus() != MajaEntry.SYNCED)
		{
			MajaApp.displayError("Could not open entry '" + e.getName() + "' because it is not synced.");
			return false;
		}
		MajaSourceEntry mse = e.getSourceEntry();
		if(mse == null)
		{
			MajaApp.displayError("Could not open entry '" + e.getName() + "' because it does not have a source.");
			return false;
		}
		
		if(mse.getUncompressedSize() < 0)
		{
			MajaApp.displayError("Editor can only work with files with a known uncompressed filesize.");
			return false;
		}
		
		if(mse.getUncompressedSize() > MAX_SIZE)
		{
			//MajaInputStream.getString only supports up to MAX_INT
			MajaApp.displayError("Editor can only work with files <= " + MAX_SIZE/1024/1024 + " MB in size. '" + e.getName() + "' is " + mse.getUncompressedSize()/1024/1024 + " MB.");
			return false;
		}
		
		MajaInputStream mis = e.openInputStream();
		if(mis == null)
		{
			MajaApp.displayError("Could not get input stream for entry '" + e.getName() + "'");
			return false;
		}
		
		if(!this.showCloseOptions(e.getName()))
			return false;
		
		String extension = null;
		String filename = e.getName();
		int lastIndex = filename.lastIndexOf('.')+1;
		if(lastIndex < filename.length() && lastIndex > 0)
			extension = filename.substring(lastIndex).toLowerCase();
		
		BasicEditor newEditor = this.chooseEditor(extension);
		if(newEditor == null)
		{
			MajaApp.displayError("'" + filename + "' does not have an extension that is supported by the editor");
			return false;
		}
		
		if(newEditor.load(mis,mse.getUncompressedSize()))
		{
			_scrollPane.setViewportView(newEditor);
			_currentEditor = newEditor;
			
			_targetFile = null;
			_targetEntry = e;
			_targetExtension = extension;
		}
		else
		{
			MajaApp.displayError("Error loading '" + e.getName() + "' into the editor");
		}
		e.closeInputStream(mis);
		
		this.targetChanged();
		return true;
	}
	
	public void setTargetModified(boolean newval)
	{
		_targetModified = newval;
		this.updateLook();
	}
	
	public class BasicEditor extends JPanel
	{
		private static final long serialVersionUID = 3650052431866126237L;

		public BasicEditor()
		{
			super(new java.awt.BorderLayout());
		}
		
		public boolean canSave()
		{
			return false;
		}

		public boolean load(MajaInputStream mis, long misLength)
		{
			MajaApp.displayError("Function not supported.");
			return false;
		}
		
		public boolean save(MajaOutputStream mos)
		{
			MajaApp.displayError("Function not supported.");
			return false;
		}
	}
	
	public class TextEditor extends BasicEditor implements javax.swing.event.UndoableEditListener
	{
		private static final long serialVersionUID = -9217620743894121229L;
		private JTextPane _textPane;
		
		TextEditor()
		{
			_textPane = new JTextPane();
			_textPane.getDocument().addUndoableEditListener(this);
			this.add(_textPane, java.awt.BorderLayout.CENTER);
		}
		
		public boolean canSave()
		{
			return true;
		}
		
		public boolean load(MajaInputStream mis, long misLength)
		{
			try
			{
				String newContent = null;
				if(misLength > Integer.MAX_VALUE)
				{
					MajaApp.displayError("TextEditor is unable to load files larger than " + Integer.MAX_VALUE + " bytes");
					return false;
				}
				newContent = mis.readString((int)misLength);
				if(newContent == null)
				{
					MajaApp.displayError("TextEditor tried to read an empty file");
					return false;
				}
	
				_textPane.setText(newContent);
			}
			catch(java.io.IOException ex)
			{
				MajaApp.displayException(ex, "TextEditor couldn't read file");
				return false;
			}
			
			return true;
		}
		
		public boolean save(MajaOutputStream mos)
		{
			String outputText = _textPane.getText();
			try
			{
				mos.writeString(outputText, outputText.length());
			}
			catch(java.io.IOException ex)
			{
				MajaApp.displayException(ex, "Error writing '" + _targetEntry.getName() + "'");
				return false;
			}
			return true;
		}
		
		//********************UndoableEditListener********************//
		public void undoableEditHappened(javax.swing.event.UndoableEditEvent e)
		{
			if(_targetEntry != null)
			{
				MajaComponentEditor.this.setTargetModified(true);
			}
		}
	}
	
	public class POFEditor extends BasicEditor implements javax.swing.event.UndoableEditListener
	{
		private final java.awt.Color COLOR_BACKGROUND_VALID_NONE = new java.awt.Color(127,0,0,255);
		private final java.awt.Color COLOR_BACKGROUND_VALID_READ = new java.awt.Color(127,127,0,255);
		private final java.awt.Color COLOR_BACKGROUND_VALID_RW = java.awt.Color.BLACK;
		private final java.awt.Color COLOR_BACKGROUND_HOVER = new java.awt.Color(0,127,0,255);
		private final java.awt.Color COLOR_FOREGROUND = java.awt.Color.WHITE;
		
		private final java.awt.Color COLOR_MODIFIABLE = java.awt.Color.WHITE;
		private final java.awt.Color COLOR_UNMODIFIABLE = new java.awt.Color(224,224,224,255);
			
		public class matrix
		{
			private vec3d _rvec, _uvec, _fvec;
			public matrix(float n_rx, float n_ry, float n_rz, float n_ux, float n_uy, float n_uz, float n_fx, float n_fy, float n_fz)
			{
				_rvec = new vec3d(n_rx, n_ry, n_rz);
				_uvec = new vec3d(n_ux, n_uy, n_uz);
				_fvec = new vec3d(n_fx, n_fy, n_fz);
			}
			
			public matrix(vec3d n_rvec, vec3d n_uvec, vec3d n_fvec)
			{
				_rvec = new vec3d(n_rvec);
				_uvec = new vec3d(n_uvec);
				_fvec = new vec3d(n_fvec);
			}
			
			public matrix(String s)
			{
				_rvec = new vec3d();
				_uvec = new vec3d();
				_fvec = new vec3d();
				
				matrix mtx = this.parseMatrix(s);
				if(mtx != null)
				{
					_rvec = mtx._rvec;
					_uvec = mtx._uvec;
					_fvec = mtx._fvec;
				}
			}
			
			private matrix parseMatrix(String n_s)
			{
				if(n_s == null || n_s.length() < 0)
					return null;
				
				String s = n_s.toUpperCase().replaceAll("\\s","");
					
				int rvecStart = s.indexOf("RVEC:(");
				if(rvecStart < 0)
					return null;
				rvecStart += 6;
				
				int uvecStart = s.indexOf("UVEC:(", rvecStart);
				if(uvecStart < 0)
					return null;
				uvecStart += 6;
				
				int fvecStart = s.indexOf("FVEC:(", uvecStart);
				if(fvecStart < 0)
					return null;
				fvecStart += 6;
				
				int rvecEnd = s.indexOf(')',rvecStart);
				int uvecEnd = s.indexOf(')',uvecStart);
				int fvecEnd = s.indexOf(')',fvecStart);
				if(rvecEnd < 0 || uvecEnd < 0 || fvecEnd < 0)
					return null;
				
				vec3d rvec = new vec3d(s.substring(rvecStart, rvecEnd));
				vec3d uvec = new vec3d(s.substring(uvecStart, uvecEnd));
				vec3d fvec = new vec3d(s.substring(fvecStart, fvecEnd));
				if(rvec == null || uvec == null || fvec == null)
					return null;
					
				return new matrix(rvec, uvec, fvec);
			}
			
			public void scale(float n_scale)
			{
				_rvec.scale(n_scale);
				_uvec.scale(n_scale);
				_fvec.scale(n_scale);
			}
			
			public String toString()
			{
				return "RVEC:(" + _rvec.toString() + ") \nUVEC:(" + _uvec.toString() + ") \nFVEC:(" + _fvec.toString() + ")";
			}
			
			public void write(MajaOutputStream mos) throws java.io.IOException
			{
				_rvec.write(mos);
				_uvec.write(mos);
				_fvec.write(mos);
			}
		}
		public class vec3d
		{
			private float _x,_y,_z;
			
			public vec3d()
			{
				_x = _y = _z = 0.0f;
			}
			
			public vec3d(float n_x, float n_y, float n_z)
			{
				_x = n_x;
				_y = n_y;
				_z = n_z;
			}
			
			public vec3d(vec3d n_v)
			{
				_x = n_v._x;
				_y = n_v._y;
				_z = n_v._z;
			}
			
			public vec3d(String s)
			{
				_x = _y = _z = 0.0f;
				
				vec3d vec = this.parseVec3D(s);
				if(vec != null)
				{
					_x = vec._x;
					_y = vec._y;
					_z = vec._z;
				}
			}
			
			public float getX()
			{
				return _x;
			}
			
			public float getY()
			{
				return _y;
			}
			
			public float getZ()
			{
				return _z;
			}
			
			private vec3d parseVec3D(String n_s)
			{
				String s = n_s.toUpperCase().replaceAll("\\s","");
				if(n_s == null || n_s.length() < 1)
					return null;
					
				int xStart = s.indexOf("X:");
				if(xStart < 0)
					return null;
				xStart += 2;
				
				int yStart = s.indexOf("Y:", xStart);
				if(yStart < 0)
					return null;
				yStart += 2;
				
				int zStart = s.indexOf("Z:", yStart);
				if(zStart < 0)
					return null;
				zStart += 2;
				
				float x = Float.parseFloat(s.substring(xStart,yStart-2));
				float y = Float.parseFloat(s.substring(yStart,zStart-2));
				float z = Float.parseFloat(s.substring(zStart));
				
				return new vec3d(x,y,z);
			}
			
			public void scale(float n_scale)
			{
				_x *= n_scale;
				_y *= n_scale;
				_z *= n_scale;
			}
			
			public String toString()
			{
				return "X: " + _x + " Y: " + _y + " Z: " + _z;
			}
			
			public void write(MajaOutputStream mos) throws java.io.IOException
			{
				mos.writeLittleFloat(_x);
				mos.writeLittleFloat(_y);
				mos.writeLittleFloat(_z);
			}
		}
		
		public class POFChunk extends JPanel implements java.awt.event.MouseListener
		{
			//*****VALID_*
			public static final int VALID_NONE = 0;
			public static final int VALID_READ = 1;
			public static final int VALID_RW = 2;
			
			private String _chunkID;
			private int _chunkSize;
			
			private JLabel _headerLabel;
			private JLabel _stateLabel;
			private java.util.Vector<POFField> _fields = new java.util.Vector<POFField>();
			
			private boolean _expanded;
			private int _numRows;
			private int _valid;
			
			public POFChunk(String n_id, int n_size)
			{
				_numRows = 0;
				_expanded = false;
				_valid = VALID_NONE;
				
				_layout = new java.awt.GridBagLayout();
				this.setLayout(_layout);
				
				_chunkID = new String(n_id);
				_chunkSize = n_size;
				
				_headerLabel = new JLabel(n_id, JLabel.LEFT);
				_headerLabel.setOpaque(true);
				_headerLabel.addMouseListener(this);
				
				_stateLabel = new JLabel("", JLabel.RIGHT);
				_stateLabel.setOpaque(true);
				_stateLabel.addMouseListener(this);
				
				java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
				gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbc.gridy = _numRows++;
				
				gbc.gridx = 0;
				gbc.weightx = 0.8;
				this.add(_headerLabel, gbc);
				
				gbc.gridx = 1;
				gbc.weightx = 0.2;
				this.add(_stateLabel, gbc);
				
				this.setExpanded(false);
			}
			
			public POFField createField(String n_name, boolean modifiable)
			{
				POFField f = new POFField(n_name, modifiable);
				_fields.add(f);
				
				java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
				gbc.fill = java.awt.GridBagConstraints.BOTH;
				gbc.weightx = 0.5;
				gbc.gridx = 0;
				gbc.gridy = _numRows++;
				this.add(f.getLabelField(), gbc);
				gbc.gridx = 1;
				this.add(f.getValueField(), gbc);
				f.setVisible(_expanded);
				
				//Update expanded status
				this.updateStatus();
				
				return f;
			}
			
			public int getValid()
			{
				return _valid;
			}
			
			public boolean isExpanded()
			{
				return _expanded;
			}
			
			public void setBackground(java.awt.Color c)
			{
				if(_headerLabel != null && _stateLabel != null)
				{
					_headerLabel.setBackground(c);
					_stateLabel.setBackground(c);
				}
			}
			
			public void setExpanded(boolean n_expanded)
			{
				_expanded = n_expanded;
				for(int i = 0; i < _fields.size(); i++)
				{
					_fields.get(i).setVisible(_expanded);
				}
				
				this.updateStatus();
			}
			
			public void setForeground(java.awt.Color c)
			{
				if(_headerLabel != null && _stateLabel != null)
				{
					_headerLabel.setForeground(c);
					_stateLabel.setForeground(c);
				}
			}
			
			//If a chunk is valid, we can write it without any problems.
			private void setValid(int n_valid)
			{
				_valid = n_valid;
				this.updateStatus();
			}
			
			private void updateStatus()
			{
				if(_expanded)
				{
					_stateLabel.setText("<< Collapse");
				}
				else
				{
					_stateLabel.setText("(" + _fields.size() + ") Expand >>");
				}
				
				switch(_valid)
				{
					case VALID_RW:
						this.setBackground(COLOR_BACKGROUND_VALID_RW);
						break;
					case VALID_READ:
						this.setBackground(COLOR_BACKGROUND_VALID_READ);
						break;
					default:
						this.setBackground(COLOR_BACKGROUND_VALID_NONE);
				}
				this.setForeground(COLOR_FOREGROUND);
			}
			
			public void write(MajaOutputStream mos) throws java.io.IOException
			{
				if(_valid != VALID_RW)
					return;
				
				mos.writeString(_chunkID, 4);
				mos.writeLittleInt(_chunkSize);
				
				int bytesWritten = 0;
				for(int i = 0; i < _fields.size(); i++)
				{
					bytesWritten += _fields.get(i).write(mos);
				}
				if(bytesWritten != _chunkSize)
				{
					MajaApp.displayWarning(_chunkID + " write: bytesWritten = " + bytesWritten + "; _chunkSize = " + _chunkSize + ";");
				}
			}
			
			//********************MouseListner********************//
			public void mouseClicked(java.awt.event.MouseEvent mse)
			{
				this.setExpanded(!this.isExpanded());
				this.setBackground(COLOR_BACKGROUND_HOVER);
			}
			public void mouseEntered(java.awt.event.MouseEvent mse)
			{
				this.setBackground(COLOR_BACKGROUND_HOVER);
			}
			public void mouseExited(java.awt.event.MouseEvent mse)
			{
				this.updateStatus();
			}
			public void mousePressed(java.awt.event.MouseEvent mse) {}
			public void mouseReleased(java.awt.event.MouseEvent mse) {}
		}
		
		public class POFField implements java.awt.event.ActionListener, java.awt.event.FocusListener
		{			
			private JComponent _label;
			private JComponent _value;
			
			private boolean _modifiable;
			
			private int _type;
			private double _valueDouble;
			private float _valueFloat;
			private int _valueInt;
			private long _valueLong;
			private String _valueString;
			private matrix _valueMatrix;
			private vec3d _valueVec3D;
			private byte[] _valueByteData;
			
			private final int TYPE_NONE = 0;
			private final int TYPE_DOUBLE = 1;
			private final int TYPE_FLOAT = 2;
			private final int TYPE_INT = 3;
			private final int TYPE_LONG = 4;
			private final int TYPE_STRING = 5;
			private final int TYPE_MATRIX = 6;
			private final int TYPE_VEC3D = 7;
			private final int TYPE_BYTEDATA = 8;
			
			public POFField(String n_label, boolean n_modifiable)
			{
				_modifiable = n_modifiable;
				
				_label = new JLabel(n_label);
				if(_modifiable)
				{
					_value = new JTextField();
					((JTextField)_value).addActionListener(this);
					((JTextField)_value).addFocusListener(this);
				}
				else
				{
					_value = new JLabel();
				}
				/*
				_label.setEnabled(false);
				_value.setEnabled(false);
				_label.setPreferredSize(new java.awt.Dimension((int)_label.getPreferredSize().getWidth(), 20));
				_value.setPreferredSize(new java.awt.Dimension((int)_value.getPreferredSize().getHeight(), 20));
				*/
				
				this.setType(TYPE_NONE);
			}
			
			public void cancel()
			{
				this.refreshText();
			}
			
			private JComponent getLabelField()
			{
				return _label;
			}
			
			private String getText()
			{
				if(_value instanceof JLabel)
					return ((JLabel)_value).getText();
				else if(_value instanceof JTextField)
					return ((JTextField)_value).getText();
				
				return "";
			}
			
			private JComponent getValueField()
			{
				return _value;
			}
			
			private int getIntValue()
			{
				return _valueInt;
			}
			
			private void refreshText()
			{
				String s = "";
				switch(_type)
				{
					case TYPE_DOUBLE:
						s = new Double(_valueDouble).toString();
						break;
					case TYPE_FLOAT:
						s = new Float(_valueFloat).toString();
						break;
					case TYPE_INT:
						s = new Integer(_valueInt).toString();
						break;
					case TYPE_LONG:
						s = new Long(_valueLong).toString();
						break;
					case TYPE_STRING:
						s = _valueString;
						break;
					case TYPE_MATRIX:
						s = _valueMatrix.toString();
						break;
					case TYPE_VEC3D:
						s = _valueVec3D.toString();
						break;
					case TYPE_BYTEDATA:
						s = "<" + _valueByteData.length + " bytes of data>";
						break;
				}
				if(_value instanceof JLabel)
				{
					((JLabel)_value).setText(s);
				}
				else if(_value instanceof JTextField)
				{
					((JTextField)_value).setText(s);
				}
			}
			
			private void save()
			{
				switch(_type)
				{
					case TYPE_DOUBLE:
						_valueDouble = Double.parseDouble(this.getText());
						break;
					case TYPE_FLOAT:
						_valueFloat = Float.parseFloat(this.getText());
						break;
					case TYPE_INT:
						_valueInt = Integer.parseInt(this.getText());
						break;
					case TYPE_LONG:
						_valueLong = Long.parseLong(this.getText());
						break;
					case TYPE_STRING:
						_valueString = this.getText();
						break;
					case TYPE_MATRIX:
						_valueMatrix = new matrix(this.getText());
						break;
					case TYPE_VEC3D:
						_valueVec3D = new vec3d(this.getText());
						break;
					default:
						break;
				}
				this.refreshText();
			}
			
			private void setType(int n_type)
			{
				_type = n_type;
				switch(_type)
				{
					case TYPE_NONE:
					case TYPE_DOUBLE:
					case TYPE_FLOAT:
					case TYPE_INT:
					case TYPE_LONG:
					case TYPE_STRING:
					case TYPE_MATRIX:
					case TYPE_VEC3D:
						_value.setEnabled(_modifiable);
						break;
					case TYPE_BYTEDATA:
					default:
						_value.setEnabled(false);
				}
				
				this.update();
			}
			
			public String getLabelText()
			{
				if(!(_label instanceof JLabel))
					return "";
				
				return ((JLabel)_label).getText();
			}
			
			public String getValueText()
			{
				if(_value instanceof JLabel)
				{
					return ((JLabel)_value).getText();
				}
				else if(_value instanceof JTextField)
				{
					return ((JTextField)_value).getText();
				}
				else
				{
					return "";
				}
			}
			
			public void setValue(float n_value)
			{
				this.setType(TYPE_FLOAT);
				_valueFloat = n_value;
				this.refreshText();
			}
			
			public void setValue(int n_value)
			{
				this.setType(TYPE_INT);
				_valueInt = n_value;
				this.refreshText();
			}
			
			public void setValue(String n_value)
			{
				this.setType(TYPE_STRING);
				_valueString = n_value;
				this.refreshText();
			}
			
			public void setValue(matrix n_value)
			{
				this.setType(TYPE_MATRIX);
				_valueMatrix = n_value;
				this.refreshText();
			}
			
			public void setValue(float n_x, float n_y, float n_z)
			{
				this.setType(TYPE_VEC3D);
				_valueVec3D = new vec3d(n_x, n_y, n_z);
				this.refreshText();
			}
			
			public void setValue(vec3d n_value)
			{
				this.setType(TYPE_VEC3D);
				_valueVec3D = n_value;
				this.refreshText();
			}
			
			public void setValue(byte[] n_value)
			{
				this.setType(TYPE_BYTEDATA);
				_valueByteData = n_value;
				this.refreshText();
			}
			public void setVisible(boolean n_visible)
			{
				_label.setVisible(n_visible);
				_value.setVisible(n_visible);
			}
			
			public void update()
			{
				_label.setOpaque(true);
				_label.setBackground(COLOR_UNMODIFIABLE);
				
				_value.setOpaque(true);
				if(_value.isEnabled())
				{
					_value.setBackground(COLOR_MODIFIABLE);
				}
				else
				{
					_value.setBackground(COLOR_UNMODIFIABLE);
				}
			}
			
			public int write(MajaOutputStream mos) throws java.io.IOException
			{
				if(mos == null)
					return 0;
					
				int bytesWritten = 0;
					
				switch(_type)
				{
					case TYPE_DOUBLE:
						mos.writeLittleDouble(_valueDouble);
						bytesWritten += 8;
						break;
					case TYPE_FLOAT:
						mos.writeLittleFloat(_valueFloat);
						bytesWritten += 4;
						break;
					case TYPE_INT:
						mos.writeLittleInt(_valueInt);
						bytesWritten += 4;
						break;
					case TYPE_LONG:
						mos.writeLittleLong(_valueLong);
						bytesWritten += 4;
						break;
					case TYPE_STRING:
						mos.writeLittleInt(_valueString.length());
						mos.writeString(_valueString, _valueString.length());
						bytesWritten += 4 + _valueString.length();
						break;
					case TYPE_MATRIX:
						_valueMatrix.write(mos);
						bytesWritten += 36;
						break;
					case TYPE_VEC3D:
						_valueVec3D.write(mos);
						bytesWritten += 12;
						break;
					case TYPE_BYTEDATA:
						mos.write(_valueByteData);
						bytesWritten += _valueByteData.length;
						break;
					case TYPE_NONE:
						break;
				}
				return bytesWritten;
			}
			
			//***************-----INTERFACE: ActionListener-----***************//
			public void actionPerformed(java.awt.event.ActionEvent ae)
			{
				this.save();
				MajaComponentEditor.this.setTargetModified(true);
			}
			
			//***************-----INTERFACE: FocusListener-----***************//
			public void focusGained(java.awt.event.FocusEvent fe)
			{
			}
			public void focusLost(java.awt.event.FocusEvent fe)
			{
				this.cancel();
			}
		}
		private final String POF_HEADER_STRING = "PSPO";
		private final int POF_MIN_VERSION = 1900;
		private final int POF_MAX_MAJOR_VERSION = 30;
		
		private POFField _header;
		private POFField _version;
		
		private java.util.Vector<POFChunk> _chunks = new java.util.Vector<POFChunk>();
		private java.awt.GridBagLayout _layout;
		private int _numRows;
		
		POFEditor()
		{
			_numRows = 0;
			_layout = new java.awt.GridBagLayout();
			this.setLayout(_layout);
			this.setBackground(java.awt.Color.WHITE);
			
			_header = new POFField("Header:", false);
			this.add(_header);
			_version = new POFField("Version:", false);
			this.add(_version);
		}
		
		public void add(POFField f)
		{
			java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.5;
			gbc.gridx = 0;
			gbc.gridy = _numRows++;
			this.add(f.getLabelField(), gbc);
			gbc.gridx = 1;
			this.add(f.getValueField(), gbc);
		}
		
		public POFChunk createChunk(String n_name, int n_size)
		{
			POFChunk ch = new POFChunk(n_name, n_size);
			_chunks.add(ch);
			
			java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.gridx = 0;
			gbc.gridy = _numRows++;
			gbc.gridwidth = 2;
			gbc.weightx = 1.0;
			this.add(ch, gbc);
			
			return ch;
		}
		
		public boolean canSave()
		{
			for(int i = 0; i < _chunks.size(); i++)
			{
				if(_chunks.get(i).getValid() != POFChunk.VALID_RW)
					return false;
			}
			return true;
		}
		
		public boolean load(MajaInputStream mis, long misLength)
		{
			if(mis.isCompressed())
			{
				MajaApp.displayError("POF editor can only handle uncompressed files.");
				return false;
			}
			try
			{
				//Use this to keep reading from going over in package files
				long totalBytesRead = 0;
				
				//Work out the header
				String fileHeader = mis.readString(4);
				totalBytesRead += 4;
				
				if(!fileHeader.equals(POF_HEADER_STRING))
				{
					MajaApp.displayError("Invalid POF file, header is '" + fileHeader + "'");
					return false;
				}				
				_header.setValue(fileHeader);
				
				//Work out the version
				int fileVersion = mis.readLittleInt();
				totalBytesRead += 4;
				
				if(fileVersion < POF_MIN_VERSION || fileVersion/100 > POF_MAX_MAJOR_VERSION)
				{
					MajaApp.displayError("Editor does not support version '" + fileVersion + "'");
					return false;
				}
				_version.setValue(fileVersion);
				
				//*****Read chunks
				while(totalBytesRead < misLength)
				{
					String c_id = mis.readString(4);
					int c_len = mis.readLittleInt();
					totalBytesRead += 8;
					totalBytesRead += this.loadChunk(c_id, c_len, mis);
				}
			}
			catch(java.io.IOException ex)
			{
				MajaApp.displayException(ex, "POFEditor encountered an exception");
				return false;
			}
			
			//WMC - test write
			/*
			if(!e.getName().equals("myTest.pof"))
			{
				try
				{
					MajaOutputStream mos = new MajaOutputStream(new java.io.File("C:\\Documents and Settings\\ThinkPad\\Desktop\\myTest.pof"));
					this.save(mos);
				}
				catch(java.io.FileNotFoundException exc)
				{
					MajaApp.displayError("Oops!");
				}
			}*/
			
			return true;
		}
		
		//Returns bytes read
		public int loadChunk(String c_id, int c_len, MajaInputStream mis)
		{
			if(c_id == null || c_id.length() < 1)
				return 0;
			
			String r_id = null;
			int r_len = 0;
			try
			{
				POFChunk pc = this.createChunk(c_id, c_len);
				POFField f = null;
				int fileVersion = _version.getIntValue();
				//MajaApp.displayStatus("Loading chunk '" + c_id + "'(" + c_len + " bytes) using version " + fileVersion);
				if(c_id.equals("OHDR") || c_id.equals("HDR2"))
				{
					//FS1 format
					if(c_id.equals("OHDR"))
					{
						pc.createField("Num submodels:", false).setValue(mis.readLittleInt());
						pc.createField("Radius:", true).setValue(mis.readLittleFloat());
						pc.createField("Flags:", false).setValue(mis.readLittleInt());
					}
					else
					{
						//FS2 format
						pc.createField("Radius:", true).setValue(mis.readLittleFloat());
						pc.createField("Flags:", false).setValue(mis.readLittleInt());
						pc.createField("Num submodels:", false).setValue(mis.readLittleInt());
					}
					
					r_len += 12;
					
					pc.createField("Bounding Mins:", true).setValue(mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat());
					pc.createField("Bounding Maxes:", true).setValue(mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat());
					
					r_len += 24;
					
					int fileNumLODs = mis.readLittleInt();
					pc.createField("LOD Submodels:", false).setValue(fileNumLODs);
					for(int i = 0; i < fileNumLODs; i++)
					{
						pc.createField("LOD " + i + " submodel:", true).setValue(mis.readLittleInt());
					}
					
					r_len += 4 + fileNumLODs*4;
					
					int fileNumDebris = mis.readLittleInt();
					pc.createField("Debris Submodels:", false).setValue(fileNumDebris);
					for(int i = 0; i < fileNumDebris; i++)
					{
						pc.createField("Debris " + i + " submodel:", true).setValue(mis.readLittleInt());
					}
					
					r_len += 4 + fileNumDebris*4;
					
					if(fileVersion >= 2009)
					{
						pc.createField("Area mass:",true).setValue(mis.readLittleFloat());
						pc.createField("Center of mass:",true).setValue(
							mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						pc.createField("Area MOI:",true).setValue(new matrix(
							mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat(),
								mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat(),
								mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat()));
						r_len += 4+12*4;
					}
					else if(fileVersion >= 1903)
					{
						pc.createField("Volume mass:",true).setValue(mis.readLittleFloat());
						pc.createField("Center of mass:",true).setValue(
							mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						pc.createField("Volume MOI:",true).setValue(new matrix(
							mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat(),
								mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat(),
								mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat()));
						r_len += 52;
					}
					//WMC - commented out as this is not compatible with
					//same-version saving method.
					/*
					//These are the values FS2 assumes if they aren't specified.
					float fileMass = 50.0f;
					vec3d fileCenterOfMass = new vec3d(0.0f, 0.0f, 0.0f);
					matrix fileMOI = new matrix(
							.001f, 0.0f, 0.0f,
							0.0f, .001f, 0.0f,
							0.0f, 0.0f, .001f);
					if(fileVersion >= 1903)
					{
						fileMass = mis.readLittleFloat();
						fileCenterOfMass = new vec3d(mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat());
						fileMOI = new matrix(
								mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat(),
								mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat(),
								mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat());
						r_len += 4+12+36;
						
						if(fileVersion < 2009)
						{
							float areaMass = (float) Math.pow(fileMass, 0.6667f) * 4.65f;
							float massRatio = fileMass/areaMass;
							
							//WMC - FS2 always uses area mass
							fileMass = areaMass;

							fileMOI.scale(massRatio);
						}
					}
					pc.createField("Mass:",false).setValue(fileMass);
					pc.createField("Center of mass:", false).setValue(fileCenterOfMass);
					pc.createField("MOI:",false).setValue(fileMOI);
					*/
					
					if(fileVersion >= 2014)
					{
						int numCrossSections = mis.readLittleInt();
						r_len += 4;
						
						if(numCrossSections > 0)
						{
							pc.createField("Cross-sections:", false).setValue(numCrossSections);
							for(int i = 0; i < numCrossSections; i++)
							{
								pc.createField("Cross-section Z:", true).setValue(mis.readLittleFloat());
								pc.createField("Cross-section radius:", true).setValue(mis.readLittleFloat());
							}
							r_len += numCrossSections*8;
						}
					}
					
					if(fileVersion >= 2007)
					{
						int numLights = mis.readLittleInt();
						r_len += 4;
						
						if(numLights > 0)
						{
							pc.createField("Lights:", false).setValue(numLights);
							for(int i = 0; i < numLights; i++)
							{
								pc.createField("Light " + i + " type:", true).setValue(mis.readLittleInt());
							}
							r_len += numLights*4;
						}
					}
					
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("SOBJ") || c_id.equals("OBJ2"))
				{
					pc.createField("ID:",true).setValue(mis.readLittleInt());
					r_len += 4;
					
					if(c_id.equals("OBJ2"))
					{
						pc.createField("Radius:",true).setValue(mis.readLittleFloat());
						r_len += 4;
					}
					
					pc.createField("Parent submodel:",true).setValue(mis.readLittleInt());
					pc.createField("Offset:",true).setValue(mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
					r_len += 16;
					
					if(c_id.equals("SOBJ"))
					{
						pc.createField("Radius:",true).setValue(mis.readLittleFloat());
						r_len += 4;
					}
					
					pc.createField("Geometric center:",true).setValue(
						mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
					pc.createField("Bounding mins:",true).setValue(
						mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
					pc.createField("Bounding maxes:",true).setValue(
						mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
					
					r_len += 36;
	
					int nameLen = mis.readLittleInt();
					pc.createField("Name:",true).setValue(mis.readString(nameLen));
					int propsLen = mis.readLittleInt();
					pc.createField("Properties:",true).setValue(mis.readString(propsLen));
					r_len += 8+nameLen+propsLen;

					pc.createField("Movement type:",true).setValue(mis.readLittleInt());
					pc.createField("Movement axis:",true).setValue(mis.readLittleInt());
					r_len += 8;
					
					pc.createField("Chunks:",false).setValue(mis.readLittleInt());
					r_len += 4;
					
					int bspLen = mis.readLittleInt();
					pc.createField("BSP data size:",false).setValue(bspLen);
					
					byte b[] = null;
					byte buf[] = new byte[bspLen];
					int bytesRead = mis.read(buf);

					r_len += 4 + bytesRead;
					
					if(bytesRead != bspLen)
					{
						b = new byte[bytesRead];
						System.arraycopy(buf, 0, b, 0, bytesRead);
						pc.setValid(POFChunk.VALID_READ);
					}
					else
					{
						b = buf;
						pc.setValid(POFChunk.VALID_RW);
					}
					
					pc.createField("BSP data:",false).setValue(b);
				}
				else if(c_id.equals("SLDC"))
				{
					int shieldCollisionTreeSize = mis.readLittleInt();
					pc.createField("Shield collision tree size:",false).setValue(shieldCollisionTreeSize);
					
					byte b[] = null;
					byte buf[] = new byte[shieldCollisionTreeSize];
					int bytesRead = mis.read(buf);

					r_len += 4 + bytesRead;
					
					if(bytesRead != shieldCollisionTreeSize)
					{
						b = new byte[bytesRead];
						System.arraycopy(buf, 0, b, 0, bytesRead);
						pc.setValid(POFChunk.VALID_READ);
					}
					else
					{
						b = buf;
						pc.setValid(POFChunk.VALID_RW);
					}
					
					pc.createField("Shield collision tree:",false).setValue(b);
				}
				else if(c_id.equals("SHLD"))
				{
					//We have two sets of data, if either is bad,
					//then this is bad to write.
					boolean isValid = true;
					
					int numVertices = mis.readLittleInt();
					pc.createField("Shield vertices:",false).setValue(numVertices);
					
					int verticesSize = 12*numVertices;
					byte b[] = null;
					byte buf[] = new byte[verticesSize];
					int bytesRead = mis.read(buf);

					r_len += 4 + bytesRead;
					
					if(bytesRead != verticesSize)
					{
						b = new byte[bytesRead];
						System.arraycopy(buf, 0, b, 0, bytesRead);
						isValid = false;
					}
					else
					{
						b = buf;
					}
					pc.createField("Shield vertex data:",false).setValue(b);
					
					int numTris = mis.readLittleInt();
					pc.createField("Shield triangles:",false).setValue(numTris);
					
					int trisSize = numTris*(12+3*4+3*4);
					b = null;
					buf = new byte[trisSize];
					bytesRead = mis.read(buf);
					
					r_len += 4 + bytesRead;
					
					if(bytesRead != trisSize)
					{
						b = new byte[bytesRead];
						System.arraycopy(buf, 0, b, 0, bytesRead);
						isValid = false;
					}
					else
					{
						b = buf;
					}
					pc.createField("Shield triangle data:",false).setValue(b);
					
					if(isValid)
					{
						pc.setValid(POFChunk.VALID_RW);
					}
					else
					{
						pc.setValid(POFChunk.VALID_READ);
					}
				}
				else if(c_id.equals("GPNT"))
				{
					int numGunpoints = mis.readLittleInt();
					r_len += 4;
					
					pc.createField("Primary Banks:",false).setValue(numGunpoints);
					for(int i = 0; i < numGunpoints; i++)
					{
						int numSlots = mis.readLittleInt();
						pc.createField("Pbank " + i + " slots:",false).setValue(numSlots);
						for(int j = 0; j < numSlots; j++)
						{
							vec3d filePnt = new vec3d(mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Pbank " + i + " slot " + j + " point:",true).setValue(filePnt);
							vec3d fileNrm = new vec3d(mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Pbank " + i + " slot " + j + " normal:",true).setValue(fileNrm);
						}
						
						r_len += 4 + 24*numSlots;
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("MPNT"))
				{
					int numGunpoints = mis.readLittleInt();
					r_len += 4;
					
					pc.createField("Secondary Banks:",false).setValue(numGunpoints);
					for(int i = 0; i < numGunpoints; i++)
					{
						int numSlots = mis.readLittleInt();
						pc.createField("Sbank " + i + " slots:",false).setValue(numSlots);
						for(int j = 0; j < numSlots; j++)
						{
							vec3d filePnt = new vec3d(mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Sbank " + i + " slot " + j + " point:",true).setValue(filePnt);
							vec3d fileNrm = new vec3d(mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Sbank " + i + " slot " + j + " normal:",true).setValue(fileNrm);
						}
						
						r_len += 4 + 24*numSlots;
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("DOCK"))
				{
					int numDockpoints = mis.readLittleInt();
					r_len += 4;
					
					pc.createField("Dockpoints:",false).setValue(numDockpoints);
					for(int i = 0; i < numDockpoints; i++)
					{
						int propsLen = mis.readLittleInt();
						pc.createField("Dock " + i + " properties:",true).setValue(mis.readString(propsLen));
						r_len += 4 + propsLen;
						
						int numSplinePaths = mis.readLittleInt();
						pc.createField("Dock " + i + " spline paths:",false).setValue(numSplinePaths);
						for(int j = 0; j < numSplinePaths; j++)
						{
							pc.createField("Dock " + i + " spline " + j + ":",true).setValue(mis.readLittleInt());
						}
						r_len += 4 + 4*numSplinePaths;
						
						
						int numSlots = mis.readLittleInt();
						pc.createField("Dock " + i + " slots:",false).setValue(numSlots);
						for(int j = 0; j < numSlots; j++)
						{
							pc.createField("Dock " + i + " slot " + j + " point:",true).setValue(
								mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Dock " + i + " slot " + j + " normal:",true).setValue(
								mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						}
						r_len += 4 + numSlots*24;
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("GLOW"))
				{
					int numBanks = mis.readLittleInt();
					pc.createField("Glowpoint banks:",false).setValue(numBanks);
					r_len += 4;
					
					for(int i = 0; i < numBanks; i++)
					{
						pc.createField("Bank " + i + " display time:",true).setValue(mis.readLittleInt());
						pc.createField("Bank " + i + " on time:",true).setValue(mis.readLittleInt());
						pc.createField("Bank " + i + " off time:",true).setValue(mis.readLittleInt());
						pc.createField("Bank " + i + " submodel parent:",true).setValue(mis.readLittleInt());
						pc.createField("Bank " + i + " LOD:",true).setValue(mis.readLittleInt());
						pc.createField("Bank " + i + " type:",true).setValue(mis.readLittleInt());
						int numPoints = mis.readLittleInt();
						pc.createField("Bank " + i + " points:",false).setValue(numPoints);
						r_len += 7*4;
						
						int propsLen = mis.readLittleInt();
						pc.createField("Bank " + i + " properties:",true).setValue(mis.readString(propsLen));
						r_len += 4 + propsLen;
						
						for(int j = 0; j < numPoints; j++)
						{
							pc.createField("Bank " + i + " point " + j + " point:",true).setValue(
								mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Bank " + i + " point " + j + " normal:",true).setValue(
								mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Bank " + i + " point " + j + " radius:",true).setValue(mis.readLittleFloat());
						}
						r_len += numPoints*28;
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("FUEL"))
				{
					boolean isValid = true;
					int numBanks = mis.readLittleInt();
					pc.createField("Thruster banks:",false).setValue(numBanks);
					r_len += 4;
					
					for(int i = 0; i < numBanks; i++)
					{
						int numPoints = mis.readLittleInt();
						pc.createField("Bank " + i + " points:",false).setValue(numPoints);
						r_len += 4;
						
						if(fileVersion >= 2117)
						{
							int propsLen = mis.readLittleInt();
							String propsString = mis.readString(propsLen);
							pc.createField("Bank " + i + " properties:",true).setValue(propsString);
							r_len += 4 + propsLen;
							
							if(propsLen != propsString.length())
								isValid = false;
						}
						
						for(int j = 0; j < numPoints; j++)
						{
							pc.createField("Bank " + i + " point " + j + " point:",true).setValue(
								mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							pc.createField("Bank " + i + " point " + j + " normal:",true).setValue(
								mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							r_len += 24;

							if(fileVersion > 2004)
							{
								pc.createField("Bank " + i + " point " + j + " radius:",true).setValue(mis.readLittleFloat());
								r_len += 4;
							}
						}
					}
					if(isValid)
						pc.setValid(POFChunk.VALID_RW);
					else
						pc.setValid(POFChunk.VALID_READ);
				}
				else if(c_id.equals("TGUN") || c_id.equals("TMIS"))
				{
					int numBanks = mis.readLittleInt();
					pc.createField("Turret banks:",false).setValue(numBanks);
					r_len += 4;
					
					for(int i = 0; i < numBanks; i++)
					{
						pc.createField("Bank " + i + " parent:",true).setValue(mis.readLittleInt());
						pc.createField("Bank " + i + " physical parent:",true).setValue(mis.readLittleInt());
						pc.createField("Bank " + i + " turret normal:",true).setValue(
							mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						r_len += 20;
						
						int numSlots = mis.readLittleInt();
						pc.createField("Bank " + i + " slots:",false).setValue(numSlots);
						for(int j = 0; j < numSlots; j++)
						{
							pc.createField("Bank " + i + " slot " + j + " point:",true).setValue(
							mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						}
						r_len += 4 + numSlots*12;
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("SPCL"))
				{
					int numSpecials = mis.readLittleInt();
					pc.createField("Special subobjects:",false).setValue(numSpecials);
					r_len += 4;
					
					for(int i = 0; i < numSpecials; i++)
					{
						int nameLen = mis.readLittleInt();
						pc.createField("Special " + i + " name:",true).setValue(mis.readString(nameLen));
						r_len += 4 + nameLen;
						
						int propsLen = mis.readLittleInt();
						pc.createField("Special " + i + " properties:",true).setValue(mis.readString(propsLen));
						r_len += 4 + propsLen;
						
						pc.createField("Special " + i + " point:",true).setValue(
							mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						pc.createField("Special " + i + " radius:",true).setValue(mis.readLittleFloat());
						r_len += 16;
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("TXTR"))
				{
					int numTextures = mis.readLittleInt();
					r_len += 4;
					
					pc.createField("Textures:",false).setValue(numTextures);
					
					for(int i = 0; i < numTextures; i++)
					{
						int stringLen = mis.readLittleInt();
						String texName = mis.readString(stringLen);
						pc.createField("Texture " + i + ":", true).setValue(texName);
						
						//Len + string itself
						r_len += 4+stringLen;
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("IDATA"))
				{
					byte buf[] = new byte[c_len];
					byte b[] = null;
					
					int bytesRead = mis.read(buf);
					r_len += bytesRead;
					
					if(bytesRead != c_len)
					{
						b = new byte[bytesRead];
						System.arraycopy(buf, 0, b, 0, bytesRead);
						pc.setValid(POFChunk.VALID_READ);
					}
					else
					{
						b = buf;
						pc.setValid(POFChunk.VALID_RW);
					}
					pc.createField("Model data:", false).setValue(b);
				}
				else if(c_id.equals("INFO") || c_id.equals("PINF"))
				{
					byte buf[] = new byte[c_len];
					byte b[] = null;
					
					int bytesRead = mis.read(buf);
					r_len += bytesRead;
					
					if(bytesRead != c_len)
					{
						b = new byte[bytesRead];
						System.arraycopy(buf, 0, b, 0, bytesRead);
						pc.setValid(POFChunk.VALID_READ);
					}
					else
					{
						b = buf;
						pc.setValid(POFChunk.VALID_RW);
					}
					pc.createField("Info:", false).setValue(b);
					//WMC - This is a good assumption, but
					//doesn't work too well with this interface.
					//I don't have a good way of handling multiple lines.
					/*
					String fileInfo = mis.readString(c_len);
					r_len += fileInfo.length();
					
					String lines[] = fileInfo.split("\r|\n|(\r\n)");
					for(int i = 0; i < lines.length; i++)
					{
						pc.createField(i + ":",false).setValue(lines[i]);
					}
					pc.setValid(POFChunk.VALID_READ);
					*/
				}
				else if(c_id.equals("GRID"))
				{
					//Do nothing.
					pc.setValid(POFChunk.VALID_NONE);
				}
				else if(c_id.equals("PATH"))
				{
					boolean isValid = true;
					int numPaths = mis.readLittleInt();
					pc.createField("Paths:",false).setValue(numPaths);
					r_len += 4;
					
					//WMC - error checking
					if(numPaths > 1000)
					{
						isValid = false;
					}
					else
					{
						for(int i = 0; i < numPaths; i++)
						{
							int nameLen = mis.readLittleInt();
							pc.createField("Path " + i + " name:",true).setValue(mis.readString(nameLen));
							r_len += 4+nameLen;
							if(fileVersion > 2002)
							{
								int parentNameLen = mis.readLittleInt();
								pc.createField("Path " + i + " parent name:",true).setValue(mis.readString(parentNameLen));
								r_len += 4 + parentNameLen;
							}
							
							int numVerts = mis.readLittleInt();
							pc.createField("Path " + i + " vertices:",false).setValue(numVerts);
							r_len += 4;
							
							//WMC - error checking
							if(numVerts > 1000)
							{
								isValid = false;
								break;
							}
							for(int j = 0; j < numVerts; j++)
							{
								pc.createField("Path " + i + " vertex " + j + " point:",true).setValue(
									mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
								pc.createField("Path " + i + " vertex " + j + " radius:",true).setValue(mis.readLittleFloat());
								r_len += 16;
								int numTurrets = mis.readLittleInt();
								pc.createField("Path " + i + " vertex " + j + " turrets:",false).setValue(numTurrets);
								r_len += 4;
								
								//WMC - error checking
								if(numTurrets > 1000)
								{
									isValid = false;
									break;
								}
								for(int k = 0; k < numTurrets; k++)
								{
									int turretID = mis.readLittleInt();
									pc.createField("Path " + i + " vertex " + j + " turret " + k + " ID:",true).setValue(turretID);
								}
								r_len += numTurrets*4;
							}
						}
					}
					if(isValid)
						pc.setValid(POFChunk.VALID_RW);
					else
						pc.setValid(POFChunk.VALID_READ);
				}
				else if(c_id.equals("EYE "))
				{
					int numEyes = mis.readLittleInt();
					pc.createField("Eyepoints:",false).setValue(numEyes);
					
					for(int i = 0; i < numEyes; i++)
					{
						pc.createField("Eyepoint " + i + " parent:",true).setValue(mis.readLittleInt());
						vec3d pnt = new vec3d(mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat());
						vec3d nrm = new vec3d(mis.readLittleFloat(), mis.readLittleFloat(), mis.readLittleFloat());
						pc.createField("Eyepoint " + i + " point:",true).setValue(pnt);
						pc.createField("Eyepoint " + i + " normal:",true).setValue(nrm);
					}
					
					r_len += 4 + numEyes*28;
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("INSG"))
				{
					int numInsigniae = mis.readLittleInt();
					pc.createField("Insigniae:",false).setValue(numInsigniae);
					r_len += 4;
					
					for(int i = 0; i < numInsigniae; i++)
					{
						pc.createField("Insignia " + i + " LOD:",true).setValue(mis.readLittleInt());
						
						int numFaces = mis.readLittleInt();
						pc.createField("Insignia " + i + " faces:",false).setValue(numFaces);
		
						int numVerts = mis.readLittleInt();
						pc.createField("Insignia " + i + " vertices:",false).setValue(numVerts);
						
						r_len += 12;
						
						for(int j = 0; j < numVerts; j++)
						{
							pc.createField("Insignia " + i + " vertex " + j + ":",true).setValue(
								mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						}
						
						r_len += numVerts*12;
						
						pc.createField("Insignia " + i + " offset:",true).setValue(
							mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
							
						r_len += 12;
							
						for(int j = 0; j < numFaces; j++)
						{
							for(int k = 0; k < 3; k++)
							{
								pc.createField("Insignia " + i + " face " + j + " vertex " + k + ":",true).setValue(mis.readLittleInt());
								pc.createField("Insignia " + i + " face " + j + " vertex " + k + " U:",true).setValue(mis.readLittleFloat());
								pc.createField("Insignia " + i + " face " + j + " vertex " + k + " V:",true).setValue(mis.readLittleFloat());
								r_len += 12;
							}
						}
					}
					pc.setValid(POFChunk.VALID_RW);
				}
				else if(c_id.equals("ACEN"))
				{
					pc.createField("Autocenter position:", true).setValue(
						mis.readLittleFloat(),mis.readLittleFloat(),mis.readLittleFloat());
						r_len += 12;
					pc.setValid(POFChunk.VALID_RW);
				}
				else
				{
					MajaApp.displayWarning("Unknown chunk '" + c_id + "' - skipping.");
				}
				
				if(r_len != c_len)
				{
					MajaApp.displayWarning(c_id + ": r_len = " + r_len + "; c_len = " + c_len + ".");
				}
				
				//We read too much somehow
				if(r_len > c_len)
				{
					MajaApp.displayError("Error reading '" + c_id + "' in POF file");
					return r_len;
				}
				
				//Skip anything leftover
				r_len += mis.skip(c_len - r_len);
			}
			catch(java.io.IOException exc)
			{
				return r_len;
			}
			
			return r_len;
		}
		
		public boolean save(MajaOutputStream mos)
		{
			if(mos == null)
				return false;
			
			if(!this.canSave())
			{
				MajaApp.displayError("Couldn't save POF - one or more chunks may have be unknown or misread.");
				return false;
			}
			
			try
			{
				//Write header/version
				mos.writeString(_header.getText(), 4);
				_version.write(mos);
				
				//Now do chunks
				for(int i = 0; i < _chunks.size(); i++)
				{
					_chunks.get(i).write(mos);
				}
			}
			catch(java.io.IOException ex)
			{
				MajaApp.displayException(ex, "Error writing '" + _targetEntry.getName() + "'");
				return false;
			}
			
			return true;
		}
		
		//********************UndoableEditListener********************//
		public void undoableEditHappened(javax.swing.event.UndoableEditEvent e)
		{
			if(_targetEntry != null)
			{
				MajaComponentEditor.this.setTargetModified(true);
			}
		}
	}
	
	public class ImageEditor extends BasicEditor
	{
		private static final long serialVersionUID = -5606656706931708979L;
		ImageEditorCanvas _canvas;

		//private static ImageReaderSpi _readerPlugins[] = null
		
		public ImageEditor()
		{
			_canvas = new ImageEditorCanvas();
			this.add(_canvas, java.awt.BorderLayout.CENTER);
		}
		
		public boolean canSave()
		{
			return false;
		}
		
		public boolean load(MajaInputStream mis, long misLength)
		{
			return _canvas.loadImage(mis);
		}
		
		public java.awt.Dimension getPreferredSize()
		{
			return _canvas.getPreferredSize();
		}
		
		public class ImageEditorCanvas extends java.awt.Component
		{
			private static final long serialVersionUID = -2559614086397409975L;
			private BufferedImage _currentImage;
			
			public boolean loadImage(MajaInputStream mis)
			{
				try
				{
					_currentImage = ImageIO.read(mis);
				}
				catch (java.io.IOException ex)
				{
					MajaApp.displayException(ex, "Exception reading image");
					return false;
				}
				catch(java.lang.Exception exc)
				{
					MajaApp.displayException(exc, "Exception processing image");
					return false;
				}
				
				return true;
			}

			public void paint(java.awt.Graphics g)
			{
				if(_currentImage != null)
				{
					g.drawImage(_currentImage, 0, 0, null);
				}
			}
			
			public java.awt.Dimension getPreferredSize()
			{
				if(_currentImage == null)
				{
					return new java.awt.Dimension(100, 100);
				}
				else
				{
					return new java.awt.Dimension(_currentImage.getWidth(null), _currentImage.getHeight(null));
				}
			}
		}
		
		
	}
	
	public static class TGAReaderSpi extends javax.imageio.spi.ImageReaderSpi
	{
		public TGAReaderSpi()
		{
			super(	"Maja",
				"1.0",
				new String[] {"TGA"},
				new String[] {"tga"},
				new String[] {"image/tga", "image/x-tga"},
				"MajaComponentEditor.TGAReaderSpi.TGAReader",
				javax.imageio.spi.ImageReaderSpi.STANDARD_INPUT_TYPE,
				null,
				false,
				null,
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null);
		}
		
		public boolean canDecodeInput(Object input) throws java.io.IOException
		{
			if(!(input instanceof ImageInputStream))
				return false;
			
			ImageInputStream is = (ImageInputStream) input;
			
			byte[] header = new byte[18 + 255];
			try
			{
				is.mark();
				is.readFully(header);
				is.reset();
			}
			catch(java.io.IOException ex)
			{
				return false;
			}
			
			int idFieldSize = (header[0] & 255);
			
			//0 for no colormap, 1 for a colormap
			int colorMapType = (header[1] & 255);
			
			//1: Color-mapped image
			//2: Unmapped RGB image
			//9: RLE Color-mapped image
			//10: RLE RGB image
			int imageType = (header[2] & 255);
			
			//*****5 BYTES COLORMAP SPECIFICATION
			
			//Technically a short. Integer index of where the colormap starts. (LITTLE ENDIAN)
			int colorMapOrigin = ((header[4] & 255) << 8) | (header[3] & 255);
			
			//Length of color map ((short) ints again?) LITTLE ENDIAN
			int colorMapLength = ((header[6] & 255) << 8) | (header[5] & 255);
			
			//16/24/32 depending on image
			int colorMapBitsPerEntry = (header[7] & 255);
			
			//*****10 BYTES IMAGE SPECIFICATION
			//These are for the lower-left corner O_o
			int xOrigin = ((header[9] & 255) << 8) | (header[8] & 255);
			int yOrigin = ((header[11] & 255) << 8) | (header[10] & 255);
			int width = ((header[13] & 255) << 8) | (header[12] & 255);
			int height = ((header[15] & 255) << 8) | (header[14] & 255);
			
			//16/24/32
			int pixelSize = (header[16] & 255);
			
			//TGA16 - 0/1
			//TGA24 - 0
			//TGA32 - 8
			int pixelAttributeSize = (header[17] & 7);
			
			//TRUE - upper left origin
			//FALSE - lower left origin
			boolean upperLeftOrigin = (header[17] & (1>>4)) == 1;
			
			//00(0) - No interleaving
			//01(1) - two way even/odd interleaving
			//10(2) - four way interleaving
			int dataInterleaving = (header[17] & ((1>>5) | (1>>6)));
			
			if(imageType != 2)
				return false;
			
			return true;
		}
		
		public ImageReader createReaderInstance(Object extension) throws java.io.IOException
		{
			//Pass the SPI along
			return new TGAReader(this);
		}
		
		public String getDescription(java.util.Locale locale)
		{
			return "Maja TGA Reader v1.0";
		}
		
		public class TGAReader extends ImageReader
		{
			int h_idFieldSize;
			int h_colorMapType;
			int h_imageType;
			int h_colorMapOrigin;
			int h_colorMapLength;
			int h_colorMapBitsPerEntry;
			int h_xOrigin;
			int h_yOrigin;
			int h_width;
			int h_height;
			int h_pixelSize;
			int h_pixelAttributeSize;
			boolean h_upperLeftOrigin;
			int h_dataInterleaving;

			private ImageInputStream _stream = null;
			private boolean _gotHeader = false;
			
			public TGAReader(javax.imageio.spi.ImageReaderSpi originalProvider)
			{
				super(originalProvider);
			}
			
			public int getHeight(int idx) throws IIOException
			{
				this.checkIndex(idx);
				this.readHeader();
				return h_height;
			}
			
			public javax.imageio.metadata.IIOMetadata getImageMetadata(int idx)
			{
				return null;
			}
			
			public java.util.Iterator<ImageTypeSpecifier> getImageTypes(int idx) throws IIOException
			{
				//this.checkIndex(idx);
				//this.readHeader();
				
				java.util.List<ImageTypeSpecifier> l = new java.util.ArrayList<ImageTypeSpecifier>();
				
				//Only support TYPE_INT_ARGB
				l.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
				
				return l.iterator();
			}
			
			public int getNumImages(boolean allowSearch)
			{
				return 1;
			}
			
			public javax.imageio.metadata.IIOMetadata getStreamMetadata()
			{
				return null;
			}
			
			public int getWidth(int idx) throws IIOException
			{
				this.checkIndex(idx);
				this.readHeader();
				return h_width;
			}
			
			public BufferedImage read(int idx, ImageReadParam param) throws java.io.IOException
			{
				this.checkIndex(idx);
				this.readHeader();
				
				java.awt.Rectangle sourceRegion = ImageReader.getSourceRegion(param, h_width, h_height);
				
				//*****Defaults+variables
				//Non-scaled
				int sourceXSubsampling = 1;
				int sourceYSubsampling = 1;
				
				//All bands
				int[] sourceBands = null;
				int[] destinationBands = null;
				
				//Upper-left corner
				java.awt.Point destinationOffset = new java.awt.Point(0, 0);
				
				if(param != null)
				{
					sourceXSubsampling = param.getSourceXSubsampling();
					sourceYSubsampling = param.getSourceYSubsampling();
					sourceBands = param.getSourceBands();
					destinationBands = param.getDestinationBands();
					destinationOffset = param.getDestinationOffset();
				}
				
				//*****Get destination buffer
				BufferedImage dst = ImageReader.getDestination(param, getImageTypes(0), h_width, h_height);
				ImageReader.checkReadParamBandSettings(param, 4, dst.getSampleModel().getNumBands());
				
				//Band buffer
				WritableRaster wrSrc = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, h_width, 1, 4, new java.awt.Point(0, 0));
				
				byte [][] banks;
				banks = ((DataBufferByte) wrSrc.getDataBuffer()).getBankData();
				
				//Destination buffer
				WritableRaster wrDst = dst.getRaster();
				
				//Get target pixels
				int dstMinX = wrDst.getMinX();
				int dstMaxX = dstMinX + wrDst.getWidth() - 1;
				int dstMinY = wrDst.getMinY();
				int dstMaxY = dstMinY + wrDst.getWidth() - 1;
				
				//Create child raster w/ available source bands
				if(sourceBands != null)
					wrSrc = wrSrc.createWritableChild(0, 0, h_width, 1, 0, 0, sourceBands);
				
				//Create child raster with available dest bands
				if(destinationBands != null)
					wrDst = wrDst.createWritableChild(0, 0, wrDst.getWidth(), wrDst.getHeight(), 0, 0, destinationBands);
				
				byte line[] = new byte[h_width * h_pixelSize/8];
				//int [] pixel = wrSrc.getPixel(0, 0, (int[]) null);
				int srcY = 0;
				for(srcY = 0; srcY < h_height; srcY++)
				{
					_stream.readFully(line);

					if(	(srcY < sourceRegion.y) ||
						(srcY >= sourceRegion.y + sourceRegion.height) ||
						(((srcY - sourceRegion.y) % sourceYSubsampling) != 0))
					{
						continue;
					}
					
					int dstY = destinationOffset.y + (srcY - sourceRegion.y)/sourceYSubsampling;
					
					//We don't need this one
					if(dstY < dstMinY)
						continue;
					
					//We're done!
					if(dstY > dstMaxY)
						break;
					
					for(int srcX = destinationOffset.x; srcX < destinationOffset.x + h_width; srcX++)
					{
						if((srcX % sourceXSubsampling) != 0)
							continue;
						
						int dstX = destinationOffset.x + (srcX - sourceRegion.x)/sourceXSubsampling;
						
						//Don't need to write it
						if(dstX < dstMinX)
							continue;
						
						//We're done!
						if(dstX < dstMaxX)
							break;
						
						int[] pixel = new int[1];
						if(h_pixelSize == 24)
						{
							pixel[0] = 0;
							pixel[0] += (255 & 255) << 24;			//Alpha
							pixel[0] += (line[srcX*3+2] & 255);		//Blue
							pixel[0] += (line[srcX*3+1] & 255) << 8;	//Green
							pixel[0] += (line[srcX*3] & 255) << 16;		//Red
							
							/*
							pixel[0] = (255 & 255);
							pixel[1] = (line[srcX*3+2] & 255);
							pixel[2] = (line[srcX*3+1] & 255);
							pixel[3] = (line[srcX*3] & 255);
							*/
							
						}
						else if(h_pixelSize == 32)
						{
							pixel[0] = 0;
							pixel[0] += (line[srcX*4+3] & 255) << 24;	//Alpha
							pixel[0] += (line[srcX*4+2] & 255);		//Blue
							pixel[0] += (line[srcX*4+1] & 255) << 8;	//Green
							pixel[0] += (line[srcX*4] & 255) << 16;		//Red
						}
						else
						{
							MajaApp.displayError("Unsupported h_PixelSize");
						}
						
						wrDst.setPixel(dstX, dstY, pixel);
					}
				}
				
				return dst;
			}
			
			public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata)
			{
				super.setInput(input, seekForwardOnly, ignoreMetadata);
				
				if(input == null)
				{
					_stream = null;
					return;
				}
				
				if(input instanceof ImageInputStream)
					_stream = (ImageInputStream) input;
				else
					throw new IllegalArgumentException("ImageInputStream required.");
			}
			
			private void checkIndex(int imageIndex)
			{
				if(imageIndex != 0)
					throw new IndexOutOfBoundsException();
			}
			
			private void readHeader() throws IIOException
			{
				if(_gotHeader)
					return;

				if(_stream == null)
					throw new IllegalStateException("No input stream given.");

				byte[] header = new byte[18];
				try
				{
					_stream.readFully(header);
				}
				catch(java.io.IOException ex)
				{
					throw new IIOException("Unable to fully read TGA header", ex);
				}
				
				h_idFieldSize = (header[0] & 255);
				
				//0 for no colormap, 1 for a colormap
				h_colorMapType = (header[1] & 255);
				
				//1: Color-mapped image
				//2: Unmapped RGB image
				//9: RLE Color-mapped image
				//10: RLE RGB image
				h_imageType = (header[2] & 255);
				
				//*****5 BYTES COLORMAP SPECIFICATION
				
				//Technically a short. Integer index of where the colormap starts. (LITTLE ENDIAN)
				h_colorMapOrigin = ((header[4] & 255) << 8) | (header[3] & 255);
				
				//Length of color map ((short) ints again?) LITTLE ENDIAN
				h_colorMapLength = ((header[6] & 255) << 8) | (header[5] & 255);
				
				//16/24/32 depending on image
				h_colorMapBitsPerEntry = (header[7] & 255);
				
				//*****10 BYTES IMAGE SPECIFICATION
				//These are for the lower-left corner O_o
				h_xOrigin = ((header[9] & 255) << 8) | (header[8] & 255);
				h_yOrigin = ((header[11] & 255) << 8) | (header[10] & 255);
				h_width = ((header[13] & 255) << 8) | (header[12] & 255);
				h_height = ((header[15] & 255) << 8) | (header[14] & 255);
				
				//16/24/32
				h_pixelSize = (header[16] & 255);
				
				//TGA16 - 0/1
				//TGA24 - 0
				//TGA32 - 8
				h_pixelAttributeSize = (header[17] & 7);
				
				//TRUE - upper left origin
				//FALSE - lower left origin
				h_upperLeftOrigin = (header[17] & (1>>4)) == 1;
				
				//00(0) - No interleaving
				//01(1) - two way even/odd interleaving
				//10(2) - four way interleaving
				h_dataInterleaving = (header[17] & ((1>>5) | (1>>6)));
				
				
				String message = "ID Field size: " + h_idFieldSize;
				message += "\nColor map type: " + h_colorMapType;
				message += "\nimageType: " + h_imageType;
				message += "\nColor map origin: " + h_colorMapOrigin;
				message += "\nColor map length: " + h_colorMapLength;
				message += "\nColor map bits per entry: " + h_colorMapBitsPerEntry;
				message += "\nX Origin: " + h_xOrigin;
				message += "\nY Origin: " + h_yOrigin;
				message += "\nwidth: " + h_width;
				message += "\nheight: " + h_height;
				message += "\npixel size: " + h_pixelSize;
				message += "\npixel attribute size: " + h_pixelAttributeSize;
				message += "\nupper left origin: " + h_upperLeftOrigin;
				message += "\ndata interleaving: " + h_dataInterleaving;
				JOptionPane.showMessageDialog(null, message, "TGA details", JOptionPane.INFORMATION_MESSAGE);
				
			}
		}
	}
	
	public static class PCXReaderSpi extends javax.imageio.spi.ImageReaderSpi
	{
		public PCXReaderSpi()
		{
			super("Maja",
				"1.0",
				new String[] {"PCX"},
				new String[] {"pcx"},
				new String[] {"image/x-pcx"},
				"MajaComponentEditor.ImageEditor.PCXReaderSpi.PCXReader",
				javax.imageio.spi.ImageReaderSpi.STANDARD_INPUT_TYPE,
				null,
				false,
				null,
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null);
		}
		
		public boolean canDecodeInput(Object input) throws java.io.IOException
		{
			if(!(input instanceof ImageInputStream))
				return false;
				
			ImageInputStream is = (ImageInputStream) input;
			
			//Read the header.
			byte[] header = new byte[128];
			try
			{
				//Use mark and rest functions so that this file
				//may be safely passed on to other readers.
				
				is.mark();
				is.readFully(header);
				is.reset();
			}
			catch(java.io.IOException ex)
			{
				return false;
			}
			
			//Manufacturer
			if(header[0] != 10)
				return false;
			
			//Version
			if(header[1] != 5)
				return false;
				
			//Encoding
			if(header[2] != 1)
				return false;
			
			//BPP
			if(header[3] != 1 && header[3] != 4 && header[3] != 8)
				return false;
			
			//NPlanes
			if(header[65] != 1 && header[65] != 3)
				return false;
			
			//NPlanes + BPP verification
			if((header[3] == 1 || header[3] == 4) && header[65] == 3)
				return false;
			
			//BytesPerLine
			//Should always be even
			if((header[66] & 1) == 1)
				return false;
			
			//Looks good.
			return true;
		}
		
		public ImageReader createReaderInstance(Object extension) throws java.io.IOException
		{
			//Pass the SPI along
			return new PCXReader(this);
		}
		
		public String getDescription(java.util.Locale locale)
		{
			return "Maja PCX Reader, based on 'PCX Meets Image I/O: Creating an Image-Reading Java Plug-in' by Jeff Friesen";
		}
		
		public class PCXReader extends ImageReader
		{
			private int width;
			private int height;
			private int bitsPerPixel;
			private int nPlanes;
			private int bytesPerLine;
			private int scanLineLength;

			private byte[] palette = new byte[256*3];
			private byte[][] scanLines = null;
			
			private ImageInputStream _stream;
			private boolean _gotHeader = false;
			
			public PCXReader(javax.imageio.spi.ImageReaderSpi originalProvider)
			{
				super(originalProvider);
			}
			
			public void checkIndex(int imageIndex)
			{
				if(imageIndex != 0)
					throw new IndexOutOfBoundsException();
			}
			
			public int getHeight(int idx) throws IIOException
			{
				this.checkIndex(idx);
				this.readHeader();
				return height;
			}
			
			public javax.imageio.metadata.IIOMetadata getImageMetadata(int idx)
			{
				return null;
			}
			
			public java.util.Iterator<ImageTypeSpecifier> getImageTypes(int idx) throws IIOException
			{
				this.checkIndex(idx);
				this.readHeader();
				
				java.util.List<ImageTypeSpecifier> l;
				l = new java.util.ArrayList<ImageTypeSpecifier>();
				
				//Only support TYPE_INT_RGB
				l.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
				
				return l.iterator();
			}
			
			public int getNumImages(boolean allowSearch)
			{
				return 1;
			}
			
			public javax.imageio.metadata.IIOMetadata getStreamMetadata()
			{
				//We don't support this.
				return null;
			}
			
			public int getWidth(int idx) throws IIOException
			{
				this.checkIndex(idx);
				this.readHeader();
				return width;
			}
			
			public BufferedImage read(int idx, ImageReadParam param) throws java.io.IOException
			{
				this.checkIndex(idx);
				this.readHeader();
				
				java.awt.Rectangle sourceRegion = ImageReader.getSourceRegion(param, width, height);
				
				//Defaults
				//Non-scaled image
				int sourceXSubsampling = 1;
				int sourceYSubsampling = 1;
				
				//All bands
				int[] sourceBands = null;
				int[] destinationBands = null;
				
				//Start at upper-left corner
				java.awt.Point destinationOffset = new java.awt.Point(0, 0);
				
				if(param != null)
				{
					sourceXSubsampling = param.getSourceXSubsampling();
					sourceYSubsampling = param.getSourceYSubsampling();
					sourceBands = param.getSourceBands();
					destinationBands = param.getDestinationBands();
					destinationOffset = param.getDestinationOffset();
				}
				
				//Create the destination buffer
				BufferedImage dst = getDestination(param, getImageTypes(0), width, height);
				
				ImageReader.checkReadParamBandSettings(param, 3, dst.getSampleModel().getNumBands());
				
				//Band buffer
				WritableRaster wrSrc = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, width, 1, 3, new java.awt.Point(0, 0));
				
				byte [][] banks;
				banks = ((DataBufferByte) wrSrc.getDataBuffer()).getBankData();
				
				//Destination buffer
				WritableRaster wrDst = dst.getRaster();
				
				//Get target pixels
				int dstMinX = wrDst.getMinX();
				int dstMaxX = dstMinX + wrDst.getWidth() - 1;
				int dstMinY = wrDst.getMinY();
				int dstMaxY = dstMinY + wrDst.getWidth() - 1;
				
				//Create child raster w/ available source bands
				if(sourceBands != null)
					wrSrc = wrSrc.createWritableChild(0, 0, width, 1, 0, 0, sourceBands);
				
				//Create child raster w/ available dest bands
				if(destinationBands != null)
					wrDst = wrDst.createWritableChild(0, 0, wrDst.getWidth(), wrDst.getHeight(), 0, 0, destinationBands);
				
				int srcY = 0;

				//byte[] scanLine = new byte[scanLineLength];
				int [] pixel = wrSrc.getPixel(0, 0, (int[]) null);
				
				for(srcY = 0; srcY < height; srcY++)
				{
					copyScanLineToBanks(banks, scanLines[srcY]);
					
					//Skip rows outside of our destination or
					//are not shown due to subsampling
					if(	(srcY < sourceRegion.y) ||
						(srcY >= sourceRegion.y + sourceRegion.height) ||
						(((srcY - sourceRegion.y) % sourceYSubsampling) != 0))
					{
						continue;
					}
					
					int dstY = destinationOffset.y + (srcY - sourceRegion.y)/sourceYSubsampling;
					
					//We don't need this one
					if(dstY < dstMinY)
						continue;
					
					//We're done!
					if(dstY > dstMaxY)
						break;
					
					//Handle x subsampling
					for(int srcX = sourceRegion.x; srcX < sourceRegion.x + sourceRegion.width; srcX++)
					{
						//We are skipping this x							
						if(((srcX - sourceRegion.x) % sourceXSubsampling) != 0)
							continue;
							
						int dstX = destinationOffset.x + (srcX - sourceRegion.x)/sourceXSubsampling;
						
						//Don't need to write this one
						if(dstX < dstMinX)
							continue;
						
						//We're done with this row
						if(dstX > dstMaxX)
							break;
						
						wrSrc.getPixel(srcX, 0, pixel);
						wrDst.setPixel(dstX, dstY, pixel);
					}
				}
				
				return dst;
			}
			
			private void readHeader() throws IIOException
			{
				if(_gotHeader)
					return;
				
				if(input == null)
					throw new IllegalStateException("No input stream given.");
				
				byte header[] = new byte[128];
				try
				{
					_stream.readFully(header);
				}
				catch(java.io.IOException ex)
				{
					throw new IIOException("Unable to fully read PCX header", ex);
				}
				
				if(header[0] != 10)
					throw new IIOException("Incorrect PCX Manufacturer value");
				
				if(header[1] != 5)
					throw new IIOException("Unsupported PCX Version");
				
				if(header[2] != 1)
					throw new IIOException("Unsupported PCX encoding");
				
				//Check nPlanes
				if(header[65] != 1 && header[65] != 3)
					throw new IIOException("Bad PCX NPlanes value");

				if((header[3] == 1 || header[3] == 4) && header[65] == 3)
					throw new IIOException("PCX Bits/NPlanes combo _stream unsupported");
					
				if((header[66] & 1) == 1)
					throw new IIOException("Unsupported (odd) PCX BytesPerLine value");
				
				//bitsPerPixel
				bitsPerPixel = new Byte(header[3]).intValue();
				
				//width + height
				//Xmin, Ymin, Xmax, Ymax
				//Convert from short-->int first.
				int xmin = ((header[5] & 255) << 8) | (header[4] & 255);
				int ymin = ((header[7] & 255) << 8) | (header[6] & 255);
				int xmax = ((header[9] & 255) << 8) | (header[8] & 255);
				int ymax = ((header[11] & 255) << 8) | (header[10] & 255);
				
				width = xmax - xmin + 1;
				height = ymax - ymin + 1;
				
				nPlanes = new Byte(header[65]).intValue();
				
				bytesPerLine = ((header[67] & 255) << 8) | (header[66] & 255);
				
				scanLineLength = bytesPerLine * nPlanes;
				
				scanLines = new byte[height][scanLineLength];
				
				//Read...everything.
				for(int y = 0; y < height; y++)
				{
					try
					{
						
						this.readScanLine(scanLines[y]);
					}
					catch(java.io.IOException ex)
					{
						throw new IIOException("Error reading line " + y + " - " + ex.getMessage(), ex);
					}
				}

				if(bitsPerPixel == 8 && nPlanes == 1)
				{
					try
					{
						//_stream.mark();
						
						//Go to EOF
						//_stream.seek(_stream.length());
						
						//Go to start of the VGA palette
						//_stream.seek(_stream.getStreamPosition() - 769);
						
						//Read VGA palette into palette array
						if(_stream.readUnsignedByte() != 12)
							throw new java.io.IOException();
						
						_stream.read(palette);
						//_stream.reset();
					}
					catch(java.io.IOException ex)
					{
						throw new IIOException("No VGA palette present in PCX, or read error encountered");
					}
				}
				else
				{
					System.arraycopy(header, 16, palette, 0, 48);
				}

				_gotHeader = true;
			}
			
			private void copyScanLineToBanks(byte[][] banks, byte[] scanLine)
			{
				if(bitsPerPixel == 1 && nPlanes == 1)
				{
					//2 colors
					//0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01
					byte[] mask = {-128, 64, 32, 16, 8, 4, 2, 1};
					
					for(int i = 0; i < width; i++)
					{
						byte nByte = scanLine[i >> 3];
						//In other words, check bits 1,2,4. This is so
						//the loop can run normally, with 8 groups per byte.
						boolean bitSet = ((nByte & mask[i & 7]) != 0) ? true : false;
						banks[0][i] = palette[!bitSet ? 0 : 3];
						banks[1][i] = palette[!bitSet ? 1 : 4];
						banks[2][i] = palette[!bitSet ? 2 : 5];
					}
				}
				else if(bitsPerPixel == 4 && nPlanes == 1)
				{
					//16 colors
					for(int i = 0; i < width; i++)
					{
						int index = 0;
						//First half of byte
						if((i & 1) == 0)
							index = (scanLine[i>>1] >> 4) & 15;
						//Second half of byte
						else
							index = scanLine[i>>1] & 15;
						
						//Skip to the right index
						index *= 3;
						
						banks[0][i] = palette[index++];
						banks[1][i] = palette[index++];
						banks[2][i] = palette[index];
					}
				}
				else if(bitsPerPixel == 8 && nPlanes == 1)
				{
					//256 colors
					for(int i = 0; i < width; i++)
					{
						int index = scanLine[i] & 255;
						
						//Skip to the right index
						index *= 3;
						
						banks[0][i] = palette[index++];
						banks[1][i] = palette[index++];
						banks[2][i] = palette[index++];
					}
				}
				else
				{
					//R
					System.arraycopy(scanLine, 0, banks[0], 0, width);
					//G
					System.arraycopy(scanLine, bytesPerLine, banks[1], 0, width);
					//B
					System.arraycopy(scanLine, bytesPerLine, banks[2], 0, width);
				}
			}
			
			private void readScanLine(byte[] scanLine) throws java.io.IOException
			{
				int index = 0;
				
				do
				{
					int rleInfo = _stream.readUnsignedByte();
					if((rleInfo & 0xc0) == 0xc0)
					{
						//Multiple pixels
						int count = rleInfo & 0x3f;
						
						if(count == 0)
							throw new java.io.IOException("PCX: Empty run count detected");
						
						if(index+count > scanLineLength)
							throw new java.io.IOException("PCX: Scanline overflow");
						
						//Read the real pixel and copy
						rleInfo = _stream.readUnsignedByte();
						for(int i = 0; i < count; i++)
							scanLine[index++] = (byte) rleInfo;
					}
					else
					{
						scanLine[index++] = (byte) rleInfo;
					}
				} while(index < scanLineLength);
			}
			
			public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata)
			{
				super.setInput(input, seekForwardOnly, ignoreMetadata);
				
				if(input == null)
				{
					_stream = null;
					return;
				}
				
				if(input instanceof ImageInputStream)
					_stream = (ImageInputStream) input;
				else
					throw new IllegalArgumentException("ImageInputStream required.");
			}
		}
	}
}
