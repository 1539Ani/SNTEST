pipeline {
    agent any

    environment {
        PATH = "/opt/homebrew/bin:${env.PATH}"
        TARGET_ENV = 'DEV'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                script {
                    int status = sh(
                        script: 'cd Test && mvn clean compile',
                        returnStatus: true
                    )

                    if (status != 0) {
                        error("COMPILE_FAILED")
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    int status = sh(
                        script: 'cd Test && mvn test',
                        returnStatus: true
                    )

                    junit 'Test/target/surefire-reports/*.xml'

                    if (status != 0) {
                        currentBuild.description = 'Tests failed'
                        unstable("UNIT_TEST_FAILED")
                    }
                }
            }
        }

        stage('Code Coverage') {
            steps {
                script {
                    sh 'cd Test && mvn jacoco:report'

                    recordCoverage(
                        qualityGates: [
                            [metric: 'LINE', threshold: 80],
                            [metric: 'BRANCH', threshold: 70]
                        ],
                        tools: [[pattern: 'Test/target/site/jacoco/jacoco.xml']]
                    )
                }
            }
        }

        stage('Static Analysis') {
            steps {
                recordIssues(
                    tool: java(),
                    qualityGates: [[threshold: 10, unstable: true]]
                )
            }
        }

        stage('Deploy') {
            steps {
                script {
                    int status = sh(
                        script: '''
                            echo "Deploying to DEV"
                            exit 1
                        ''',
                        returnStatus: true
                    )

                    if (status != 0) {
                        error("DEPLOY_FAILED")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // --- FINAL CLASSIFICATION ---
                def result = currentBuild.currentResult ?: 'SUCCESS'
                def failureType = 'NONE'

                if (result == 'FAILURE') {
                    if (currentBuild.rawBuild.getLog(50).any { it.contains('DEPLOY_FAILED') }) {
                        failureType = 'PIPELINE_FAILED'
                    } else {
                        failureType = 'BUILD_FAILED'
                    }
                } else if (result == 'UNSTABLE') {
                    failureType = 'BUILD_UNSTABLE'
                }

                def payload = [
                    job         : env.JOB_NAME,
                    buildNumber : env.BUILD_NUMBER,
                    result      : result,
                    failureType : failureType,
                    environment : env.TARGET_ENV
                ]

                echo '===== FINAL PAYLOAD ====='
                echo groovy.json.JsonOutput.prettyPrint(
                    groovy.json.JsonOutput.toJson(payload)
                    )

                sh """
                  echo '${payloadJson}' > payload.json
                  curl -X POST -H "Content-Type: application/json" \
                       -d @payload.json \
                       https://webhook.site/4746df80-50b3-4fc8-af8f-92be5b1a512c
                """
            }
        }
    }
}
