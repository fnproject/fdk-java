#!/bin/bash

set -euo pipefail

destdir=flag
POM_PLACEHOLDER="<modelVersion>.*>"
POM_REPLACEMENT="<modelVersion>4.0.0</modelVersion>
  <licenses>
    <license>
      <name>The Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <organization>
    <name>oracle</name>
    <url>https://www.oracle.com/</url>
  </organization>
  <developers>
   <developer>
       <name>fnproject/core</name>
       <email>roneet.shaw@oracle.com</email>
       <organization>oracle</organization>
       <organizationUrl>https://www.oracle.com/</organizationUrl>
   </developer>
   </developers>
   <scm>
         <connection>scm:git:git://github.com/fnproject/fdk-java.git</connection>
         <developerConnection>scm:git:ssh://github.com:fnproject/fdk-java.git</developerConnection>
         <url>https://github.com/fnproject/fdk-java</url>
   </scm>
  <distributionManagement>
      <repository>
          <id>central</id>
          <url>https://central.sonatype.com/api/v1/staging/deploy/maven2</url>
      </repository>
      <snapshotRepository>
          <id>central</id>
          <url>https://central.sonatype.com/repository/maven-snapshots/</url>
      </snapshotRepository>
  </distributionManagement>
  <name>fdk-java</name>
  <description>The Function Development Kit for Java makes it easy to build and deploy Java functions to Fn</description>
  <url>https://fnproject.io/tutorials/JavaFDKIntroduction/</url>
  "

# Constants
MAVEN_CENTRAL_STAGINGURL="https://central.sonatype.com/api/v1/staging/deploy/maven2"
MAVEN_CENTRAL_REPOID="central"
OUTPUT_DIR="output"

# TODO: add versions e.g. (1.0.0 1.0.1 1.0.2) 10*1+ 5+2+1

VERSIONS=(1.0.107 1.0.106 1.0.105 1.0.104 1.0.103 1.0.102 1.0.101 1.0.100 1.0.99 1.0.98 1.0.97 1.0.96 1.0.95 1.0.94 1.0.93 1.0.92 1.0.91 1.0.90 1.0.89 1.0.88)

#VERSIONS=(1.0.127 1.0.87 1.0.86 1.0.85 1.0.84 1.0.83 1.0.82 1.0.81 1.0.80 1.0.79 1.0.78 1.0.77 1.0.76 1.0.75 1.0.74 1.0.72 1.0.71 1.0.70 1.0.64 1.0.63 1.0.62 1.0.61 1.0.60 1.0.59 1.0.58 1.0.57 1.0.56 1.0.55 1.0.54 1.0.53 1.0.52 1.0.51 1.0.50 1.0.49 1.0.48 1.0.47 1.0.46 1.0.45 1.0.44 1.0.43 1.0.42 1.0.41 1.0.40 1.0.39 1.0.38 1.0.37 1.0.36 1.0.35 1.0.34 1.0.33 1.0.32 1.0.31 1.0.30 1.0.29 1.0.28 1.0.27 1.0.26 1.0.25 1.0.24 1.0.23 1.0.22 1.0.21 1.0.20 1.0.19 1.0.18 1.0.17 1.0.16 1.0.15 1.0.14 1.0.13 1.0.11 1.0.10 1.0.9 1.0.8 1.0.7 1.0.6 1.0.5 1.0.4 1.0.3 1.0.2 1.0.1)

#VERSIONS=(1.0.127 1.0.125 1.0.124 1.0.123 1.0.122 1.0.121 1.0.120 1.0.119 1.0.118 1.0.117 1.0.116 1.0.115 1.0.114 1.0.113 1.0.112 1.0.111 1.0.110 1.0.109 1.0.108 1.0.107 1.0.106 1.0.105 1.0.104 1.0.103 1.0.102 1.0.101 1.0.100 1.0.99 1.0.98 1.0.97 1.0.96 1.0.95 1.0.94 1.0.93 1.0.92 1.0.91 1.0.90 1.0.89 1.0.88 1.0.87 1.0.86 1.0.85 1.0.84 1.0.83 1.0.82 1.0.81 1.0.80 1.0.79 1.0.78 1.0.77 1.0.76 1.0.75 1.0.74 1.0.72 1.0.71 1.0.70 1.0.64 1.0.63 1.0.62 1.0.61 1.0.60 1.0.59 1.0.58 1.0.57 1.0.56 1.0.55 1.0.54 1.0.53 1.0.52 1.0.51 1.0.50 1.0.49 1.0.48 1.0.47 1.0.46 1.0.45 1.0.44 1.0.43 1.0.42 1.0.41 1.0.40 1.0.39 1.0.38 1.0.37 1.0.36 1.0.35 1.0.34 1.0.33 1.0.32 1.0.31 1.0.30 1.0.29 1.0.28 1.0.27 1.0.26 1.0.25 1.0.24 1.0.23 1.0.22 1.0.21 1.0.20 1.0.19 1.0.18 1.0.17 1.0.16 1.0.15 1.0.14 1.0.13 1.0.11 1.0.10 1.0.9 1.0.8 1.0.7 1.0.6 1.0.5 1.0.4 1.0.3 1.0.2 1.0.1 1.0.0)

