pipeline{
    agent any
    stages{
        stage("stop current container"){
            steps{
                sh 'docker-compose down'
            }
        }
        stage("Build/Start container"){
            steps{
                sh 'docker-compose up -d --build'
                sh 'docker ps'
            }
        }
    }
}