#!/usr/bin/env groovy

pipeline {
    parameters {
        choice(name: 'ENV', choices: ["test", "prod"], description: 'AWS account to execute in, default test')
    }

    options {
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout and resolve variables') {
            steps {
                script {
                    deleteDir()
                    checkout scm

                    if (env.ENV == 'test') {
                        env.JENKINS_ROLE = "${JENKINS_ROLE_TEST}"
                        env.AWS_ACCOUNT = "${AWS_TEST_ACCOUNT}"
                    } else if (env.ENV == 'prod') {
                        env.JENKINS_ROLE = "${JENKINS_ROLE_PROD}"
                        env.AWS_ACCOUNT = "${AWS_PROD_ACCOUNT}"
                    } else {
                        error("Select proper ENV!")
                    }

                    currentBuild.description = "Deploying in ${ENV}"
                }
            }
        }

        stage("Deploy CloudFormation Stack") {
            steps {
                withAWS(role: "${JENKINS_ROLE}", roleAccount: "${AWS_ACCOUNT}", region: "us-east-1") {
                    script {
                        dir('rds/templates') {
                            cfnValidate(file: "db-stack.yaml")
                            cfnUpdate(
                                    file: 'db-stack.yaml',
                                    paramsFile: "${ENV}/params.yaml",
                                    stack: "db-${ENV}",
                                    tags: ["Env=${ENV}"])
                        }
                    }
                }
            }
        }
    }
}
