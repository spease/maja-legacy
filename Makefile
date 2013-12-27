DIR_BIN=bin
DIR_DOC=doc
DIR_SRC=src

PROJECT_NAME=Maja
JAR_FILEPATH=$(DIR_BIN)/$(PROJECT_NAME).jar
MANIFEST_FILEPATH=$(DIR_SRC)/Manifest.txt

JAVA_VERSION=1.7

SOURCES=$(shell find $(DIR_SRC) -iname "*.java")
SOURCE_CLASSES=$(SOURCES:%.java=%.class)
CLASSES=$(SOURCE_CLASSES:$(DIR_SRC)%=$(DIR_BIN)%)

all: jar

clean:
	rm -rf --preserve-root $(DIR_BIN)
	rm -rf --preserve-root $(DIR_DOC)

%.class:
	mkdir -p $(DIR_BIN)
	javac -classpath $(DIR_SRC) -s $(DIR_SRC) -d $(DIR_BIN) -target $(JAVA_VERSION) $(SOURCES) -g:none

doc:
	javadoc -classpath $(DIR_SRC) -d $(DIR_DOC) $(SOURCES)

jar: $(CLASSES)
	jar cfm $(JAR_FILEPATH) $(MANIFEST_FILEPATH) -C $(DIR_BIN) .

run:
	java -jar $(JAR_FILEPATH)

