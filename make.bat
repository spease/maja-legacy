@echo off
@echo ********************BEGINNING BUILD********************
@echo MAJA EXPRESS DEBUG MODE
javac -classpath src -s src -d bin -target 1.5 src/Maja/*.java src/org/apache/tools/bzip2/*.java -g:none
@echo ********************COMPLETED BUILD********************
@echo ********************BEGINNING JAR BUILD********************
jar cfm bin/Maja.jar src/Manifest.txt -C bin .
@echo ********************COMPLETED JAR BUILD********************
@echo TIP: Don't forget to turn off debug info for release!!!! (-g:none)
