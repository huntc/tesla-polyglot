/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.sonatype.maven.polyglot.scala

package object dsl {

  implicit def stringToDependency(s: String) = {
    // TODO Replace dummy implementation through real thing
    Dependency("a", "b", "1.0")
  }
}
