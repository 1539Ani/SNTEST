pipeline {
    agent any

    environment {
        PATH = "/opt/homebrew/bin:${env.PATH}"
        
        // Tracks the type of failure: BUILD_FAILED, QUALITY_FAILED, PIPELINE_FAILED, or NONE
        FAILURE_TYPE = ''
        // Tracks multiple stages that caused UNSTABLE (quality failures)
        FAILED_STAGES = ''
        // Stores a short error summary, e.g., compilation or deployment errors
        ERROR_SUMMARY = ''
        // Target deployment environment (DEV / PDI / NONPROD / PROD)
        TARGET_ENV = 'DEV'
    }

    stages {

        /* ================= CHECKOUT ================= */
        stage('Checkout') {
            steps {
                // Pulls the code from SCM (Git)
                checkout scm
            }
        }

        /* ================= BUILD ================= */
        stage('Compile') {
            steps {
                script {
                    try {
                        dir('Test') {
                            // Compile Java code using Maven
                            sh 'mvn clean compile'
                        }
                    } catch (err) {
                        // Mark build as failed if compilation errors occur
                        env.FAILURE_TYPE = 'BUILD_FAILED'
                        env.FAILED_STAGES = 'Compile'
                        env.ERROR_SUMMARY = err.getMessage()
                        // Stop pipeline execution immediately
                        error('Build failed during compilation')
                    }
                }
            }
        }

        /* ================= UNIT TESTS ================= */
        stage('Unit Tests') {
            steps {
                // Run tests; if any test fails, mark UNSTABLE but continue pipeline
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    sh 'mvn test'
                }
                // Publish JUnit XML test reports to Jenkins
                junit 'target/surefire-reports/*.xml'
                script {
                    if (currentBuild.result == 'UNSTABLE') {
                        // Mark quality failure and append stage to FAILED_STAGES
                        env.FAILURE_TYPE = 'BUILD_UNSTABLE'
                        env.FAILED_STAGES = (env.FAILED_STAGES ?: '') + 'Unit Tests,'
                    }
                }
            }
        }

        /* ================= CODE COVERAGE ================= */
        stage('Code Coverage') {
            steps {
                // Read JaCoCo coverage reports and mark UNSTABLE if thresholds not met
                recordCoverage(
                    tools: [[parser: 'JACOCO']],
                    qualityGates: [
                        [metric: 'LINE', threshold: 80.0, unstable: true],
                        [metric: 'BRANCH', threshold: 70.0, unstable: true]
                    ]
                )
                script {
                    if (currentBuild.result == 'UNSTABLE') {
                        // Append Code Coverage to FAILED_STAGES for webhook
                        env.FAILURE_TYPE = 'BUILD_UNSTABLE'
                        env.FAILED_STAGES = (env.FAILED_STAGES ?: '') + 'Code Coverage,'
                    }
                }
            }
        }

        /* ================= WARNINGS ================= */
        stage('Static Analysis (Warnings)') {
            steps {
                // Use Warnings-NG plugin to record static analysis issues
                // If warnings exceed threshold, mark build UNSTABLE
                recordIssues(
                    tool: java(),
                    qualityGates: [
                        [threshold: 10, type: 'TOTAL', unstable: true]
                    ]
                )
                script {
                    if (currentBuild.result == 'UNSTABLE') {
                        env.FAILURE_TYPE = 'BUILD_UNSTABLE'
                        env.FAILED_STAGES = (env.FAILED_STAGES ?: '') + 'Warnings,'
                    }
                }
            }
        }

        /* ================= DEPLOYMENT ================= */
        stage('Deploy') {
            when {
                // Only deploy if build did not fail (UNSTABLE is okay)
                expression { currentBuild.result != 'FAILURE' }
            }
            steps {
                script {
                    try {
                        echo "Deploying to ${env.TARGET_ENV}"

                        // For testing, you can simulate a failure on PDI
                        if (env.TARGET_ENV == 'PDI') {
                            sh 'exit 1'
                        } else {
                            sh 'echo Deployment successful'
                        }

                    } catch (err) {
                        // Mark pipeline as failed if deployment fails
                        env.FAILURE_TYPE = 'PIPELINE_FAILED'
                        env.FAILED_STAGES = 'Deploy'
                        env.ERROR_SUMMARY = err.getMessage()
                        error("Deployment failed in ${env.TARGET_ENV}")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // Collect start and end times
                def startTime = new Date(currentBuild.startTimeInMillis).toString()
                def endTime = new Date().toString()

                // Collect information about who triggered the build
                def triggeredBy = currentBuild.getBuildCauses()
                    .collect { it.shortDescription }
                    .join(', ')

                // Collect list of changed files for webhook
                def changedFiles = []
                currentBuild.changeSets.each { cs ->
                    cs.items.each { item ->
                        item.affectedFiles.each { f ->
                            changedFiles << f.path
                        }
                    }
                }

                // Remove trailing comma from FAILED_STAGES
                def failedStagesClean = (env.FAILED_STAGES ?: '').trim().replaceAll(/,$/, '')

                // Build webhook payload
                def payload = [
                    source        : 'jenkins',
                    job           : env.JOB_NAME,
                    buildNumber   : env.BUILD_NUMBER,
                    result        : currentBuild.currentResult,
                    failureType   : env.FAILURE_TYPE ?: 'NONE',
                    failedStages  : failedStagesClean,
                    errorSummary  : env.ERROR_SUMMARY ?: '',
                    changedFiles  : changedFiles.unique(),
                    environment   : env.TARGET_ENV ?: '',
                    triggeredBy   : triggeredBy,
                    startTime     : startTime,
                    endTime       : endTime
                ]

                def payloadJson = groovy.json.JsonOutput.toJson(payload)

                echo "===== WEBHOOK PAYLOAD ====="
                echo groovy.json.JsonOutput.prettyPrint(payloadJson)
                echo "==========================="

                // Send webhook payload
                sh """
                  echo '${payloadJson}' > payload.json
                  curl -X POST \
                       -H "Content-Type: application/json" \
                       -d @payload.json \
                       https://webhook.site/e25ea33e-9af6-4d1f-b4fd-41c4dae5490a
                """
            }
        }
    }
}