# TODO: https://dl.bintray.com/<bintray-org>/<bintray-repo>/<group-id-slash-separated>/<artifact-id>
BINTRAYURL="http://dl.bintray.com/fnproject/fnproject/com/fnproject/fn/"

#IFS=$' ' read -d '' -r -a START_VER < $destdir

IFS=$' ' GLOBIGNORE='*' command eval  'START_VER=($(cat flag))'

# TODO : add artifact Id
ARTIFACT_IDs=(fdk api experimental-native-image-support flow-api flow-runtime flow-testing fn-spring-cloud-function jrestless-handler runtime testing-core testing-junit4 testing)
#ARTIFACT_IDs=(fdk api )


#parameters $1: url to test
function ping_url() {
  status_code=$(curl --head --write-out '%{http_code}\n' --silent --output /dev/null $1)
  echo $status_code
}

# Utilities
function escape_pom() {
  echo "$1" | sed 's#/#\\/#g' | tr '\n' '@'
}

function xml_encode() {
  echo $1 | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g; s/'"'"'/\&#39;/g'
}

#parameters $1: artifact_id, $2: version
function download_and_save(){
  #copy javadoc

  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2-javadoc.jar ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2-javadoc.jar \
    -o $OUTPUT_DIR/$1/$2/$1-$2-javadoc.jar
  fi

  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2-javadoc.jar.md5 ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2-javadoc.jar.md5 \
    -o $OUTPUT_DIR/$1/$2/$1-$2-javadoc.jar.md5
  fi

  #copy sources
  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2-sources.jar ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2-sources.jar \
    -o $OUTPUT_DIR/$1/$2/$1-$2-sources.jar
  fi

  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2-sources.jar.md5 ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2-sources.jar.md5 \
    -o $OUTPUT_DIR/$1/$2/$1-$2-sources.jar.md5
  fi

  #copy jar
  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2.jar ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2.jar \
    -o $OUTPUT_DIR/$1/$2/$1-$2.jar
  fi

  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2.jar.md5 ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2.jar.md5 \
    -o $OUTPUT_DIR/$1/$2/$1-$2.jar.md5
  fi

  #copy pom
  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2.pom ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2.pom \
    -o $OUTPUT_DIR/$1/$2/$1-$2.pom
  fi

  if [ $( ping_url $BINTRAYURL/$1/$2/$1-$2.pom.md5 ) == 200 ]
  then
    mkdir -p $OUTPUT_DIR/$1/$2
    curl -L $BINTRAYURL/$1/$2/$1-$2.pom.md5 \
    -o $OUTPUT_DIR/$1/$2/$1-$2.pom.md5
  fi

}

function save_flag() {
  echo "$1 $2" > "$destdir"
}

