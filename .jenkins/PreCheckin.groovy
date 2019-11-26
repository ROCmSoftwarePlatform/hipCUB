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
        
        echo "************Checkout common file"
        checkout scm

        sh '''
            ls 
            ls .jenkins/
            cat .jenkins/Common.groovy
        '''

        echo "************Loading common file"
        commonGroovy = load ".jenkins/Common.groovy"
        
        echo "************Running compile command"
        // def command = commonGroovy.getCompileCommand(platform, project)
        commonGroovy.runCompileCommand(platform, project)
    }

    def testCommand =
    {
        platform, project->

        String sudo = auxiliary.sudo(platform.jenkinsLabel)

	def command = """#!/usr/bin/env bash
                    set -x
                    cd ${project.paths.project_build_prefix}/build/release
                    make -j4
                    ${sudo} LD_LIBRARY_PATH=/opt/rocm/lib/ ctest --output-on-failure
                """

        platform.runCommand(this, command)
    }

    def packageCommand =
    {
        platform, project->

        def command
        
        if(platform.jenkinsLabel.contains('hip-clang'))
        {
            packageCommand = null
        }
        else if(platform.jenkinsLabel.contains('ubuntu'))
        {
            command = """
                    set -x
                    cd ${project.paths.project_build_prefix}/build/release
                    make package
                    rm -rf package && mkdir -p package
                    mv *.deb package/
                    dpkg -c package/*.deb
                  """        
            
            platform.runCommand(this, command)
            platform.archiveArtifacts(this, """${project.paths.project_build_prefix}/build/release/package/*.deb""")
        }
        else
        {
            command = """
                    set -x
                    cd ${project.paths.project_build_prefix}/build/release
                    make package
                    rm -rf package && mkdir -p package
                    mv *.rpm package/
                    rpm -qlp package/*.rpm
                  """
            
            platform.runCommand(this, command)
            platform.archiveArtifacts(this, """${project.paths.project_build_prefix}/build/release/package/*.rpm""")
        }
    }

    buildProject(hipcub, formatCheck, nodes.dockerArray, compileCommand, testCommand, packageCommand)
}

