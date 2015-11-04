package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;

/**
 * Created by qinbin on 2015/1/12.
 */
public class NrtSearch {
    private Directory ramDir = new RAMDirectory();
    public void testNrt() throws IOException {
        //IndexWriter
        IndexWriterConfig writerConfig = new IndexWriterConfig(Version.LUCENE_4_10_0, new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(ramDir, writerConfig);

        Document doc = new Document();
        doc.add(new TextField("title", "lucene", Field.Store.YES));
        doc.add(new TextField("author", "zhangsan", Field.Store.YES));
        writer.addDocument(doc);
        //IndexReader
//        IndexReader reader = DirectoryReader.open(writer, true);
//        IndexSearcher searcher = new IndexSearcher(reader);
        SearcherManager searcherManager = new SearcherManager(writer, true, new SearcherFactory());
        IndexSearcher searcher = searcherManager.acquire();

        TermQuery query = new TermQuery(new Term("title", "lucene"));
        TopDocs docs = searcher.search(query, 10);
        System.out.println(docs.totalHits);

        for (int i = 0; i < 10; ++i) {
            doc = new Document();
            doc.add(new TextField("title", "lucene " + i, Field.Store.YES));
            doc.add(new TextField("author", "zhangsan " + i, Field.Store.YES));
            writer.addDocument(doc);
        }
        searcherManager.maybeRefresh();
        searcherManager.release(searcher);
        searcher = searcherManager.acquire();
        //openIfChanged
//        IndexReader newReader = DirectoryReader.openIfChanged((DirectoryReader) reader, writer, true);
//        if (reader != newReader) {
//            searcher = new IndexSearcher(newReader);
//            reader.close();
//        }

        docs = searcher.search(query, 10);
        System.out.println(docs.totalHits);
        searcherManager.release(searcher);
        searcherManager.close();
    }
    public static void main(String[] args) throws IOException {
        /*NrtSearch nrtSearch = new NrtSearch();
        nrtSearch.testNrt();*/
        String str = "nihao";
        System.out.println(str.intern());
    }
}
