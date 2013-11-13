To get the testbed :
 svn co https://svn.shibboleth.net/java-idp-testbed/trunk idp-testbed

To run the testbed from the command line : 
 cd idp-testbed
 mvn jetty:run

To import the testbed into Eclipse :
 File -> Import -> Maven -> Existing Maven Projects then browse to java-idp-testbed
 
To run the testbed from within Eclipse :
 Right click on common.Main -> Run As -> Java Application

Open the following location in your browser : 
 http://127.0.0.1:8080

The idp.home system property defines the path to configuration files, which are located relative to this file in src/main/resources.
