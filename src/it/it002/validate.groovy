/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

def assertExistsDirectory( file )
{
  if ( !file.exists() || ! file.isDirectory() )
  {
      println( file.getAbsolutePath() + " file is missing or is not a directory." )
      return false
  }
  return true
}

def assertExistsFile( file )
{
  if ( !file.exists() || file.isDirectory() )
  {
      println( file.getAbsolutePath() + " file is missing or a directory." )
      return false
  }
  return true
}

File target = new File( basedir, "webapp/target" )
assert assertExistsDirectory( target )

String[] expectedFiles = [ "webapp.war" ]
expectedFiles.each{
 assert assertExistsFile( new File ( target, it ) )
}

File explodedWebstart = new File( target, "webapp/webstart" )
assert assertExistsDirectory( explodedWebstart )

String[] expectedJnlpFiles = [ "core-1.0.jar", "hello-world-1.0.jar", "launch.jnlp", "version.xml" ]
expectedJnlpFiles.each{
 assert assertExistsFile( new File ( explodedWebstart, it ) )
}

// regression test for MWEBSTART-54: make sure test, system and provided deps are not included
assert ! assertExistsFile( new File ( explodedWebstart, "tools.jar" ) )
assert ! assertExistsFile( new File ( explodedWebstart, "servlet-api.jar" ) )
assert ! assertExistsFile( new File ( explodedWebstart, "junit.jar" ) )

File explodedWebstartImages = new File( explodedWebstart, "images" )
assert assertExistsDirectory( explodedWebstartImages )

String[] expectedJnlpImages = [ "icon.gif" ]
expectedJnlpImages.each{
 assert assertExistsFile( new File ( explodedWebstartImages, it ) )
}


assert explodedWebstart.list().length == expectedJnlpFiles.length + 1 // images

return true
