package com.ldp.reader.model.local;

import android.util.Log;

import com.ldp.reader.model.bean.BookChapterBean;
import com.ldp.reader.model.bean.BookRecordBean;
import com.ldp.reader.model.bean.CollBookBean;

import com.ldp.reader.model.objectbox.ObjectBoxBookStore;
import com.ldp.reader.model.objectbox.ObjectBoxBookRecordStore;
import com.ldp.reader.model.objectbox.ObjectBoxDbHelper;
import com.ldp.reader.utils.BookManager;
import com.ldp.reader.utils.Constant;
import com.ldp.reader.utils.FileUtils;
import com.ldp.reader.utils.IOUtils;
import com.ldp.reader.utils.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.objectbox.BoxStore;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

/**
 * Created by ldp on 17-5-8.
 * 存储关于书籍内容的信息(CollBook(收藏书籍),BookChapter(书籍列表),ChapterInfo(书籍章节),BookRecord(记录))
 */

public class BookRepository {
    private static final String TAG = "CollBookManager";
    private static volatile BookRepository sInstance;
    private ObjectBoxBookStore mBookStore;
    private ObjectBoxBookRecordStore mBookRecordStore;
    private BookRepository(){
        BoxStore boxStore = ObjectBoxDbHelper.getInstance().getStore();
        mBookStore = new ObjectBoxBookStore(boxStore);
        mBookRecordStore = new ObjectBoxBookRecordStore(boxStore);
    }

    public static BookRepository getInstance(){
        if (sInstance == null){
            synchronized (BookRepository.class){
                if (sInstance == null){
                    sInstance = new BookRepository();
                }
            }
        }
        return sInstance;
    }

    //存储已收藏书籍
    public void saveCollBookWithAsync(CollBookBean bean){
        //启动异步存储
        mBookStore.runInTxAsync(
                () -> {
                            if (bean.getBookChapters() != null){
                                replaceBookChaptersInTx(bean.get_id(), bean.getBookChapters());

                                Log.d(TAG, "saveCollBookWithAsync: "+"进行存储" +  bean.getBookChapters());
                            }
                            Log.d(TAG, "saveCollBookWithAsync: "+"进行存储" + bean.getAuthor() +bean.getTitle()+bean.getShortIntro());
                            //存储CollBook (确保先后顺序，否则出错)
                            //表示当前CollBook已经阅读
                            bean.setIsUpdate(false);
                            bean.setLastRead(StringUtils.
                                    dateConvert(System.currentTimeMillis(), Constant.FORMAT_BOOK_DATE));
                            //直接更新
                            CollBookBean collBookBeanOrigin = BookRepository.getInstance().getCollBook(bean.get_id());
                            if(null != collBookBeanOrigin) {
                                bean.setBookIdInBiquge(collBookBeanOrigin.getBookIdInBiquge());
                            }
                            BookRepository.getInstance().saveCollBook(bean);
//                            mCollBookDao.insertOrReplaceInTx(bean);
                            Log.d(TAG, "saveCollBookWithAsync: "+"存储完成" + bean.getAuthor() +bean.getTitle()+bean.getShortIntro());
                            List<CollBookBean> collBooksTest = mBookStore.getCollBooks();
                            for (CollBookBean collBookBean: collBooksTest ) {
                                Log.d(TAG, "+存储后: "+"进行存储" +   collBookBean.getTitle());


                            }
                        });
    }
    /**
     * 异步存储。
     * 同时保存BookChapter
     * @param beans
     */
    public void saveCollBooksWithAsync(List<CollBookBean> beans){
        mBookStore.runInTxAsync(
                () -> {
                            Log.d(TAG, "111saveCollBookWithAsync : "+"进行存储" +  beans.toString());
                            for (CollBookBean bean : beans){
                                if (bean.getBookChapters() != null){
                                    replaceBookChaptersInTx(bean.get_id(), bean.getBookChapters());
                                }
                            }
                            //存储CollBook (确保先后顺序，否则出错)
                            for (CollBookBean bookBean: beans) {
                                CollBookBean collBookBeanOrigin = BookRepository.getInstance().getCollBook(bookBean.get_id());
                                bookBean.setBookIdInBiquge(collBookBeanOrigin.getBookIdInBiquge());
                            }
                            mBookStore.saveCollBooks(beans);
                        });
    }

