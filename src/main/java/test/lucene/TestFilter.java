package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.junit.*;
import org.junit.Test;

import java.io.IOException;

/**
 * IndexSearcher.
 * public TopDocs search(Query query, Filter filter, int n)
 * public TopFieldDocs search(Query query, Filter filter, int n,
 Sort sort)
 * Created by qinbin on 2015/10/23.
 */
public class TestFilter {
    Directory dir;
    @BeforeClass
    public static void testBeforeClass() {
        System.out.println("before class");
    }
    @Before
    public void setup() throws IOException {
        dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir,
                new IndexWriterConfig(Version.LUCENE_4_10_0, new StandardAnalyzer()));
        Document doc = new Document();
        doc.add(new TextField("title", "lucene in action", Field.Store.YES));
        doc.add(new StringField("sn", "book1", Field.Store.YES));
        doc.add(new FloatField("price", 30.5f, Field.Store.YES));
        doc.add(new IntField("left", 56, Field.Store.YES));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new TextField("title", "core python programming", Field.Store.YES));
        doc.add(new StringField("sn", "book2", Field.Store.YES));
        doc.add(new FloatField("price", 40.0f, Field.Store.YES));
        doc.add(new IntField("left", 300, Field.Store.YES));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new TextField("title", "c++ primer", Field.Store.YES));
        doc.add(new StringField("sn", "book3", Field.Store.YES));
        doc.add(new FloatField("price", 87f, Field.Store.YES));
        doc.add(new IntField("left", 210, Field.Store.YES));
        writer.addDocument(doc);

        writer.commit();
        writer.close();
    }

    private void search(Query query, Filter filter, int num) throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        TopDocs docs = searcher.search(query, filter, num);
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println(doc.get("title") + "," + doc.get("sn") + "," + doc.get("price") + "," + doc.get("left"));
        }
    }

    @Test
    public void testTermRangeFilter() throws IOException {
        Filter filter = new TermRangeFilter("sn", new BytesRef("book0"), new BytesRef("book2"), true, true);
        search(new MatchAllDocsQuery(), filter, 10);
        System.out.println("-------------------------------");
    }

    @Test
    public void testNumericRangeFilter() throws IOException {
        Filter filter = NumericRangeFilter.newFloatRange("price", 20f, 50f, true, true);
        search(new MatchAllDocsQuery(), filter, 10);
        System.out.println("-------------------------------");
    }

    @Test
    public void testFieldCacheRangeFilter() throws IOException {
        Filter filter = FieldCacheRangeFilter.newIntRange("left", 0, 100, true, true);
        search(new MatchAllDocsQuery(), filter, 10);
        System.out.println("-------------------------------");
    }

    @Test
    public void testFieldCacheTermsFilter() throws IOException {
        Filter filter = new FieldCacheTermsFilter("sn",
                new BytesRef[]{new BytesRef("book1"), new BytesRef("book2")});
        search(new MatchAllDocsQuery(), filter, 10);
        System.out.println("-------------------------------");
    }

    @Test
    public void testQueryWrapperFilter() throws IOException {
        Query query = new TermQuery(new Term("title", "python"));
        Filter filter = new QueryWrapperFilter(query);
        search(new MatchAllDocsQuery(), filter, 10);
        System.out.println("-------------------------------");
    }

    @Test
    public void testPrefixFilter() throws IOException {
        Filter filter = new PrefixFilter(new Term("sn", "book"));
        search(new MatchAllDocsQuery(), filter, 10);
        System.out.println("-------------------------------");
    }

    @Test
    //缓存同一个IndexReader/IndexSearch实例下的同一个Filter实例的结果
    public void testCachingWrapperFilter() throws IOException {
        Filter filter = new QueryWrapperFilter(new TermQuery(new Term("title", "python")));
        CachingWrapperFilter cachingWrapperFilter = new CachingWrapperFilter(filter);
        search(new MatchAllDocsQuery(), cachingWrapperFilter, 10);
        search(new MatchAllDocsQuery(), cachingWrapperFilter, 10);
        System.out.println("-------------------------------");
    }
    @AfterClass
    public static void testAfterClass() {
        System.out.println("after class");
    }
}