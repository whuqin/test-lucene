package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.document.Field;


import java.io.File;
import java.io.IOException;

/**
 * Created by whuqin on 2014/9/4.
 */
public class SimpleLuceneTest {
    //private Directory index_dir = new RAMDirectory();
    private Directory index_dir;
    private IndexWriter writer;
    SimpleLuceneTest() throws IOException {
        index_dir = FSDirectory.open(new File("D:\\index"));
        //建立IndexWriter，指定其索引分析器、索引存放目录
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_10_0, new StandardAnalyzer());
        writer = new IndexWriter(index_dir, indexWriterConfig);

    }


    public void build_index(int start)
            throws IOException{


        for (int i = 0; i < 10; ++i) {
            //建立一个被查找元素doc
            Document doc = new Document();
            //为doc增加Field
            TextField field = new TextField("name", "book" + (i + start), Field.Store.YES);
            field.setBoost(2.0f);
            doc.add(field);
            doc.add(new TextField("content", "test" + (i + start), Field.Store.YES));

            writer.addDocument(doc);
        }
        writer.commit();
        //关闭writer，输出索引
        //writer.close();
        //index_dir.close();
    }

    /**
     * 在内容中搜索str
     * @param str
     * @return
     * @throws IOException
     */
    public String search(String str) throws IOException, ParseException{
        //打开索引目录
        DirectoryReader indexReader = DirectoryReader.open(writer, true);
      //  build_index(10);
      //  indexReader = DirectoryReader.openIfChanged(indexReader);

        //建立IndexSearcher
        IndexSearcher searcher = new IndexSearcher(indexReader);
        //规定查询条件
        QueryParser parser = new QueryParser("", new StandardAnalyzer());
        Query query = parser.parse(str);
        //System.out.println(query.toString());
        query = parser.parse("name:(nihao book0) or test:ok hehe");
        System.out.println(query.toString());
        //query = parser.parse("content:\"nihao well\"");
        //System.out.println(query.toString());
        //执行查询
        TopDocs docs = searcher.search(query, 10);
        String file_name = "";
        if (docs.totalHits != 0) {
            int id = docs.scoreDocs[0].doc;
            Document doc = searcher.doc(id);
            file_name = doc.get("name");
        }
        indexReader.close();
        return file_name;
    }

    public static void main(String[] args) throws IOException, ParseException{
        SimpleLuceneTest searcher = new SimpleLuceneTest();
        searcher.build_index(0);
        String name = searcher.search("test11");
        System.out.println("file:" + name);
    }
}
