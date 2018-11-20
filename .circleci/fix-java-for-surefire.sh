#!/usr/bin/env bash

 set -ex

 cat << 'EOF' > $HOME/.m2/settings.xml
 <settings>
   <profiles>
     <profile>
       <id>SUREFIRE-1588</id>
       <activation>
         <activeByDefault>true</activeByDefault>
       </activation>
       <properties>
         <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
       </properties>
     </profile>
   </profiles>
 </settings>
 EOF