#!/usr/bin/env groovy
// This shared library is available at https://github.com/ROCmSoftwarePlatform/rocJENKINS/
@Library('rocJenkins') _

// This file is for internal AMD use.
// If you are interested in running your own Jenkins, please raise a github issue for assistance.

import com.amd.project.*
import com.amd.docker.*
import java.nio.file.Path;

hipCUBCI:
{

    def hipcub = new rocProject('hipCUB')

    // Define test architectures, optional rocm version argument is available
    def nodes = new dockerNodes(['ubuntu && gfx906'], hipcub)

    boolean formatCheck = false

    def commonGroovy

    def compileCommand =
    {
        platform, project->
        // sh '''pwd
        //       ls'''
        // Needed to get the latest version of Common.groovy. To be properly implemented in the future in rocJenkins
        // checkout scm
        sh '''pwd
              ls
              ls ${project.paths.project_src_prefix} -a'''
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

