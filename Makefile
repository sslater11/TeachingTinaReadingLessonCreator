
# The command to run my program.
output: compile_using_eclipse
	java -classpath bin TeachingTinaReadingLessonCreator
#	java -classpath /home/simon/MyStuff/Programming/eclipse-workspace/TeachingTinaReadingLessonCreator/bin TeachingTinaReadingLessonCreator

# This will compile my program using eclipse in a headless mode.
compile_using_eclipse:
	eclipse -nosplash -application org.eclipse.jdt.apt.core.aptBuild  startup.jar -data ./
#	/home/simon/eclipse/java-2022-03/eclipse/eclipse -nosplash -application org.eclipse.jdt.apt.core.aptBuild  startup.jar -data /home/simon/MyStuff/Programming/eclipse-workspace/TeachingTinaReadingLessonCreator/

