pipeline {
    agent any

    environment {
        PATH = "/opt/homebrew/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile Java Code') {
            steps {
                dir('Test') {
                    sh 'mvn clean compile'
                }
            }
        }

        stage('Collect Git Details & Send Webhook') {
            steps {
                script {
                    def startTime = new Date(currentBuild.startTimeInMillis).toString()
                    def triggeredBy = currentBuild.getBuildCauses()
                        .collect { it.shortDescription }
                        .join(", ")

                    def gitBranch = env.GIT_BRANCH ?: 'origin/main'
                    def repoUrl = scm.userRemoteConfigs[0].url
                    def repoName = repoUrl.tokenize('/').last().replace('.git', '')

                    def changes = []
                    currentBuild.changeSets.each { changeSet ->
                        changeSet.items.each { entry ->
                            changes << [
                                commitId : entry.commitId,
                                author   : entry.author.fullName,
                                message  : entry.msg,
                                timestamp: new Date(entry.timestamp).toString(),
                                files    : entry.affectedFiles.collect { it.path }
                            ]
                        }
                    }

                    def payload = [
                        source      : "jenkins",
                        sourceType  : "pipeline",
                        job         : env.JOB_NAME,
                        build       : env.BUILD_NUMBER,
                        status      : currentBuild.currentResult,
                        repository  : repoName,        
                        repoUrl     : repoUrl,
                        branch      : gitBranch,
                        triggeredBy : triggeredBy,
                        buildStart  : startTime,
                        changes     : changes
                    ]

                    echo "===== JSON PAYLOAD ====="
                    echo groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(payload))
                    echo "========================"

                    httpRequest(
                        httpMode: 'POST',
                        url: 'https://webhook.site/49bc75e8-2afe-460c-8382-9cbf9be8ea84',
                        contentType: 'APPLICATION_JSON',
                        requestBody: groovy.json.JsonOutput.toJson(payload)
                    )
                }
            }
        }
    }
}
