package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.*;

import java.io.IOException;

/**
 * IndexSearcher.
 * public TopFieldDocs search(Query query, Filter filter, int n, Sort sort)
 * new Sort(new SortField):
 * public SortField(String field, FieldComparatorSource comparator)
 * Created by qinbin on 2015/10/27.
 */
public class TestCustomSort {
    private Directory dir;

    @Before
    public void setup() throws IOException {
        dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir,
                new IndexWriterConfig(Version.LUCENE_4_10_0, new StandardAnalyzer()));
        addPoint(writer, "E1 Charro", "restaurant", 1, 2);
        addPoint(writer, "Cafe Poca Cosa", "restaurant", 5, 9);
        addPoint(writer, "Los Betos", "restaurant", 9, 6);
        addPoint(writer, "Nico's Taco Shop", "restaurant", 3, 8);
        writer.close();
    }

    private void addPoint(IndexWriter writer, String name, String type, int x, int y) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("name", name, Field.Store.YES));
        doc.add(new StringField("type", type, Field.Store.YES));
        doc.add(new IntField("x", x, Field.Store.YES));
        doc.add(new IntField("y", y, Field.Store.YES));
        writer.addDocument(doc);
    }

    @org.junit.Test
    //自定义排序
    public void testFieldComparatorSource() throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        TopFieldDocs docs = searcher.search(new MatchAllDocsQuery(), null, 10,
                new Sort(new SortField("", new DistanceComparatorSource())));

        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            FieldDoc fieldDoc = (FieldDoc)scoreDoc;
            Document document = searcher.doc(scoreDoc.doc);
            System.out.println(document.get("name") + "," + document.get("type") + ":"
                    + document.get("x") + "," + document.get("y") + ",distance:" + fieldDoc.fields[0]);
        }


    }

    private class DistanceComparatorSource extends FieldComparatorSource {

        @Override
        public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed)
                throws IOException {
            return new DistanceComparator(10);
        }
    }

    private class DistanceComparator extends FieldComparator {
        private FieldCache.Ints xDoc; //当前segment下x坐标
        private FieldCache.Ints yDoc; //当前segment下y坐标
        private float[] values;      //记录各hits的距离
        private float bottom;        //记录队尾最小值
        private float top;           //记录队首最大值

        public DistanceComparator(int numHits) {
            values = new float[numHits];
        }

        @Override
        //返回-1， slot2在slot1后面;返回1, slot2在slot1前面
        public int compare(int slot1, int slot2) {
            if (values[slot1] < values[slot2]) return -1;
            if (values[slot1] > values[slot2]) return 1;
            return 0;
        }

        @Override
        //slot是评分最低的槽位
        public void setBottom(int slot) {
            bottom = values[slot];
        }

        @Override
        public void setTopValue(Object value) {
            top = (Float)value;
        }

        @Override
        //doc和bottom比较，看doc是排在bottom前面（返回1）还是后面（返回-1）
        public int compareBottom(int doc) throws IOException {
            float distance = computeDistance(doc);
            if (bottom < distance) return -1;
            if (bottom > distance) return 1;
            return 0;
        }

        @Override
        public int compareTop(int doc) throws IOException {
            float distance = computeDistance(doc);
            if (top < distance) return -1;
            if (top > distance) return 1;
            return 0;
        }

        @Override
        //将doc的比较值放入slot位
        public void copy(int slot, int doc) throws IOException {
            values[slot] = computeDistance(doc);
        }

        @Override
        //用于通知遍历下一个segment
        public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
            xDoc = FieldCache.DEFAULT.getInts(context.reader(), "x", false);
            yDoc = FieldCache.DEFAULT.getInts(context.reader(), "y", false);
            System.out.println("docBase:" + context.docBase);
            return this;
        }

        @Override
        //获得某hit的比较值
        public Comparable value(int slot) {
            return new Float(values[slot]);
        }

        private float computeDistance(int doc) {
            int deltaX = xDoc.get(doc);
            int deltaY = yDoc.get(doc);
            float distance = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            return distance;
        }
    }
}
