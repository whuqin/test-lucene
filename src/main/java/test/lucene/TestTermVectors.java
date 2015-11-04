package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;

/**
 * Created by qinbin on 2015/4/8.
 */
public class TestTermVectors {
    public static void main(String[] args) throws IOException {
        IndexWriter writer = new IndexWriter(new RAMDirectory(),
                new IndexWriterConfig(Version.LATEST, new StandardAnalyzer()));
        Document doc = new Document();
        FieldType fieldType = new FieldType();
        fieldType.setIndexed(true);
        fieldType.setStored(true);
        fieldType.setStoreTermVectors(true);
        doc.add(new Field("author", "john david", fieldType));
        doc.add(new Field("subject", "john nihao", fieldType));
        writer.addDocument(doc);
        writer.commit();

        IndexReader reader = DirectoryReader.open(writer, true);
        IndexSearcher searcher = new IndexSearcher(reader);
        TermQuery termQuery = new TermQuery(new Term("author", "john"));
        TopDocs topDocs = searcher.search(termQuery, 10);
        Document topDoc = searcher.doc(topDocs.scoreDocs[0].doc);
        System.out.println("author:" + topDoc.get("author"));
        System.out.println("subject:" + topDoc.get("subject"));
        Terms terms = reader.getTermVector(topDocs.scoreDocs[0].doc, "author");
        System.out.println(terms.size());
        System.out.println(terms.getSumDocFreq());
    }
}
