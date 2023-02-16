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
        string(name: 'GIT_HUB_BRANCH', defaultValue: 'main', description: 'NETAPP branch name')
        // string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'GIT_HUB_USER', defaultValue: 'main', description: 'Git hub user')
        string(name: 'GIT_HUB_PASSWORD', defaultValue: 'main', description: 'Git hub password')
    }

    environment {
        GIT_HUB_URL="${params.GIT_HUB_URL}"
        GIT_HUB_USER="${params.GIT_HUB_URL}"
        GIT_HUB_PASSWORD="${params.GIT_HUB_URL}"
        // GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_HUB_BRANCH="${params.GIT_HUB_BRANCH}"
        VERSION="${params.VERSION}"
        SERVICE_NAME = serviceName("${params.GIT_HUB_URL}").toLowerCase()
        DOCKER_VAR = false
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
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $SERVICE_NAME 
                    mkdir $SERVICE_NAME 
                    cd $SERVICE_NAME
                    git clone credentialsId: $ https://username@github.com/username/repository.git

                    git branch: 'master',
                        credentialsId: '12345-1234-4696-af25-123455',
                        url: 'ssh://git@bitbucket.org:company/repo.git'
                    git clone --single-branch --branch $GIT_HUB_BRANCH $GIT_HUB_URL .
                    '''
                }
           }
        }
    }