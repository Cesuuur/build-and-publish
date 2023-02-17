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
        stage('Publish in Artifacotry') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}/${NETAPP_NAME}/services") {
                        sh '''
                            docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                            docker-compose push
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
