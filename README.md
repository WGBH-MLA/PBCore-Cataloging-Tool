# PBCore-Cataloging-Tool

Disclaimer:  

This code is still in the early stages and not a completed product. 

Version 5   1/24/2018
--------- 

First cut of UI for  Description and Instantiation Document Windows.   PBCore XML can be edited using Controlled Vocabularies.   Files are not read or written from disk quite yet.

Version 4  - 1/17/2018
---------

Changes from previous versions are  in the directory crawling and searching code.

 

Second Commit - 12/21/2017

To just view the FontEnd.
-----------------------


1) Download Scene Builder http://gluonhq.com/products/scene-builder/#download and install

2) Download a zip of this application from GitHub https://github.com/WGBH/PBCore-Cataloging-Tool
-- Click on the green button Clone or Download.
-- Extract the Zip.

3)  From this directory:  ...\PBCore-Cataloging-Tool\src\main\resources\fxml

    Open these files in this director in Scence Builder.


To Complile and Build Application
----------------------------------

In order to build.   NetBeans is the easiest method.  And we have included a NetBeans project since we are using it for development. Minimally all you need is Java 8 & Maven. Maven comes packaged with NetBeans.


1) Download NetBeans.

   JDK 8 is required for installing and running the Java SE, Java EE and All NetBeans Bundles. 
    
  A) If you don't have Java alreay installed - Install Java 8 & NetBeans from here:

   http://www.oracle.com/technetwork/java/javase/downloads/jdk-netbeans-jsp-142931.html  

   B)Otherwise download just NetBeans  https://netbeans.org/downloads/index.html 
   
   Choose the right version for the operating system you are running. 
   When it's downloaded Run the installer.


3) Download a zip of this application from GitHub https://github.com/WGBH/PBCore-Cataloging-Tool
-- Click on the green button Clone or Download.

4) Run NetBeans.   
 Select    File -> Import Project  -> From Zip
 
 and select the zip you downloaded.  This will import the project into NetBeans.
 
 This will put PBCore in the "Projects" window

5) If you haven't already install SceneBuilder.   YOu can click on the .fxml files inside NetBeans.
    
    PBCore ->  Other Sources  -> src/main/resources  -> fxml
    
6) You can complile in NetBeans by right clicking on PBCore (in the left Projects window)

   Select Build - to build the application.
   Select Run  - to run the application

   


First Commit - 12/6/2017
--------------------------
This version runs tests. (17 of them.)   If you run the applicaiton it doesn't do more than show the splash screen.

You should be able to run it through Netbeans or build it using Maven.
To run you need the lates Java JVM installed.  And  Maven or NetBeans installed.

To actually run the app 
just go into that subfolder and run “java -jar PBCore-Cataloging-Tool-1.0.jar” 
(assuming that was the jarfile generated by Maven - otherwise replace accordingly).

