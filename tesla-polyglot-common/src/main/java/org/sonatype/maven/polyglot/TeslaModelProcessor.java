/*
 * Copyright (C) 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sonatype.maven.polyglot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 * Polyglot model processor.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 0.7
 */
@Component(role = ModelProcessor.class, hint = "tesla-polyglot")
public class TeslaModelProcessor implements ModelProcessor {

  @Requirement
  private PolyglotModelManager manager;

  public File locatePom(final File dir) {
    assert manager != null;
    File pomFile = manager.locatePom(dir);
    if ( pomFile != null && !"pom.xml".equals( pomFile.getName() ) && ! pomFile.getName().endsWith( ".pom" ) ) {
        pomFile = new File( pomFile.getParentFile(), ".tesla." + pomFile.getName() );
        try
        {
            pomFile.createNewFile();
        }
        catch (IOException e)
        {
            throw new RuntimeException( "error creating empty file", e );
        }
    }

    // behave like proper maven in case there is no pom from manager
    return pomFile == null ? new File(dir, "pom.xml") : pomFile;
  }

  public Model read(final File input, final Map<String, ?> options) throws IOException, ModelParseException {
    Model model;

    Reader reader = new BufferedReader(new FileReader(input));
    try {
      model = read(reader, options);
      model.setPomFile(input);
    } finally {
      IOUtil.close(reader);
    }
    return model;
  }

  public Model read(final InputStream input, final Map<String, ?> options) throws IOException, ModelParseException {
    return read(new InputStreamReader(input), options);
  }

  @SuppressWarnings( { "unchecked", "rawtypes" } )
  public Model read(final Reader input, final Map<String, ?> options) throws IOException, ModelParseException {
    assert manager != null;
    ModelSource source = (ModelSource) options.get( ModelProcessor.SOURCE );
    if ( ( "" + source ).contains(  ".tesla." ) ) {
        System.out.println( source.getLocation() );

        File pom = new File( source.getLocation() );
        source =  new FileModelSource( new File( pom.getPath().replaceFirst( "[.]tesla[.]", "" ) ) );

        ((Map)options).put( ModelProcessor.SOURCE, source );

        ModelReader reader = manager.getReaderFor(options);
        Model model = reader.read(source.getInputStream(), options);

        MavenXpp3Writer xmlWriter = new MavenXpp3Writer();
        StringWriter xml = new StringWriter();
        xmlWriter.write( xml, model );

        FileUtils.fileWrite( pom, xml.toString() );

        // dump pom if filename is given via the pom properties
        String dump = model.getProperties().getProperty( "tesla.dump.pom" );
        if ( dump != null ) {
            File dumpPom =  new File( pom.getParentFile(), dump );
            if ( !dumpPom.exists() || ! FileUtils.fileRead( dumpPom ).equals( xml.toString() ) ){
                dumpPom.setWritable( true );
                FileUtils.fileWrite( dumpPom, xml.toString() );
                if ( "true".equals( model.getProperties().getProperty( "tesla.dump.readonly" ) ) ){
                    dumpPom.setReadOnly();
                }
            }
        }

        model.setPomFile(pom);
        return model;
    }
    else {
        ModelReader reader = manager.getReaderFor(options);
        return reader.read(input, options);
    }
  }
}
