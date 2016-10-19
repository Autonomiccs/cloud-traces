# cloud-traces
This project transforms Google data traces that are a job oriented scenario to a cloud computing one. Moreover, it provides a framework to use the converted traces and test management and deployment techniques.

## Usage
There are two usage modes. First you can use the set of classes provided by this project to convert a data set such as Google data traces (or any other that has the same format) to a cloud data set. The format we expect is the same as the one found at(https://github.com/google/cluster-data/blob/master/TraceVersion1.md). That means, we assign a virtual machine for every Job in the data set. For that, tasks resource usage a grouped and then distributed to a virtual machine that has enough resources to run the Job and all of its tasks.

To build the project you can simply use:
```
mvn clean compile package
```

Then, to convert Google traces you can execute:
```
java -cp cloud-traces-1.0.0-SNAPSHOT-jar-with-dependencies.jar br.com.autonomiccs.cloudTraces.main.GoogleTracesToCloudTracesParser C:\Users\<user>\Downloads\google-cluster-data-1\google-cluster-data-1.csv
```

The aforementioned command will create a file called 'cloudVmTraces.csv'. If you use the same data set provided at 'https://github.com/google/cluster-data/blob/master/TraceVersion1.md', the result will be the same as the one found in our project root directory.

Now that we have create a cloud data set, we can start playing around with it. To execute a simulation you can simply run:
```
java -cp cloud-traces-1.0.0-SNAPSHOT-jar-with-dependencies.jar br.com.autonomiccs.cloudTraces.main.CloudTracesSimulator <pathTocloudVmTraces.csv>
```

The output of the simulation will be written to a file named 'cloud-traces.log' in your current directory. If you want to test some deployment or management technique you can do the following.
* Fork our project :)
* for management techniques you can extend 'br.com.autonomiccs.cloudTraces.algorithms.management.ClusterAdministrationAlgorithmEmptyImpl' or implement 'br.com.autonomiccs.cloudTraces.algorithms.management.ClusterAdministrationAlgorithm'. After that, you can write your own and fancy management methods ;)
* With your brand new management algorithm you need to change the method 'br.com.autonomiccs.cloudTraces.main.CloudTracesSimulator.getClusterAdministrationAlgorithms()' to return your newly created algorithm, then just re-run the aforementioned maven tasks and execute the simulation. 
* get the result, analyze the data, plot it, write the paper and publish it.  

For deployment the steps are basically the same, with only small changes:
* you have to implement 'br.com.autonomiccs.cloudTraces.algorithms.deployment.DeploymentHeuristic' or extend 'br.com.autonomiccs.cloudTraces.algorithms.deployment.SmallestClustersFirstDeploymentHeuristic'
* then, change the method 'br.com.autonomiccs.cloudTraces.main.CloudTracesSimulator.getDeploymentHeuristic()'