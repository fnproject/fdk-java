def profile = env.BRANCH_NAME == "master" ? "-Pjenkins" : ""
pipeline {
    agent any
    tools {
        maven 'M3'
        jdk   'jdk8'
    }

    stages {
        stage ('Build'){
            steps {
                sh "mvn ${profile} -T4 -B clean package"
                junit allowEmptyResults: true, testResults: '*/target/surefire-reports/*.xml'
                jacoco()
            }
        }

        stage ('MvnDeploy'){
            when {
                branch 'master'
            }
            steps {
                milestone ordinal: 10, label: 'Deploy build to nexus'
                sh "mvn -Pjenkins -DskipTests deploy"
            }
        }
        stage ('IntegrationTest') {
            when {
                branch 'master'
            }
            steps {
                sh 'integration-tests/run-local.sh'
            }
        }

        stage ('DockerPush'){
            when {
                branch 'master'
            }
            steps {
                milestone ordinal: 20, label: 'Push docker images'
                sh "docker tag `cat headrevtag.txt` registry.oracledx.com/skeppare/`cat headrevtag.txt`"
                sh "docker push registry.oracledx.com/skeppare/`cat headrevtag.txt`"
            }
        }

        stage ('DockerDeploy'){
            when {
                branch 'master'
            }
            steps {
                milestone ordinal: 30, label: 'Deploy latest docker images'
                sh ". /home/mjg/proxy ; kubectl set image deployment/completer-service completer-service=registry.oracledx.com/skeppare/`cat headrevtag.txt`"
            }
        }
    }

    /*
    // Mattermost broken at the moment. Turn it off.
    post {
        success {
            #mattermostSend color: 'good', endpoint: 'https://odx.stengpoc.ucfc2z3b.usdv1.oraclecloud.com/hooks/rp44gubpw3btfnqxwk39za7kje', message: ":sunflower: :sunflower:  Build Passed ${env.JOB_NAME} ${env.BUILD_NUMBER} ([build](${env.BUILD_URL}console), [gitlab](http://gitlab-odx.oracle.com/odx/jfaas/tree/${env.BRANCH_NAME})) :sunflower: :sunflower:"
        }
        failure {
            #mattermostSend color: 'danger', endpoint: 'https://odx.stengpoc.ucfc2z3b.usdv1.oraclecloud.com/hooks/rp44gubpw3btfnqxwk39za7kje', message: ":boom: :boom:  Build Failed ${env.JOB_NAME} ${env.BUILD_NUMBER} ([build](${env.BUILD_URL}console), [gitlab](http://gitlab-odx.oracle.com/odx/jfaas/tree/${env.BRANCH_NAME})) :boom: :boom:"
            #mattermostSend color: 'danger', endpoint: 'https://odx.stengpoc.ucfc2z3b.usdv1.oraclecloud.com/hooks/eqxe7mqgrbrqiryu47wnrjzppy', message: ":boom: :boom:  Build Failed ${env.JOB_NAME} ${env.BUILD_NUMBER} ([build](${env.BUILD_URL}console), [gitlab](http://gitlab-odx.oracle.com/odx/jfaas/tree/${env.BRANCH_NAME})) :boom: :boom:"
        }
    }
    */
}
