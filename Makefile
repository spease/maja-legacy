PROJECT_NAME=Maja

DIR_BIN=bin
DIR_CLASS=class
DIR_DOC=doc
DIR_SRC=src

JAR_FILEPATH=$(DIR_BIN)/$(PROJECT_NAME).jar
MANIFEST_FILEPATH=$(DIR_SRC)/Manifest.txt

JAVA_VERSION=1.7

SOURCES=$(shell find $(DIR_SRC) -iname "*.java")
CLASSES=$(patsubst $(DIR_SRC)%,$(DIR_CLASS)%,$(SOURCES:%.java=%.class))
CLASSES_MANIFEST=$(DIR_CLASS)/Manifest.txt

all: $(JAR_FILEPATH)

clean:
	rm -rf --preserve-root $(DIR_BIN)
	rm -rf --preserve-root $(DIR_CLASS)
	rm -rf --preserve-root $(DIR_DOC)

%.class:
	mkdir -p $(DIR_CLASS)
	javac -classpath $(DIR_SRC) -s $(DIR_SRC) -d $(DIR_CLASS) -target $(JAVA_VERSION) $(patsubst $(DIR_CLASS)%,$(DIR_SRC)%,$(patsubst %.class,%.java,$@)) -g:none

$(CLASSES_MANIFEST): $(MANIFEST_FILEPATH)
	cp $(MANIFEST_FILEPATH) $(CLASSES_MANIFEST)

doc:
	javadoc -classpath $(DIR_SRC) -d $(DIR_DOC) $(SOURCES)

$(JAR_FILEPATH): $(CLASSES) $(CLASSES_MANIFEST)
	mkdir -p $(DIR_BIN)
	jar cfm $(JAR_FILEPATH) $(MANIFEST_FILEPATH) -C $(DIR_CLASS) .

run: $(JAR_FILEPATH)
	java -jar $(JAR_FILEPATH)
