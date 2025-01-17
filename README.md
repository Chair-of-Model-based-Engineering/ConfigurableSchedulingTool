# Creating a runnable Jar
To create a Jar for this project, you need Java, as well as Maven.
With Maven installed and this repository downloaded, open a terminal and navigate to the folder `ConfigurableSchedulingTool`, in which the pom.xml is located.
Here run the command:
```
mvn clean install package
```
The Jar will be located in the folder `target`.

# How to repeat the evaluation of the VaMoS'25 paper

The folder `example_models/VaMoS_25_Evaluation` holds all feature models used in the evaluation for the VaMoS'25 paper with the title "Modeling and Analysis of Configurable Job-Shop Scheduling Problems". All models are provided as uvl and xml-files and can be used to reproduce the family-based analysis. The separate configuration files to reproduce the instance-based analysis are not included due to their large size. To repeat the evaluation of the instance-based approach, the configuration files have to be generated using [FeatureIDE](https://featureide.github.io/).

# Usage
In a terminal, navigate to the `target`-folder. A Jar can be run with the command
```
java -jar <jarName> [list of parameters]
```
The following parameters are available.

### Path-Preferences
Setting where solutions to solved problems are stored
```
solutionpath <path>     // set location
get solutionpath        // get location
delete solutionpath     // delete preference
```
Setting where uvl-files of generated problems are stored
``` 
problempath <path>      // set location
get problempath         // get location
delete problempath      // delete preference
```
If no path-preferences are set, the program will create a `schedules`- or `problems`-folder in the working directory (if you don't change anything, this will be the `target`-folder).  

### Generating problems
Problems can be generated using the command
``` 
generate <jobCount> <taskCount< <durationOutlier> <machineCount> <optionalCount> <altCount> <altGroupCount> <deadline> <durationConCount> <maxDurationConFromOneTask> <name>
```

### Solving problems
Problems can be solved in various ways. Firstly, you can choose between two options. `o` to search for an optimal solution, or `f` to search for a feasible solution.


If you generated a problem with the program, or set the `problempath` and inserted the problem into the corresponding folder, it is not necessary to specify a path.
```
solve [o|f] <problemName>.[uvl|xml]
```
It is also possible to specify the complete path.
``` 
solve [o|f] <problemFilePath>
```
If you want to solve a variable problem via its instances, the path to the folder containing the valid configurations of the problem has to be specified, as well as the path to the xml-file of the corresponding feature model. In this case, only xml-files are supported.
```
solve [o|f] <fmXmlFilePath> <configsFolderPath>
```