    public void saveCollBook(CollBookBean bean){
        Log.d(TAG, "22saveCollBookWithAsync : "+"进行存储" +  bean.toString());
        CollBookBean collBookBeanOrigin = BookRepository.getInstance().getCollBook(bean.get_id());
        if(null != collBookBeanOrigin) {
            bean.setBookIdInBiquge(collBookBeanOrigin.getBookIdInBiquge());
        }
        mBookStore.saveCollBook(bean);
    }
    public void saveCollBooks(List<CollBookBean> beans){
        Log.d(TAG, "33saveCollBookWithAsync : "+"进行存储" +  beans.toString());
        for (CollBookBean bookBean: beans) {
            CollBookBean collBookBeanOrigin = BookRepository.getInstance().getCollBook(bookBean.get_id());
            if(null != collBookBeanOrigin) {
                bookBean.setBookIdInBiquge(collBookBeanOrigin.getBookIdInBiquge());
            }
        }
        mBookStore.saveCollBooks(beans);
    }

    /**
     * 异步存储BookChapter
     * @param beans
     */
    public void saveBookChaptersWithAsync(List<BookChapterBean> beans){
        mBookStore.runInTxAsync(
                () -> {

                            if (beans == null || beans.isEmpty()) {
                                return;
                            }
                            replaceBookChaptersInTx(beans.get(0).getBookId(), beans);
                            for (BookChapterBean bookChapterBean: beans ) {
                                Log.d("+存储", "saveBookChaptersWithAsync: "+bookChapterBean.getTitle());

                            }

                            Log.d(TAG, "saveBookChaptersWithAsync: "+"进行存储");
                        });
    }

    /**
     * 存储章节
     * @param folderName
     * @param fileName
     * @param content
     */
    public void saveChapterInfo(String folderName,String fileName,String content){
        if(null == content){
            return;
        }
        File file = BookManager.getBookFile(folderName, fileName);
        //获取流并存储
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            IOUtils.close(writer);
        }
    }

    public void saveBookRecord(BookRecordBean bean){
        mBookRecordStore.saveBookRecord(bean);
    }

    /*****************************get************************************************/
    public CollBookBean getCollBook(String bookId){
        return mBookStore.getCollBook(bookId);
    }


    public  List<CollBookBean> getCollBooks(){
        return mBookStore.getCollBooks();
    }



    //获取书籍列表
    public Single<List<BookChapterBean>> getBookChaptersInRx(String bookId){
        return Single.create(new SingleOnSubscribe<List<BookChapterBean>>() {
            @Override
            public void subscribe(SingleEmitter<List<BookChapterBean>> e) throws Exception {
                e.onSuccess(mBookStore.getBookChapters(bookId));
            }
        });
    }

    //获取阅读记录
    public BookRecordBean getBookRecord(String bookId){
        return mBookRecordStore.getBookRecord(bookId);
    }

    /************************************************************/

    /************************************************************/
    public Single<Void> deleteCollBookInRx(CollBookBean bean) {
        return Single.create(new SingleOnSubscribe<Void>() {
            @Override
            public void subscribe(SingleEmitter<Void> e) throws Exception {
                //查看文本中是否存在删除的数据
                deleteBook(bean.get_id());
                //删除目录
                deleteBookChapter(bean.get_id());
                //删除CollBook
                mBookStore.deleteCollBook(bean);
                e.onSuccess(new Void());
            }
        });
    }

    //这个需要用rx，进行删除
    public void deleteBookChapter(String bookId){
        mBookStore.deleteBookChapters(bookId);
    }

    private void replaceBookChaptersInTx(String bookId, List<BookChapterBean> beans) {
        if (bookId == null || beans == null) {
            return;
        }
        mBookStore.replaceBookChapters(bookId, beans);
    }

    public void deleteCollBook(CollBookBean collBook){
        mBookStore.deleteCollBook(collBook);
    }

    //删除书籍
    public void deleteBook(String bookId){
        FileUtils.deleteFile(Constant.BOOK_CACHE_PATH+bookId);
    }

    public void deleteBookRecord(String id){
        mBookRecordStore.deleteBookRecord(id);
    }

}
