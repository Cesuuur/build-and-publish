String serviceName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

String getPath(deployment) {
    String var = deployment
    if("verification".equals(var)) {
        return "";
    }else if("validation".equals(var)){
        return "validation/";
    }else {
        return "certification/";
    }
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_HUB_URL', description: 'URL of the Github Repository')
        string(name: 'GIT_HUB_BRANCH', defaultValue: 'master', description: 'NETAPP branch name')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'dockerhub.hi.inet', description: 'Docker registry')
        // string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        // string(name: 'GIT_HUB_USER', defaultValue: 'main', description: 'Git hub user')
        // string(name: 'GIT_HUB_PASSWORD', defaultValue: 'main', description: 'Git hub password')
    }

    environment {
        GIT_HUB_URL="${params.GIT_HUB_URL}"
        // GIT_HUB_USER="${params.GIT_HUB_URL}"
        // GIT_HUB_PASSWORD="${params.GIT_HUB_URL}"
        // GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_HUB_BRANCH="${params.GIT_HUB_BRANCH}"
        VERSION="${params.VERSION}"
        SERVICE_NAME = serviceName("${params.GIT_HUB_URL}").toLowerCase()
        DOCKER_VAR = false
        DOCKER_REGISTRY="${params.DOCKER_REGISTRY}"
        PATH_DOCKER = getPath("${params.STAGE}")
    }
    stages {
        stage('Clean workspace') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
                    docker system prune -a -f --volumes
                    sudo rm -rf $WORKSPACE/$SERVICE_NAME/
                    docker network create services_default
                    '''
                }
            }
        }
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
                }
            steps {
                script{
                    dir ("${env.WORKSPACE}/") {
                        sh '''
                            rm -rf $SERVICE_NAME 
                            mkdir $SERVICE_NAME 
                            cd $SERVICE_NAME
                        '''
                    }
                    dir ("${env.WORKSPACE}/${SERVICE_NAME}/") {
                        script {
                            git branch: 'main',
                                credentialsId: '11b0c311-eb71-4277-a430-4071de1d8c82',
                                url: 'git@github.com:Telefonica/pesp-dcip-microbackend.git'
                            // sh ''' cd pesp-dcip-microbackend'''
                        }
                    }
                }
           }
        }
        stage('Check if there is a docker-compose in the repository') {
            steps {
                script{
                    DOCKER_VAR = fileExists "${env.WORKSPACE}/${SERVICE_NAME}/docker-compose.yml"
                }
                echo "env DOCKER VAR is ${DOCKER_VAR}"
            }
        }
        stage('Build') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean() 
                }
            }                
            steps {
                dir ("${env.WORKSPACE}/${SERVICE_NAME}/") {
                    sh '''
                    docker build -t ${SERVICE_NAME} .
                    '''
                }
            }
        }

 
Pull requests
Issues
Codespaces
Marketplace
Explore
 
@Cesuuur 
EVOLVED-5G
/
cicd
Public
 Watch 2 
Fork 0 Fork your own copy of EVOLVED-5G/cicd
  Star 0
Code
Issues
Pull requests
1
Actions
Projects
Security
Insights
 CAPIF_validati… 
cicd/pac/Jenkins-build.groovy
Go to file

@alemosan1
alemosan1 build pipeline
Latest commit b14f186 2 weeks ago
 History
 1 contributor
261 lines (253 sloc)  12.6 KB
Raw Blame

 
String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

String getPathAWS(deployment) {
    String var = deployment
    if("verification".equals(var)) {
        return "";
    }else if("validation".equals(var)){
        return "validation";
    }else {
        return "certification";
    }
}

String getPath(deployment) {
    String var = deployment
    if("verification".equals(var)) {
        return "";
    }else if("validation".equals(var)){
        return "validation/";
    }else {
        return "certification/";
    }
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        choice(name: 'STAGE', choices: ["verification", "validation", "certification"])       
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = credentials('AWS_ACCOUNT_NUMBER')
        SERVICE_NAME = netappName("${params.GIT_NETAPP_URL}").toLowerCase()
        DOCKER_VAR = false
        PATH_DOCKER = getPath("${params.STAGE}")
        PATH_AWS = getPathAWS("${params.STAGE}")
    }
    stages {
        stage('Clean workspace') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
                    docker system prune -a -f --volumes
                    sudo rm -rf $WORKSPACE/$SERVICE_NAME/
                    docker network create services_default
                    echo ${env.PATH_AWS}
                    '''
                }
            }
        }
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
                }
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $SERVICE_NAME 
                    mkdir $SERVICE_NAME 
                    cd $SERVICE_NAME
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                    '''
                }
           }
        }
        stage('Check if there is a docker-compose in the repository') {
            steps {
                script{
                    DOCKER_VAR = fileExists "${env.WORKSPACE}/${SERVICE_NAME}/docker-compose.yml"
                }
                echo "env DOCKER VAR is ${DOCKER_VAR}"
                
            }
        }
        //NICE TO HAVE: Makefile to encapsulate docker and docker-compose commands
        stage('Build') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean() 
                }
            }                
            steps {
                dir ("${env.WORKSPACE}/${SERVICE_NAME}/") {
                    sh '''
                    docker build -t ${SERVICE_NAME} .
                    '''
                }
            }
        }
        stage('Build Docker Compose') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()
                }
            }  
            steps {
                dir ("${env.WORKSPACE}/${SERVICE_NAME}/") {
                    sh '''
                    docker network create demo-network
                    docker-compose up --build --force-recreate -d
                    '''
                }
            }
        }
        stage('Modify container name to upload Docker-compose to Artifactory') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()  
                }
            }  
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    retry(1){
                        script {   
                            sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet '''
                            def cmd = "docker ps --format '{{.Image}}'"
                            def cmd2 = "docker ps --format '{{.Names}}'"
                            def image = sh(returnStdout: true, script: cmd).trim()
                            def name  = sh(returnStdout: true, script: cmd2).trim()
                            sh '''$(aws ecr get-login --no-include-email)'''
                            [image.tokenize(), name.tokenize()].transpose().each { x ->
                                if (env.PATH_DOCKER != null){
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${SERVICE_NAME}/"${SERVICE_NAME}-${x[1]}":${VERSION}"""
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${SERVICE_NAME}/"${SERVICE_NAME}-${x[1]}":latest"""
                                sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${SERVICE_NAME}/"${SERVICE_NAME}-${x[1]}" """
                                } else{
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${SERVICE_NAME}/"${SERVICE_NAME}-${x[1]}":${VERSION}"""
                                sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${SERVICE_NAME}/"${SERVICE_NAME}-${x[1]}":latest"""
                                sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/${SERVICE_NAME}/"${SERVICE_NAME}-${x[1]}" """
                                }
                            }
                        }
                    }
                }               
            }
        }   
        stage('Publish in Artefactory') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean() 
                }
            }   
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    retry(1){
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        if [[ -n ${PATH_DOCKER} ]]
                        then
                            docker image tag ${SERVICE_NAME} dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${SERVICE_NAME}:${VERSION}
                            docker image tag ${SERVICE_NAME} dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${SERVICE_NAME}:latest
                            docker image push --all-tags dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${SERVICE_NAME}
                        else
                            docker image tag ${SERVICE_NAME} dockerhub.hi.inet/evolved-5g/${SERVICE_NAME}:${VERSION}
                            docker image tag ${SERVICE_NAME} dockerhub.hi.inet/evolved-5g/${SERVICE_NAME}:latest
                            docker image push --all-tags dockerhub.hi.inet/evolved-5g/${SERVICE_NAME}
                        fi
                        '''
                    }
                }
            }
        }
    }
    post {
        always {
            sh '''
            docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q) 
            docker system prune -a -f --volumes
            sudo rm -rf $WORKSPACE/$SERVICE_NAME/
            '''
        }
        cleanup{
            /* clean up our workspace */
            deleteDir()
            /* clean up tmp directory */
            dir("${env.workspace}@tmp") {
                deleteDir()
            }
            /* clean up script directory */
            dir("${env.workspace}@script") {
                deleteDir()
            }
        }
    }
}
