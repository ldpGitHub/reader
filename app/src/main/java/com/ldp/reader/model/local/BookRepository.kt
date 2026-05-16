package com.ldp.reader.model.local

import android.util.Log
import com.ldp.reader.model.bean.BookChapterBean
import com.ldp.reader.model.bean.BookRecordBean
import com.ldp.reader.model.bean.CollBookBean
import com.ldp.reader.model.objectbox.ObjectBoxBookRecordStore
import com.ldp.reader.model.objectbox.ObjectBoxBookStore
import com.ldp.reader.model.objectbox.ObjectBoxDbHelper
import com.ldp.reader.utils.BookManager
import com.ldp.reader.utils.Constant
import com.ldp.reader.utils.FileUtils
import com.ldp.reader.utils.IOUtils
import com.ldp.reader.utils.StringUtils
import io.objectbox.BoxStore
import io.reactivex.Single
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.io.Writer

/**
 * Created by ldp on 17-5-8.
 * 存储关于书籍内容的信息(CollBook(收藏书籍),BookChapter(书籍列表),ChapterInfo(书籍章节),BookRecord(记录))
 */
class BookRepository private constructor() {
    private val mBookStore: ObjectBoxBookStore
    private val mBookRecordStore: ObjectBoxBookRecordStore

    init {
        val boxStore: BoxStore = ObjectBoxDbHelper.getInstance().store
        mBookStore = ObjectBoxBookStore(boxStore)
        mBookRecordStore = ObjectBoxBookRecordStore(boxStore)
    }

    // 存储已收藏书籍
    fun saveCollBookWithAsync(bean: CollBookBean) {
        // 启动异步存储
        mBookStore.runInTxAsync(
            Runnable {
                if (bean.getBookChapters() != null) {
                    replaceBookChaptersInTx(bean.get_id(), bean.getBookChapters())
                    Log.d(TAG, "saveCollBookWithAsync: " + "进行存储" + bean.getBookChapters())
                }
                Log.d(
                    TAG,
                    "saveCollBookWithAsync: " + "进行存储" + bean.author + bean.title + bean.shortIntro
                )
                // 存储CollBook (确保先后顺序，否则出错)
                // 表示当前CollBook已经阅读
                bean.setIsUpdate(false)
                bean.lastRead = StringUtils.dateConvert(
                    System.currentTimeMillis(),
                    Constant.FORMAT_BOOK_DATE
                )
                // 直接更新
                val collBookBeanOrigin = getInstance().getCollBook(bean.get_id())
                if (collBookBeanOrigin != null) {
                    bean.bookIdInBiquge = collBookBeanOrigin.bookIdInBiquge
                }
                getInstance().saveCollBook(bean)
                Log.d(
                    TAG,
                    "saveCollBookWithAsync: " + "存储完成" + bean.author + bean.title + bean.shortIntro
                )
                val collBooksTest = mBookStore.getCollBooks()
                for (collBookBean in collBooksTest) {
                    Log.d(TAG, "+存储后: " + "进行存储" + collBookBean.title)
                }
            }
        )
    }

    /**
     * 异步存储。
     * 同时保存BookChapter
     */
    fun saveCollBooksWithAsync(beans: List<CollBookBean>) {
        mBookStore.runInTxAsync(
            Runnable {
                Log.d(TAG, "111saveCollBookWithAsync : " + "进行存储" + beans.toString())
                for (bean in beans) {
                    if (bean.getBookChapters() != null) {
                        replaceBookChaptersInTx(bean.get_id(), bean.getBookChapters())
                    }
                }
                // 存储CollBook (确保先后顺序，否则出错)
                for (bookBean in beans) {
                    val collBookBeanOrigin = getInstance().getCollBook(bookBean.get_id())
                    bookBean.bookIdInBiquge = collBookBeanOrigin!!.bookIdInBiquge
                }
                mBookStore.saveCollBooks(beans)
            }
        )
    }

