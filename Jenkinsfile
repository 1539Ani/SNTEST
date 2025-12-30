import groovy.json.JsonOutput

pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile Java Code') {
            steps {
                sh 'mvn clean compile'
            }
        }
    }

    post {
        always {
            script {
                def startTime = new Date(currentBuild.startTimeInMillis).toString()

                def repoUrl = scm.userRemoteConfigs[0].url
                def repoName = repoUrl.tokenize('/').last().replace('.git', '')

                def payload = [
                    source     : "jenkins",
                    sourceType : "pipeline",
                    job        : env.JOB_NAME,
                    build      : env.BUILD_NUMBER,
                    status     : currentBuild.currentResult, // SUCCESS / FAILURE
                    repository : repoName,
                    repoUrl    : repoUrl,
                    branch     : env.GIT_BRANCH,
                    buildUrl   : env.BUILD_URL,
                    buildStart : startTime
                ]

                echo "===== JSON PAYLOAD ====="
                echo JsonOutput.prettyPrint(JsonOutput.toJson(payload))
                echo "========================"

                httpRequest(
                    httpMode: 'POST',
                    url: 'https://webhook.site/0fb194c3-6c22-4a4e-9f59-e97ff87905b7',
                    contentType: 'APPLICATION_JSON',
                    requestBody: JsonOutput.toJson(payload)
                )
            }
        }
    }
}
