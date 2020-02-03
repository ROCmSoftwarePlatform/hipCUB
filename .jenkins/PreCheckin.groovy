#!/usr/bin/env groovy
// This shared library is available at https://github.com/ROCmSoftwarePlatform/rocJENKINS/
@Library('rocJenkins@ping') _

// This file is for internal AMD use.
// If you are interested in running your own Jenkins, please raise a github issue for assistance.

import com.amd.project.*
import com.amd.docker.*
import java.nio.file.Path;

properties(auxiliary.setProperties())

def hipCUBCI = 
{
    nodeDetails, jobName->

    def hipcub = new rocProject('hipCUB', 'PreCheckin')

    // Define test architectures, optional rocm version argument is available
    def nodes = new dockerNodes(nodeDetails, jobName, hipcub)

    boolean formatCheck = false

    def commonGroovy

    def compileCommand =
    {
        platform, project->

        commonGroovy = load "${project.paths.project_src_prefix}/.jenkins/Common.groovy"
        commonGroovy.runCompileCommand(platform, project)
    }

    def testCommand =
    {
        platform, project->

        commonGroovy.runTestCommand(platform, project)
    }

    def packageCommand =
    {
        platform, project->
        
        commonGroovy.runPackageCommand(platform, project)
    }

    buildProject(hipcub, formatCheck, nodes.dockerArray, compileCommand, testCommand, packageCommand)
}

ci: { 
    String buildURL = env.BUILD_URL
    echo buildURL
    println(buildURL)
    // 'http://10.216.151.18:8080/job/PreCheckin/job/Tensile/job/develop/18'
    def jobNameList = ["compute-rocm-dkms-no-npi":([ubuntu16:['gfx803'],centos7:['gfx803','gfx900'],sles15sp1:['gfx803']]), 
                        "rocm-docker":([ubuntu16:['gfx803'],centos7:['gfx803','gfx900'],sles15sp1:['gfx803']])]
    jobNameList.each 
    {
        jobName, nodeDetails->
        echo jobName
        if (${env.buildURL}.contains(jobName))
            hipCUBCI(nodeDetails, jobName)
    }
}