    fun saveCollBook(bean: CollBookBean) {
        Log.d(TAG, "22saveCollBookWithAsync : " + "进行存储" + bean.toString())
        val collBookBeanOrigin = getInstance().getCollBook(bean.get_id())
        if (collBookBeanOrigin != null) {
            bean.bookIdInBiquge = collBookBeanOrigin.bookIdInBiquge
        }
        mBookStore.saveCollBook(bean)
    }

    fun saveCollBooks(beans: List<CollBookBean>) {
        Log.d(TAG, "33saveCollBookWithAsync : " + "进行存储" + beans.toString())
        for (bookBean in beans) {
            val collBookBeanOrigin = getInstance().getCollBook(bookBean.get_id())
            if (collBookBeanOrigin != null) {
                bookBean.bookIdInBiquge = collBookBeanOrigin.bookIdInBiquge
            }
        }
        mBookStore.saveCollBooks(beans)
    }

    /**
     * 异步存储BookChapter
     */
    fun saveBookChaptersWithAsync(beans: List<BookChapterBean>?) {
        mBookStore.runInTxAsync(
            Runnable {
                if (beans == null || beans.isEmpty()) {
                    return@Runnable
                }
                replaceBookChaptersInTx(beans[0].bookId, beans)
                for (bookChapterBean in beans) {
                    Log.d("+存储", "saveBookChaptersWithAsync: " + bookChapterBean.title)
                }
                Log.d(TAG, "saveBookChaptersWithAsync: " + "进行存储")
            }
        )
    }

    /**
     * 存储章节
     */
    fun saveChapterInfo(folderName: String?, fileName: String?, content: String?) {
        if (content == null) {
            return
        }
        val file = BookManager.getBookFile(folderName, fileName)
        // 获取流并存储
        var writer: Writer? = null
        try {
            writer = BufferedWriter(FileWriter(file))
            writer.write(content)
            writer.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            IOUtils.close(writer)
        }
    }

    fun saveBookRecord(bean: BookRecordBean) {
        mBookRecordStore.saveBookRecord(bean)
    }

    /*****************************get************************************************/
    fun getCollBook(bookId: String?): CollBookBean? {
        return mBookStore.getCollBook(bookId)
    }

    val collBooks: List<CollBookBean>
        get() = mBookStore.getCollBooks()

    // 获取书籍列表
    fun getBookChaptersInRx(bookId: String?): Single<List<BookChapterBean>> {
        return Single.create { emitter -> emitter.onSuccess(mBookStore.getBookChapters(bookId)) }
    }

    // 获取阅读记录
    fun getBookRecord(bookId: String?): BookRecordBean? {
        return mBookRecordStore.getBookRecord(bookId)
    }

    /************************************************************/
    fun deleteCollBookInRx(bean: CollBookBean): Single<Void> {
        return Single.create { emitter ->
            // 查看文本中是否存在删除的数据
            deleteBook(bean.get_id())
            // 删除目录
            deleteBookChapter(bean.get_id())
            // 删除CollBook
            mBookStore.deleteCollBook(bean)
            emitter.onSuccess(Void())
        }
    }

    // 这个需要用rx，进行删除
    fun deleteBookChapter(bookId: String?) {
        mBookStore.deleteBookChapters(bookId)
    }

    private fun replaceBookChaptersInTx(bookId: String?, beans: List<BookChapterBean>?) {
        if (bookId == null || beans == null) {
            return
        }
        mBookStore.replaceBookChapters(bookId, beans)
    }

    fun deleteCollBook(collBook: CollBookBean) {
        mBookStore.deleteCollBook(collBook)
    }

    // 删除书籍
    fun deleteBook(bookId: String?) {
        FileUtils.deleteFile(Constant.BOOK_CACHE_PATH + bookId)
    }

    fun deleteBookRecord(id: String?) {
        mBookRecordStore.deleteBookRecord(id)
    }

    companion object {
        private const val TAG = "CollBookManager"

        @Volatile
        private var sInstance: BookRepository? = null

        @JvmStatic
        fun getInstance(): BookRepository {
            if (sInstance == null) {
                synchronized(BookRepository::class.java) {
                    if (sInstance == null) {
                        sInstance = BookRepository()
                    }
                }
            }
            return sInstance!!
        }
    }
}