function download_all_versions(){
  for (( i=${START_VER[0]}; i<${#ARTIFACT_IDs[@]}; i++ )); do
    for (( j=${START_VER[1]}; j<${#VERSIONS[@]}; j++ )); do
      aid=${ARTIFACT_IDs[$i]}
      ver=${VERSIONS[$j]}

      echo "Downloading version $ver $aid"
      download_and_save $aid $ver
      save_flag $i $j
    done
    START_VER[1]=0
  done
}

function prepare_pom() {
  echo "Preparing $1"
  EXAMPLE_MODULE="<module>examples<\/module>"
  EXP_MODULE="<module>integration-tests<\/module>"
  NEW=" "
  PLUGINS="<plugins>"
  SCM_PLUGIN="
    <plugins>
    <plugin>
      <groupId>org.sonatype.central</groupId>
      <artifactId>central-publishing-maven-plugin</artifactId>
      <version>0.8.0</version>
      <extensions>true</extensions>
      <configuration>
          <publishingServerId>central</publishingServerId>
          <autoPublish>true</autoPublish>
          <waitUntil>published</waitUntil>
          <!-- Other configurations as needed -->
      </configuration>
  </plugin>
  "
  for (( i=${START_VER[0]}; i<${#ARTIFACT_IDs[@]}; i++ )); do
    for (( j=${START_VER[1]}; j<${#VERSIONS[@]}; j++ )); do
      aid=${ARTIFACT_IDs[$i]}
      ver=${VERSIONS[$j]}

      echo $aid $ver
      #if [ $ver = $2 ]; then
        pom=$OUTPUT_DIR/$aid/$ver/$aid-$ver.pom
        if [ -f "$pom" ]; then
          if [ $aid = "fdk" ]; then
            newPom=$1/$aid/$ver/pom.xml
            mkdir -p $1/$aid/$ver
            sed -e "s/$POM_PLACEHOLDER/$(escape_pom "$POM_REPLACEMENT")/g; s/$EXAMPLE_MODULE/$NEW/g; s/$EXP_MODULE/$NEW/g; s/$PLUGINS/$(escape_pom "$SCM_PLUGIN")/1;" \
                $pom |\
                tr '@' '\n' > temp.txt
            mv temp.txt $newPom
          else
            newPom=$1/fdk/$ver/$aid/pom.xml
            mkdir -p $1/fdk/$ver/$aid

            #Copy pom
            cp $pom $1/fdk/$ver/$aid/
            mv $1/fdk/$ver/$aid/$aid-$ver.pom $newPom
          fi
        fi
      #fi
    done
  done

}


function modify_group_id() {
  echo "Changing $1"
  OLD_GROUP_ID="<groupId>com.fnproject.fn<\/groupId>"
  NEW_GROUP_ID="<groupId>$1</groupId>"
  EXAMPLE_MODULE="<module>examples<\/module>"
  EXP_MODULE="<module>integration-tests<\/module>"
  NEW=" "

  PLUGINS="<plugins>"
  SCM_PLUGIN="
    <plugins>
    <plugin>
      <groupId>org.sonatype.central</groupId>
      <artifactId>central-publishing-maven-plugin</artifactId>
      <version>0.8.0</version>
      <extensions>true</extensions>
      <configuration>
          <publishingServerId>central</publishingServerId>
          <autoPublish>true</autoPublish>
          <waitUntil>published</waitUntil>
          <!-- Other configurations as needed -->
      </configuration>
  </plugin>
  "
  for (( i=${START_VER[0]}; i<${#ARTIFACT_IDs[@]}; i++ )); do
    for (( j=${START_VER[1]}; j<${#VERSIONS[@]}; j++ )); do
      aid=${ARTIFACT_IDs[$i]}
      ver=${VERSIONS[$j]}

      echo $aid $ver
      if [ $ver = $2 ]; then
        pom=$OUTPUT_DIR/$aid/$ver/$aid-$ver.pom
        if [ -f "$pom" ]; then
          if [ $aid = "fdk" ]; then
            newPom=$1/$aid/$ver/pom.xml
            mkdir -p $1/$aid/$ver
            sed -e "s/$OLD_GROUP_ID/$(escape_pom "$NEW_GROUP_ID")/g; s/$POM_PLACEHOLDER/$(escape_pom "$POM_REPLACEMENT")/g; s/$EXAMPLE_MODULE/$NEW/g; s/$EXP_MODULE/$NEW/g; s/$PLUGINS/$(escape_pom "$SCM_PLUGIN")/1;" \
                $pom |\
                tr '@' '\n' > temp.txt
            mv temp.txt $newPom
          else
            newPom=$1/fdk/$ver/$aid/pom.xml
            mkdir -p $1/fdk/$ver/$aid
            sed -e "s/$OLD_GROUP_ID/$(escape_pom "$NEW_GROUP_ID")/g" \
                $pom |\
                tr '@' '\n' > temp.txt
            mv temp.txt $newPom
          fi
        fi
      fi
    done
  done

}

function deploy() {
  for (( i=${START_VER[0]}; i<${#ARTIFACT_IDs[@]}; i++ )); do
    for (( j=${START_VER[1]}; j<${#VERSIONS[@]}; j++ )); do
      aid=${ARTIFACT_IDs[$i]}
      ver=${VERSIONS[$j]}

      #if [ $ver = $2 ]; then
        # Add required metadata to pom.xml
        if [ $aid = "fdk" ]; then
          pom=$1/$aid/$ver/pom.xml
          if [  -f "$pom" ]; then
            echo "pom maven fdk"
            echo $pom
            mvn -s ./user-settings.xml gpg:sign-and-deploy-file \
               -Durl=$MAVEN_CENTRAL_STAGINGURL \
               -DrepositoryId=$MAVEN_CENTRAL_REPOID \
               -DpomFile=$pom \
               -Dfile=$pom
          fi
        else
          pom=$1/fdk/$ver/$aid/pom.xml
          if [  -f "$pom" ]; then
            echo "pom maven other"
            mvn -s ./user-settings.xml gpg:sign-and-deploy-file \
               -Durl=$MAVEN_CENTRAL_STAGINGURL \
               -DrepositoryId=$MAVEN_CENTRAL_REPOID \
               -DpomFile=$pom \
               -Dfile=$pom
          fi
          echo "Singing jar"
          echo $pom

          #sign jar files
          if [ -f "$pom" ] && [ -f $OUTPUT_DIR/$aid/$ver/$aid-$ver.jar ]; then
            echo "After jar inside if"
            echo $pom
            mvn -s ./user-settings.xml gpg:sign-and-deploy-file \
               -Durl=$MAVEN_CENTRAL_STAGINGURL \
               -DrepositoryId=$MAVEN_CENTRAL_REPOID \
               -DpomFile=$pom \
               -Dfile=$OUTPUT_DIR/$aid/$ver/$aid-$ver.jar
          fi

          echo "Singing sources"
          echo $pom
          #Sign sources.jar
          if [ -f "$pom" ] && [ -f $OUTPUT_DIR/$aid/$ver/$aid-$ver-sources.jar ]; then
            echo "sources"
            mvn -s ./user-settings.xml gpg:sign-and-deploy-file \
             -Durl=$MAVEN_CENTRAL_STAGINGURL \
             -DrepositoryId=$MAVEN_CENTRAL_REPOID \
             -DpomFile=$pom \
             -Dfile=$OUTPUT_DIR/$aid/$ver/$aid-$ver-sources.jar \
             -Dclassifier=sources
           elif [ -d $OUTPUT_DIR/$aid/$ver ] && [ $aid != "fdk" ]; then
              if [ -f fallback/$aid-sources.jar ]; then
               echo "Fallback sourcess"
               echo $OUTPUT_DIR/$aid/$ver
               cp fallback/$aid-sources.jar $1/fdk/$ver/$aid/
               mv $1/fdk/$ver/$aid/$aid-sources.jar $1/fdk/$ver/$aid/$aid-$ver-sources.jar
               mvn -s ./user-settings.xml gpg:sign-and-deploy-file \
                -Durl=$MAVEN_CENTRAL_STAGINGURL \
                -DrepositoryId=$MAVEN_CENTRAL_REPOID \
                -DpomFile=$pom \
                -Dfile=$1/fdk/$ver/$aid/$aid-$ver-sources.jar \
                -Dclassifier=sources
              fi
          fi
          echo "Singing Javadocs"
          echo $pom

          #sign javadoc.jar
          if [ -f "$pom" ] && [ -f $OUTPUT_DIR/$aid/$ver/$aid-$ver-javadoc.jar ]; then
            echo "javadocs"
            mvn -s ./user-settings.xml gpg:sign-and-deploy-file \
             -Durl=$MAVEN_CENTRAL_STAGINGURL \
             -DrepositoryId=$MAVEN_CENTRAL_REPOID \
             -DpomFile=$pom \
             -Dfile=$OUTPUT_DIR/$aid/$ver/$aid-$ver-javadoc.jar \
             -Dclassifier=javadoc
          elif [ -d $OUTPUT_DIR/$aid/$ver ] && [ $aid != "fdk" ]; then
            if [ -f fallback/$aid-javadoc.jar ]; then
              echo "Fallback javadocs2"
              echo $OUTPUT_DIR/$aid/$ver
              cp fallback/$aid-javadoc.jar $1/fdk/$ver/$aid/
              mv $1/fdk/$ver/$aid/$aid-javadoc.jar $1/fdk/$ver/$aid/$aid-$ver-javadoc.jar
              mvn -s ./user-settings.xml gpg:sign-and-deploy-file \
               -Durl=$MAVEN_CENTRAL_STAGINGURL \
               -DrepositoryId=$MAVEN_CENTRAL_REPOID \
               -DpomFile=$pom \
               -Dfile=$1/fdk/$ver/$aid/$aid-$ver-javadoc.jar \
               -Dclassifier=javadoc
           fi
          fi
        fi
      #fi
      #save_flag $i $j
    done
    START_VER[1]=0
  done
}

#main
#Script execution will start from here
if  [[ $1 = "-d" ]]; then
  echo "Downloading fdk-java version from Bintray"
  download_all_versions
  save_flag $i $j
elif [[ $1 = "-c" ]]; then
  echo "Changing groupId to $2"
  modify_group_id $2 $3
  deploy $2 $3
else
  echo "Sign and Deploy to Maven Central"
  echo $1
  prepare_pom $1
  deploy $1
fi
exit 0
