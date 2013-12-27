===== CHANGELOG
0.85->0.86 (Unofficial release)
- Fix bug where renaming tag would not rename head entry
- Add numbers to component title bars
- Added a POF Editor (does not work with ZIP files)
- Added the ability to open files in the editor independently
- Added "Save As" button to editor
0.82->0.85
- Fixed connected/sync status
- Added ability to read and write zip files
- Added ability to load items from the command line using syntax "java -jar MajaExpress085.jar %1"
- Added several more supported text files to the editor
- Reworked backend to provide for more flexible source entry/project entry management
0.81->0.82
- Fixed entry details dialog to show entry size using MB/kB/B
- Added ZIP import/export ability
- Cleaned up DetailsDialog and MajaHandlerManager source
- Updated "Export..." entries tree menu option to reflect type of item being exported.
- Added "Export Package..." menu item
- Added lockout for saving with retail VP names

===== TODO
- Optimize import/export of files from the same source
- Possibly integrate MajaPackageHandler with MajaSource
- Run GUI as a separate thread entirely

===== DEBUG COMMAND

javac -classpath source -s source -d classes -target 1.5 source/Maja/MajaBranch.java 2> errors.txt

===== MISCELLANIA:
Maja was started at about Sunday, April 22, 2007, 1:32:21 PM.