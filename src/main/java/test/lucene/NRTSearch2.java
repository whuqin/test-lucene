package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.*;

import java.io.IOException;

/**
 * Created by qinbin on 2015/3/25.
 */
public class NRTSearch2 {
    @org.junit.Test
    public void test() throws IOException {
        IndexWriter indexWriter = new IndexWriter(new RAMDirectory(),
                new IndexWriterConfig(Version.LUCENE_4_10_0, new StandardAnalyzer()));
        TrackingIndexWriter trackingIndexWriter = new TrackingIndexWriter(indexWriter);

        SearcherManager searcherManager = new SearcherManager(indexWriter, true, new SearcherFactory());
        ControlledRealTimeReopenThread<IndexSearcher> crt = new ControlledRealTimeReopenThread<IndexSearcher>(
                trackingIndexWriter, searcherManager, 5, 1);
        crt.start();

        Document doc = new Document();
        doc.add(new StringField("name", "zhangsan", Field.Store.YES));
        doc.add(new IntField("age", 5, Field.Store.YES));
        trackingIndexWriter.addDocument(doc);

        IndexSearcher indexSearcher = searcherManager.acquire();
        TopDocs topDocs = indexSearcher.search(new TermQuery(new Term("name", "zhangsan")), 10);
        System.out.println(topDocs.totalHits);
        searcherManager.release(indexSearcher);

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        indexSearcher = searcherManager.acquire();
        topDocs = indexSearcher.search(new TermQuery(new Term("name", "zhangsan")), 10);
        System.out.println(topDocs.totalHits);
        searcherManager.release(indexSearcher);
    }
}
