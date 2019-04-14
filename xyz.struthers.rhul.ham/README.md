#A Computational Heterogeneous Agent Model of the Australian Economy
by Adam Struthers



##Start the Java RMI registry

To start the registry, run the rmiregistry command on the server's host. This command produces no output (when successful) and is typically run in the background. For more information, see the tools documentation for rmiregistry [Solaris, Windows].

For example, on Windows platforms:
start rmiregistry 

By default, the registry runs on TCP port 1099. To start a registry on a different port, specify the port number from the command line. For example, to start the registry on port 2001 on a Windows platform:
start rmiregistry 2001

If the registry will be running on a port other than 1099, you'll need to specify the port number in the calls to LocateRegistry.getRegistry in the Server and Client classes. For example, if the registry is running on port 2001 in this example, the call to getRegistry in the server would be:

Registry registry = LocateRegistry.getRegistry(2001);

##Start the server
To start the server, run the Server class using the java command as follows:

On Windows platforms:
start java -classpath classDir -Djava.rmi.server.codebase=file:classDir/ example.hello.Server

where classDir is the root directory of the class file tree (see destDir in the section "Compiling the source files"). Setting the java.rmi.server.codebase system property ensures that the registry can load the remote interface definition (note that the trailing slash is important); for more information about using this property, see the codebase tutorial.

The output from the server should look like this:
Server ready

The server remains running until the process is terminated by the user (typically by killing the process).

##Run the client
Once the server is ready, the client can be run as follows:
java  -classpath classDir example.hello.Client

where classDir is the root directory of the class file tree (see destDir in the section "Compiling the source files").

The output from the client is the following message:
response: Hello, world!

SOURCE: https://docs.oracle.com/javase/7/docs/technotes/guides/rmi/hello/hello-world.html
