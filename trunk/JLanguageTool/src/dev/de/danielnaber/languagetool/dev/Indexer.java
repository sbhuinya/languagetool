/* LanguageTool, a natural language style checker 
 * Copyright (C) 2006 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package de.danielnaber.languagetool.dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import de.danielnaber.languagetool.Language;
import de.danielnaber.languagetool.tools.StringTools;

/**
 * POS tag and index text files using Lucene. Required for ContextFinder.java.
 * TODO: hard-coded to index a specific kind of XML.
 * 
 * @author Daniel Naber
 */
public class Indexer  {

  static final String BODY_FIELD = "body";
  
  //Constructor
  private Indexer() {}
  
  public static void main(String[] args) throws IOException {
    final Indexer prg = new Indexer();
    if (args.length != 2) {
      System.err.println("Usage: Indexer <dataDir> <indexDir>");
      System.exit(1);
    }
    // FIXME: make this an option:
    final Language lang = Language.GERMAN;
    prg.run(args[0], args[1], lang);
  }
  
  private void run(String dataDir, String indexDir, Language lang) throws IOException {
    final IndexWriter iw = new IndexWriter(indexDir, new POSTagAnalyzer(lang.getTagger()), true);
    iw.setMaxBufferedDocs(100);
    index(iw, new File(dataDir), 1);
    System.out.println("Optimizing index...");
    iw.optimize();
    iw.close();
    System.out.println("Done.");
  }

  private void index(IndexWriter iw, File dir, int count) throws IOException {
    if (dir.isDirectory()) {
      final File[] files = dir.listFiles();
      for (File file : files) {
        index(iw, file, ++count);
      }
    } else {
      final Document doc = new Document();
      if (count % 50 == 0)
        System.out.println("Indexing file #" + count);
      String s = StringTools.readFile(new FileInputStream(dir.getAbsolutePath()), "iso-8859-1");
      // XML data:
      s = getParagraphs(s);
      //s = s.replaceAll("(\\w)([.,?!])", "$1 $2");
      //s = s.replaceAll("<.*?>", "");
      //System.err.println(">"+s);
      doc.add(new Field(BODY_FIELD, s, Field.Store.YES, Field.Index.TOKENIZED));
      iw.addDocument(doc);
    }
  }

  private String getParagraphs(String xml) {
    final StringBuilder sb = new StringBuilder();
    final Pattern pattern = Pattern.compile("<p>(.*?)</p>", Pattern.DOTALL);
    final Matcher matcher = pattern.matcher(xml);
    int pos = 0;
    while (matcher.find(pos)) {
      sb.append(matcher.group(1));
      pos = matcher.end();
    }
    return sb.toString();
  }
  
}
